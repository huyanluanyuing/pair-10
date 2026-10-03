package ca.en.solution.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(AuthFilterTest.ProbeController.class)
@Import(AuthFilterTest.ProbeController.class)
class AuthFilterTest {

    private static final String UNAUTHORIZED_BODY = """
            { "error": "unauthorized", "message": "Missing or invalid Authorization header. Expected: Bearer <token>." }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aRequestWithoutAnAuthorizationHeaderIsRejectedWith401AndTheErrorBody() throws Exception {
        mockMvc.perform(get("/portfolios/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(UNAUTHORIZED_BODY, JsonCompareMode.STRICT));
    }

    @Test
    void aHeaderWithoutTheBearerPrefixIsRejected() throws Exception {
        expectUnauthorized(withHeader("superday-demo-token"));
    }

    @Test
    void aWrongTokenIsRejected() throws Exception {
        expectUnauthorized(withHeader("Bearer wrong"));
    }

    @Test
    void aBearerPrefixWithNoTokenIsRejected() throws Exception {
        expectUnauthorized(withHeader("Bearer "));
    }

    @Test
    void aLowerCaseSchemeIsRejected() throws Exception {
        expectUnauthorized(withHeader("bearer superday-demo-token"));
    }

    @Test
    void theValidHeaderReachesTheEndpoint() throws Exception {
        withHeader("Bearer superday-demo-token")
                .andExpect(status().isOk())
                .andExpect(content().string("reached"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/portfolios/probe", "/clients/probe", "/holdings/probe" })
    void everyRouteFamilyIsProtected(String path) throws Exception {
        expectUnauthorized(mockMvc.perform(get(path)));
    }

    @Test
    void anUnknownRouteWithoutATokenIs401Not404() throws Exception {
        expectUnauthorized(mockMvc.perform(get("/no-such-route")));
    }

    private ResultActions withHeader(String authorization) throws Exception {
        return mockMvc.perform(get("/portfolios/probe").header("Authorization", authorization));
    }

    private static void expectUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().json(UNAUTHORIZED_BODY, JsonCompareMode.STRICT));
    }

    @RestController
    static class ProbeController {

        @GetMapping({ "/portfolios/probe", "/clients/probe", "/holdings/probe" })
        String probe() {
            return "reached";
        }

    }

}
