package ca.en.solution.client;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(String portfolioId, String label, BigDecimal totalMarketValue) {
}
