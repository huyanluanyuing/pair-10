package ca.en.solution.holding;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Task 2's formulas, at full precision. Weights are relative to the total of the positions given.
 */
public final class HoldingCalculator {

    // Working precision for the ratios; rounding for display happens at output.
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private HoldingCalculator() {
    }

    public static List<HoldingValuation> value(List<HoldingPosition> positions) {
        BigDecimal portfolioTotal = positions.stream()
                .map(HoldingCalculator::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return positions.stream().map(position -> value(position, portfolioTotal)).toList();
    }

    private static HoldingValuation value(HoldingPosition position, BigDecimal portfolioTotal) {
        BigDecimal marketValue = marketValue(position);
        BigDecimal dayChangePerShare = position.price().subtract(position.previousClosePrice());
        return new HoldingValuation(
                position,
                marketValue,
                // A zero market value also covers a portfolio whose total is zero.
                marketValue.signum() == 0 ? BigDecimal.ZERO : marketValue.divide(portfolioTotal, PRECISION),
                // The cost basis is null only when nothing is held.
                position.quantity().signum() == 0
                        ? BigDecimal.ZERO
                        : position.price().subtract(position.costBasisPerShare()).multiply(position.quantity()),
                dayChangePerShare.multiply(position.quantity()),
                position.previousClosePrice().signum() == 0
                        ? null
                        : dayChangePerShare.divide(position.previousClosePrice(), PRECISION));
    }

    private static BigDecimal marketValue(HoldingPosition position) {
        return position.quantity().multiply(position.price());
    }

}
