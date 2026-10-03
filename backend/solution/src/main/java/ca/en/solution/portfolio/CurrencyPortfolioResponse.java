package ca.en.solution.portfolio;

import java.math.BigDecimal;

import ca.en.solution.common.Rounding;
import ca.en.solution.currency.CurrencyContext;

public record CurrencyPortfolioResponse(String portfolioId, String clientId, String label, String currency,
        BigDecimal exchangeRate, BigDecimal totalMarketValue, BigDecimal dayChangeAmount,
        BigDecimal dayChangePercent, BigDecimal totalReturnSinceInception, String asOf) {

    public static CurrencyPortfolioResponse from(PortfolioMetadata metadata, CurrencyContext context) {
        return new CurrencyPortfolioResponse(metadata.portfolioId(), metadata.clientId(), metadata.label(),
                context.currency(), context.exchangeRate(), context.convertMoney(metadata.totalMarketValue()),
                context.convertMoney(metadata.dayChangeAmount()), Rounding.ratio(metadata.dayChangePercent()),
                Rounding.ratio(metadata.totalReturnSinceInception()), metadata.asOf());
    }
}
