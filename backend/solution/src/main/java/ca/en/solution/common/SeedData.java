package ca.en.solution.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ca.en.solution.ledger.TransactionType;

/**
 * Our own data, held in memory. Loaded once from seed.json and never changed.
 * A holding row has no quantity or cost: those come from replaying its transactions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedData(List<PortfolioRow> portfolios, List<HoldingRow> holdings, List<TransactionRow> transactions) {

    public record PortfolioRow(String portfolioId, String clientId, String label, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HoldingRow(String holdingId, String portfolioId, String ticker, String name, String assetClass,
            BigDecimal price, BigDecimal previousClosePrice) {
    }

    public record TransactionRow(String transactionId, String holdingId, TransactionType type, BigDecimal quantity,
            BigDecimal price, LocalDate date) {
    }

    public boolean hasPortfolio(String portfolioId) {
        return portfolios.stream().anyMatch(portfolio -> portfolio.portfolioId().equals(portfolioId));
    }

    public List<HoldingRow> holdingsOf(String portfolioId) {
        return holdings.stream().filter(holding -> holding.portfolioId().equals(portfolioId)).toList();
    }

    public List<TransactionRow> transactionsOf(String holdingId) {
        return transactions.stream().filter(transaction -> transaction.holdingId().equals(holdingId)).toList();
    }

}
