package ca.en.solution.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sun.net.httpserver.HttpServer;

class FrankfurterCurrencyRateClientTest {

    @Test
    void readsTheLatestCadToUsdRateFromTheSinglePairEndpoint() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        try (TestServer server = new TestServer(200, """
                {"date":"2026-10-03","base":"CAD","quote":"USD","rate":0.73456}
                """, requestedPath)) {
            FrankfurterCurrencyRateClient client = server.client();

            assertThat(client.cadToUsdRate()).isEqualByComparingTo("0.73456");
            assertThat(requestedPath.get()).isEqualTo("/v2/rate/cad/usd");
        }
    }

    @Test
    void rejectsFailedMalformedMissingAndNonPositiveRateResponses() throws Exception {
        assertUnavailable(503, "service unavailable");
        assertUnavailable(200, "not json");
        assertUnavailable(200, "{}");
        assertUnavailable(200, "{\"rate\":0}");
        assertUnavailable(200, "{\"rate\":-0.1}");
        assertUnavailable(200, "{\"base\":\"USD\",\"quote\":\"CAD\",\"rate\":1.42}");
    }

    @Test
    void rejectsRedirectsInsteadOfFollowingThemToAnotherRateResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/v2/rate/cad/usd", exchange -> {
                exchange.getResponseHeaders().set("Location", "/redirected-rate");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            });
            server.createContext("/redirected-rate", exchange -> {
                byte[] response = "{\"base\":\"CAD\",\"quote\":\"USD\",\"rate\":0.73456}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();

            FrankfurterCurrencyRateClient client = new FrankfurterCurrencyRateClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(1), Duration.ofSeconds(2));

            assertThatThrownBy(client::cadToUsdRate).isInstanceOf(CurrencyRateUnavailableException.class);
        } finally {
            server.stop(0);
        }
    }

    private static void assertUnavailable(int status, String body) throws Exception {
        try (TestServer server = new TestServer(status, body, new AtomicReference<>())) {
            assertThatThrownBy(() -> server.client().cadToUsdRate())
                    .isInstanceOf(CurrencyRateUnavailableException.class);
        }
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;

        TestServer(int status, String body, AtomicReference<String> requestedPath) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                requestedPath.set(exchange.getRequestURI().getPath());
                byte[] response = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                exchange.sendResponseHeaders(status, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
        }

        FrankfurterCurrencyRateClient client() {
            return new FrankfurterCurrencyRateClient("http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofSeconds(1), Duration.ofSeconds(2));
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
