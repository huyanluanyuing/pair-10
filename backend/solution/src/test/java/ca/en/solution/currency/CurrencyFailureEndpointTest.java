package ca.en.solution.currency;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CurrencyFailureEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheStructured502WhenTheLiveRateIsUnavailable() throws Exception {
        mockMvc.perform(get("/portfolios/P-9001/holdings?currency=USD")
                .header("Authorization", "Bearer superday-demo-token"))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("""
                        {"error":"currency_rate_unavailable","message":"Currency rate service is unavailable."}
                        """, JsonCompareMode.STRICT));
    }

    @TestConfiguration
    static class UnavailableRateConfig {
        @Bean
        @Primary
        CurrencyRateClient unavailableCurrencyRateClient() {
            return () -> {
                throw new CurrencyRateUnavailableException();
            };
        }
    }
}
