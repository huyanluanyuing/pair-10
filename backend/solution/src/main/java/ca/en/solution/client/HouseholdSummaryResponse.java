package ca.en.solution.client;

import java.math.BigDecimal;

/**
 * The day change percent is null when the household had no value at the previous close.
 */
public record HouseholdSummaryResponse(String clientId, BigDecimal totalMarketValue, int portfolioCount,
        BigDecimal dayChangeAmount, BigDecimal dayChangePercent) {
}
