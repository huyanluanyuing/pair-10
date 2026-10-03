package ca.en.solution.holding;

import java.math.BigDecimal;

import ca.en.solution.common.Rounding;

public record HoldingResponse(String ticker, String name, String assetClass, BigDecimal quantity,
        BigDecimal costBasisPerShare, BigDecimal price, BigDecimal previousClosePrice, BigDecimal marketValue,
        BigDecimal weightPercent, BigDecimal unrealizedGainLoss, BigDecimal dayChangeAmount,
        BigDecimal dayChangePercent) {

    public static HoldingResponse from(HoldingValuation valuation) {
        HoldingPosition position = valuation.position();
        return new HoldingResponse(
                position.ticker(),
                position.name(),
                position.assetClass(),
                position.quantity(),
                Rounding.money(position.costBasisPerShare()),
                Rounding.money(position.price()),
                Rounding.money(position.previousClosePrice()),
                Rounding.money(valuation.marketValue()),
                Rounding.ratio(valuation.weightPercent()),
                Rounding.money(valuation.unrealizedGainLoss()),
                Rounding.money(valuation.dayChangeAmount()),
                Rounding.ratio(valuation.dayChangePercent()));
    }

}
