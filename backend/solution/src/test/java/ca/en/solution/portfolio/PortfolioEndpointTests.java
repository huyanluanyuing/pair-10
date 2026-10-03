package ca.en.solution.portfolio;

import ca.en.solution.crm.CrmClient;
import ca.en.solution.crm.CrmUnavailableException;
import ca.en.solution.crm.HttpCrmClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioEndpointTests {
    private static final String AUTH = "Bearer superday-demo-token";
    private static final String ACCOUNTS = """
            [
              {"acct_ref":"P-9001","acct_nickname":"Taxable Brokerage",
               "curr_val":{"amt":48930,"ccy":"CAD"},
               "chg_1d":{"amt":30,"pct":0.0006134969325153374},"since_inception_pct":0.187},
              {"acct_ref":"P-9002","acct_nickname":"Retirement",
               "curr_val":{"amt":500,"ccy":"CAD"},
               "chg_1d":{"amt":500,"pct":0},"since_inception_pct":0.25},
              {"acct_ref":"P-EMPTY","acct_nickname":"Empty",
               "curr_val":{"amt":0,"ccy":"CAD"},
               "chg_1d":{"amt":0,"pct":0},"since_inception_pct":0}
            ]
            """;
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final AtomicReference<Reply> REPLY = new AtomicReference<>();
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicReference<String> REQUEST_URI = new AtomicReference<>();
    private static final HttpServer CRM = startCrm();

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void crmProperties(DynamicPropertyRegistry properties) {
        properties.add("crm.base-url", () -> "http://127.0.0.1:" + CRM.getAddress().getPort());
    }

    @BeforeEach
    void resetCrm() {
        CALLS.set(0);
        REQUEST_URI.set(null);
        REPLY.set(new Reply(200, payload(false), false, false, new CountDownLatch(0)));
    }

    @AfterEach
    void releaseSlowResponse() {
        REPLY.get().release().countDown();
    }

    @AfterAll
    static void closeCrm() {
        CRM.stop(0);
        EXECUTOR.close();
    }

    @Test
    void mapsSuccessfulCrmResponseToTheExactPublicSchema() throws Exception {
        mvc.perform(get("/portfolios/P-9001").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("""
                        {"portfolioId":"P-9001","clientId":"abc123","label":"Taxable Brokerage",
                         "currency":"CAD","totalMarketValue":48930,"dayChangeAmount":30,
                         "dayChangePercent":0.000613,"totalReturnSinceInception":0.187,
                         "asOf":"2026-10-03T12:00:00Z"}
                        """))
                .andExpect(jsonPath("$.length()").value(9));
        assertEquals("/crm/portfolios/P-9001", REQUEST_URI.get());
        assertEquals(1, CALLS.get());
    }

    @Test
    void selectsTheRequestedAccountEvenWhenItIsNotFirst() throws Exception {
        mvc.perform(get("/portfolios/P-9002").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("P-9002"))
                .andExpect(jsonPath("$.label").value("Retirement"))
                .andExpect(jsonPath("$.totalMarketValue").value(500))
                .andExpect(jsonPath("$.dayChangePercent").value(0));
        assertEquals("/crm/portfolios/P-9002", REQUEST_URI.get());
    }

    @Test
    void mapsAccountsNestedUnderRelationships() throws Exception {
        reply(200, payload(true));
        mvc.perform(get("/portfolios/P-9002").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("P-9002"))
                .andExpect(jsonPath("$.clientId").value("abc123"))
                .andExpect(jsonPath("$.totalMarketValue").value(500));
    }

    @Test
    void mapsMissingNicknameAndNullMarketValueExplicitly() throws Exception {
        reply(200, """
                {"client_record":{"client_id":"abc123","accounts":[
                  {"acct_ref":"P-9001","curr_val":{"amt":null,"ccy":"CAD"},
                   "chg_1d":{"amt":30,"pct":0.000613},"since_inception_pct":0.187}]},
                 "meta":{"retrieved_at":"2026-10-03T12:00:00Z"}}
                """);
        mvc.perform(get("/portfolios/P-9001").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").hasJsonPath())
                .andExpect(jsonPath("$.label").value(nullValue()))
                .andExpect(jsonPath("$.totalMarketValue").hasJsonPath())
                .andExpect(jsonPath("$.totalMarketValue").value(nullValue()))
                .andExpect(jsonPath("$.currency").value("CAD"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"client_record\":{\"accounts\":[{\"acct_ref\":\"P-9001\"}]}}",
            "{\"client_record\":{\"client_id\":null,\"accounts\":[{\"acct_ref\":\"P-9001\",\"acct_nickname\":null,\"curr_val\":null,\"chg_1d\":null,\"since_inception_pct\":null}]},\"meta\":null}"
    })
    void keepsEveryMissingOrNullOutputFieldPresent(String body) throws Exception {
        reply(200, body);
        mvc.perform(get("/portfolios/P-9001").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"portfolioId":"P-9001","clientId":null,"label":null,"currency":null,
                         "totalMarketValue":null,"dayChangeAmount":null,"dayChangePercent":null,
                         "totalReturnSinceInception":null,"asOf":null}
                        """))
                .andExpect(jsonPath("$.length()").value(9));
    }

    @Test
    void mapsZeroValuesWithoutTreatingThemAsMissing() throws Exception {
        mvc.perform(get("/portfolios/P-EMPTY").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMarketValue").value(0))
                .andExpect(jsonPath("$.dayChangeAmount").value(0))
                .andExpect(jsonPath("$.dayChangePercent").value(0))
                .andExpect(jsonPath("$.totalReturnSinceInception").value(0));
    }

    @Test
    void roundsExactCrmDecimalsOnceAtTheOutputBoundary() throws Exception {
        reply(200, """
                {"client_record":{"accounts":[{"acct_ref":"P-9001",
                  "curr_val":{"amt":9007199254740993.455,"ccy":"CAD"},
                  "chg_1d":{"amt":-1.235,"pct":0.1234565},"since_inception_pct":-0.1234565}]}}
                """);
        mvc.perform(get("/portfolios/P-9001").header("Authorization", AUTH))
                .andExpect(status().isOk())
                // Check the JSON number token directly; a double loses precision here.
                .andExpect(content().string(containsString("\"totalMarketValue\":9007199254740993.46")))
                .andExpect(jsonPath("$.dayChangeAmount").value(-1.24))
                .andExpect(jsonPath("$.dayChangePercent").value(0.123457))
                .andExpect(jsonPath("$.totalReturnSinceInception").value(-0.123457));
    }

    @Test
    void returnsStructuredNotFoundForCrm404() throws Exception {
        reply(404, "{\"error\":\"unknown_account\",\"message\":\"legacy detail\"}");
        assertError("UNKNOWN", 404, "not_found");
        assertEquals("/crm/portfolios/UNKNOWN", REQUEST_URI.get());
    }

    @Test
    void returnsNotFoundWhenNoAccountMatchesTheRequestedId() throws Exception {
        assertError("UNKNOWN", 404, "not_found");
        assertEquals(1, CALLS.get());
    }

    @Test
    void returnsNotFoundForAnEmptyCrmAccountList() throws Exception {
        reply(200, "{\"client_record\":{\"accounts\":[]}}");
        assertError("P-9001", 404, "not_found");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 429, 500, 503, 504})
    void returnsStructuredBadGatewayForOtherCrmErrorStatusesWithoutRetrying(int upstreamStatus) throws Exception {
        reply(upstreamStatus, "{\"error\":\"legacy_unavailable\",\"message\":\"legacy detail\"}");
        assertError("P-9001", 502, "crm_unavailable");
        assertEquals(1, CALLS.get());
    }

    @Test
    void rejectsCrmRedirectsWithoutFollowingThem() throws Exception {
        reply(302, payload(false));
        assertError("P-9001", 502, "crm_unavailable");
        assertEquals(1, CALLS.get());
    }

    @Test
    void translatesConnectionRefusalIntoCrmUnavailable() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }
        CrmClient client = new HttpCrmClient("http://localhost:" + closedPort,
                Duration.ofSeconds(1), Duration.ofSeconds(2));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(CrmUnavailableException.class, () -> client.fetchPortfolio("P-9001")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not JSON", "", "null", "{}", "{\"client_record\":null}",
            "{\"client_record\":{}}", "{\"client_record\":{\"accounts\":null}}",
            "{\"client_record\":{\"accounts\":{}}}",
            "{\"client_record\":{\"accounts\":[null]}}",
            "{\"client_record\":{\"accounts\":[{\"acct_ref\":\"P-9001\",\"curr_val\":{\"amt\":\"invalid\"}}]}}"
    })
    void returnsBadGatewayForMalformedCrmResponses(String body) throws Exception {
        reply(200, body);
        assertError("P-9001", 502, "crm_unavailable");
        assertEquals(1, CALLS.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void realReadTimeoutBoundsBothSlowHeadersAndSlowBody(boolean delayBody) {
        REPLY.set(new Reply(200, payload(false), !delayBody, delayBody, new CountDownLatch(1)));
        long started = System.nanoTime();
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertError("P-9001", 502, "crm_unavailable"));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(elapsedMillis >= 1500 && elapsedMillis < 5000,
                "Expected the 2-second read timeout; elapsed=" + elapsedMillis + "ms");
        assertEquals(1, CALLS.get());
    }

    private void assertError(String id, int expectedStatus, String error) throws Exception {
        mvc.perform(get("/portfolios/{id}", id).header("Authorization", AUTH))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.length()").value(2));
    }

    private static void reply(int status, String body) {
        REPLY.set(new Reply(status, body, false, false, new CountDownLatch(0)));
    }

    private static String payload(boolean nested) {
        String accounts = nested ? "\"relationships\":{\"accounts\":" + ACCOUNTS + "}"
                : "\"accounts\":" + ACCOUNTS;
        return "{\"client_record\":{\"client_id\":\"abc123\",\"full_name\":\"Jane Doe\","
                + accounts + "},\"meta\":{\"retrieved_at\":\"2026-10-03T12:00:00Z\",\"source\":\"legacy-crm-v2\"}}";
    }

    private static HttpServer startCrm() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(EXECUTOR);
            server.createContext("/", exchange -> {
                CALLS.incrementAndGet();
                REQUEST_URI.set(exchange.getRequestURI().toString());
                Reply reply = REPLY.get();
                try (exchange) {
                    if (reply.delayHeaders()) {
                        reply.release().await(10, TimeUnit.SECONDS);
                    }
                    byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    if (reply.status() == 302) {
                        exchange.getResponseHeaders().set("Location", "/crm/redirect-target");
                    }
                    exchange.sendResponseHeaders(reply.status(), body.length == 0 ? -1 : body.length);
                    if (reply.delayBody()) {
                        reply.release().await(10, TimeUnit.SECONDS);
                    }
                    if (body.length > 0) {
                        exchange.getResponseBody().write(body);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    if (!reply.delayHeaders() && !reply.delayBody()) {
                        throw e;
                    }
                    // A timeout deliberately disconnects before the stub finishes writing.
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private record Reply(int status, String body, boolean delayHeaders, boolean delayBody, CountDownLatch release) {
    }
}
