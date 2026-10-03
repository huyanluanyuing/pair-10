package ca.en.solution.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The real seed data through the whole app. Expected values are worked by hand in docs/requirements.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClientEndToEndTest {

    private static final String AUTH = "Bearer superday-demo-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aClientWithSeveralPortfoliosGetsThemAllWithTheirTotals() throws Exception {
        mockMvc.perform(get("/clients/abc123/portfolios").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"portfolioId":"P-9001","label":"Taxable Brokerage","totalMarketValue":48930.00},
                         {"portfolioId":"P-9002","label":"Retirement Account","totalMarketValue":500.00},
                         {"portfolioId":"P-EMPTY","label":"Empty Account","totalMarketValue":0.00}]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void theHouseholdSummaryAggregatesAllOfAClientsPortfolios() throws Exception {
        // 48930 + 500 + 0 = 49430; 30 + 500 + 0 = 530; 530 / 48900 = 0.010838
        mockMvc.perform(get("/clients/abc123/household-summary").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"clientId":"abc123","totalMarketValue":49430.00,"portfolioCount":3,
                         "dayChangeAmount":530.00,"dayChangePercent":0.010838}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void aClientWithOnePortfolioGetsValidDataFromBothRoutes() throws Exception {
        mockMvc.perform(get("/clients/single-client/portfolios").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"portfolioId":"P-SINGLE","label":"Single Asset Class","totalMarketValue":2275.00}]
                        """, JsonCompareMode.STRICT));

        // 25 / 2250 = 0.011111
        mockMvc.perform(get("/clients/single-client/household-summary").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"clientId":"single-client","totalMarketValue":2275.00,"portfolioCount":1,
                         "dayChangeAmount":25.00,"dayChangePercent":0.011111}
                        """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/clients/UNKNOWN/portfolios", "/clients/UNKNOWN/household-summary" })
    void anUnknownClientReturns404OnBothRoutes(String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", AUTH))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"error":"not_found","message":"Client 'UNKNOWN' was not found."}
                        """, JsonCompareMode.STRICT));
    }

}
