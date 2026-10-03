package ca.en.solution.client;

import java.math.BigDecimal;

/**
 * One portfolio's totals at full precision, summed over its holdings.
 */
public record PortfolioTotals(BigDecimal marketValue, BigDecimal dayChangeAmount) {
}
