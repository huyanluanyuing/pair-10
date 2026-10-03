package ca.en.solution.history;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "history.file=classpath:performance-history.json")
@AutoConfigureMockMvc
class PerformanceHistoryEndToEndTest {

    private static final String AUTH = "Bearer superday-demo-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void filtersAndSortsHistoryForEverySupportedRange() throws Exception {
        // Fixed UTC today: 2026-10-03. 1D starts 10-02; 1M starts 09-03;
        // YTD starts 01-01; 1Y starts 2025-10-03.
        mockMvc.perform(get("/portfolios/P-9001/performance-history").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"date":"2025-09-30","marketValue":45000.00},
                          {"date":"2025-10-03","marketValue":46000.00},
                          {"date":"2025-12-31","marketValue":47000.00},
                          {"date":"2026-01-01","marketValue":48000.00},
                          {"date":"2026-09-03","marketValue":48500.00},
                          {"date":"2026-10-02","marketValue":48900.00},
                          {"date":"2026-10-03","marketValue":48930.00}
                        ]
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/P-9001/performance-history?range=1D").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"date":"2026-10-02","marketValue":48900.00},
                         {"date":"2026-10-03","marketValue":48930.00}]
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/P-9001/performance-history?range=1M").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"date":"2026-09-03","marketValue":48500.00},
                         {"date":"2026-10-02","marketValue":48900.00},
                         {"date":"2026-10-03","marketValue":48930.00}]
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/P-9001/performance-history?range=YTD").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"date":"2026-01-01","marketValue":48000.00},
                         {"date":"2026-09-03","marketValue":48500.00},
                         {"date":"2026-10-02","marketValue":48900.00},
                         {"date":"2026-10-03","marketValue":48930.00}]
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/P-9001/performance-history?range=1Y").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"date":"2025-10-03","marketValue":46000.00},
                         {"date":"2025-12-31","marketValue":47000.00},
                         {"date":"2026-01-01","marketValue":48000.00},
                         {"date":"2026-09-03","marketValue":48500.00},
                         {"date":"2026-10-02","marketValue":48900.00},
                         {"date":"2026-10-03","marketValue":48930.00}]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void preservesTheAvailableHistoryAndReturnsStructuredErrors() throws Exception {
        mockMvc.perform(get("/portfolios/P-9002/performance-history?range=1Y").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"date":"2026-08-05","marketValue":450.00},
                         {"date":"2026-10-03","marketValue":500.00}]
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/P-EMPTY/performance-history").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/P-9001/performance-history?range=all").header("Authorization", AUTH))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"error":"bad_request","message":"range must be one of: 1D, 1M, YTD, 1Y, All."}
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/portfolios/UNKNOWN/performance-history").header("Authorization", AUTH))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"error":"not_found","message":"Portfolio 'UNKNOWN' was not found."}
                        """, JsonCompareMode.STRICT));
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
