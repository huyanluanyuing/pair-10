package ca.en.solution.history;

import java.math.BigDecimal;
import java.time.LocalDate;

import ca.en.solution.common.Rounding;

public record PerformanceSnapshot(LocalDate date, BigDecimal marketValue) {

    PerformanceSnapshot rounded() {
        return new PerformanceSnapshot(date, Rounding.money(marketValue));
    }
}
