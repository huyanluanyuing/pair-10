package ca.en.solution.portfolio;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PortfolioMetadata(
        String portfolioId,
        String clientId,
        String label,
        String currency,
        BigDecimal totalMarketValue,
        BigDecimal dayChangeAmount,
        BigDecimal dayChangePercent,
        BigDecimal totalReturnSinceInception,
        String asOf) {
}
