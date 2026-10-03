package ca.en.solution.holding;

import java.math.BigDecimal;

import ca.en.solution.common.Rounding;
import ca.en.solution.currency.CurrencyContext;

public record CurrencyHoldingResponse(String ticker, String name, String assetClass, BigDecimal quantity,
        BigDecimal costBasisPerShare, BigDecimal price, BigDecimal previousClosePrice, BigDecimal marketValue,
        BigDecimal weightPercent, BigDecimal unrealizedGainLoss, BigDecimal dayChangeAmount,
        BigDecimal dayChangePercent, String currency, BigDecimal exchangeRate) {

    public static CurrencyHoldingResponse from(HoldingValuation valuation, CurrencyContext context) {
        HoldingPosition position = valuation.position();
        return new CurrencyHoldingResponse(position.ticker(), position.name(), position.assetClass(),
                position.quantity(), context.convertMoney(position.costBasisPerShare()),
                context.convertMoney(position.price()), context.convertMoney(position.previousClosePrice()),
                context.convertMoney(valuation.marketValue()), Rounding.ratio(valuation.weightPercent()),
                context.convertMoney(valuation.unrealizedGainLoss()), context.convertMoney(valuation.dayChangeAmount()),
                Rounding.ratio(valuation.dayChangePercent()), context.currency(), context.exchangeRate());
    }
}
