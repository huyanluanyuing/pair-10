package ca.en.solution.holding;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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
class HoldingsEndToEndTest {

    private static final String AUTH = "Bearer superday-demo-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theSeedPortfolioReturnsItsHoldingsInTickerOrderWithEveryCalculatedField() throws Exception {
        mockMvc.perform(get("/portfolios/P-9001/holdings").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                          "ticker": "AAPL",
                          "name": "Apple Inc.",
                          "assetClass": "Equity",
                          "quantity": 120,
                          "costBasisPerShare": 200.00,
                          "price": 227.50,
                          "previousClosePrice": 225.00,
                          "marketValue": 27300.00,
                          "weightPercent": 0.557940,
                          "unrealizedGainLoss": 3300.00,
                          "dayChangeAmount": 300.00,
                          "dayChangePercent": 0.011111
                        }, {
                          "ticker": "BND",
                          "name": "Vanguard Total Bond ETF",
                          "assetClass": "Fixed Income",
                          "quantity": 300,
                          "costBasisPerShare": 74.00,
                          "price": 72.10,
                          "previousClosePrice": 73.00,
                          "marketValue": 21630.00,
                          "weightPercent": 0.442060,
                          "unrealizedGainLoss": -570.00,
                          "dayChangeAmount": -270.00,
                          "dayChangePercent": -0.012329
                        }, {
                          "ticker": "ZERO",
                          "name": "Closed Position",
                          "assetClass": "Equity",
                          "quantity": 0,
                          "costBasisPerShare": null,
                          "price": 12.00,
                          "previousClosePrice": 10.00,
                          "marketValue": 0.00,
                          "weightPercent": 0.000000,
                          "unrealizedGainLoss": 0.00,
                          "dayChangeAmount": 0.00,
                          "dayChangePercent": 0.200000
                        }]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void theSeedHoldingWithAZeroPreviousCloseReturnsANullDayChangePercent() throws Exception {
        mockMvc.perform(get("/portfolios/P-9002/holdings").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                          "ticker": "NEW",
                          "name": "New Security",
                          "assetClass": "Equity",
                          "quantity": 10,
                          "costBasisPerShare": 40.00,
                          "price": 50.00,
                          "previousClosePrice": 0.00,
                          "marketValue": 500.00,
                          "weightPercent": 1.000000,
                          "unrealizedGainLoss": 100.00,
                          "dayChangeAmount": 500.00,
                          "dayChangePercent": null
                        }]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void theEmptySeedPortfolioReturnsAnEmptyArray() throws Exception {
        mockMvc.perform(get("/portfolios/P-EMPTY/holdings").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void anUnknownPortfolioWithoutATokenIs401Not404() throws Exception {
        mockMvc.perform(get("/portfolios/UNKNOWN/holdings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void anUnknownPortfolioReturns404() throws Exception {
        mockMvc.perform(get("/portfolios/UNKNOWN/holdings").header("Authorization", AUTH))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        { "error": "not_found", "message": "Portfolio 'UNKNOWN' was not found." }
                        """, JsonCompareMode.STRICT));
    }

}
