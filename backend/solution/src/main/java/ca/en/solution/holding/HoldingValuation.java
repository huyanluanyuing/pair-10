package ca.en.solution.holding;

import java.math.BigDecimal;

/**
 * A holding with its calculated fields at full precision, before rounding.
 * The day change percent is null when the previous close is 0.
 */
public record HoldingValuation(HoldingPosition position, BigDecimal marketValue, BigDecimal weightPercent,
        BigDecimal unrealizedGainLoss, BigDecimal dayChangeAmount, BigDecimal dayChangePercent) {
}
