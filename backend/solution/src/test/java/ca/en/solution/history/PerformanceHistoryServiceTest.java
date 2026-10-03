package ca.en.solution.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import ca.en.solution.common.SeedData;
import ca.en.solution.common.SeedData.PortfolioRow;

class PerformanceHistoryServiceTest {

    @Test
    void oneMonthRangeUsesCalendarMonthClampingAtMonthEnd() {
        HistoryRepository history = portfolioId -> List.of(
                snapshot("2026-02-27"), snapshot("2026-02-28"), snapshot("2026-03-31"));
        PerformanceHistoryService service = new PerformanceHistoryService(seedData(), history,
                Clock.fixed(Instant.parse("2026-03-31T12:00:00Z"), ZoneOffset.UTC));

        assertThat(service.historyOf("P-1", "1M")).extracting(PerformanceSnapshot::date)
                .containsExactly(LocalDate.parse("2026-02-28"), LocalDate.parse("2026-03-31"));
    }

    private static SeedData seedData() {
        return new SeedData(List.of(new PortfolioRow("P-1", "C-1", "Main", "CAD")), List.of(), List.of());
    }

    private static PerformanceSnapshot snapshot(String date) {
        return new PerformanceSnapshot(LocalDate.parse(date), BigDecimal.ONE);
    }
}
