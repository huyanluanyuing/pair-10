package ca.en.solution.holding;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AllocationEndToEndTest {

    private static final String AUTH = "Bearer superday-demo-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aggregatesMarketValuesByAssetClassInDescendingValueOrder() throws Exception {
        // Equity: 27300 + 0 = 27300; Fixed Income: 21630; total: 48930.
        mockMvc.perform(get("/portfolios/P-9001/allocation").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"assetClass":"Equity","value":27300.00,"percent":0.557940},
                         {"assetClass":"Fixed Income","value":21630.00,"percent":0.442060}]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void supportsSingleClassEmptyAndUnknownPortfolios() throws Exception {
        mockMvc.perform(get("/portfolios/P-SINGLE/allocation").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"assetClass":"Equity","value":2275.00,"percent":1.000000}]
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/P-EMPTY/allocation").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/UNKNOWN/allocation").header("Authorization", AUTH))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"error":"not_found","message":"Portfolio 'UNKNOWN' was not found."}
                        """, JsonCompareMode.STRICT));
    }
}
