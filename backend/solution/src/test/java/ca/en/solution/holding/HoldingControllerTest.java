package ca.en.solution.holding;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import ca.en.solution.portfolio.PortfolioNotFoundException;

@WebMvcTest(HoldingController.class)
class HoldingControllerTest {

    private static final String AUTH = "Bearer superday-demo-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HoldingService holdingService;

    @Test
    void holdingsAreReturnedAsAnArrayWithEveryFieldIncludingNulls() throws Exception {
        given(holdingService.holdingsOf("P-9002")).willReturn(List.of(new HoldingResponse(
                "NEW", "New Security", "Equity", new BigDecimal("10"), null, new BigDecimal("50.00"),
                new BigDecimal("0.00"), new BigDecimal("500.00"), new BigDecimal("1.000000"),
                new BigDecimal("100.00"), new BigDecimal("500.00"), null)));

        mockMvc.perform(get("/portfolios/P-9002/holdings").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                          "ticker": "NEW",
                          "name": "New Security",
                          "assetClass": "Equity",
                          "quantity": 10,
                          "costBasisPerShare": null,
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
    void aPortfolioWithoutHoldingsReturnsAnEmptyArray() throws Exception {
        given(holdingService.holdingsOf("P-EMPTY")).willReturn(List.of());

        mockMvc.perform(get("/portfolios/P-EMPTY/holdings").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void anUnknownPortfolioReturns404WithTheErrorBody() throws Exception {
        given(holdingService.holdingsOf("UNKNOWN")).willThrow(new PortfolioNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/portfolios/UNKNOWN/holdings").header("Authorization", AUTH))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        { "error": "not_found", "message": "Portfolio 'UNKNOWN' was not found." }
                        """, JsonCompareMode.STRICT))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

}
