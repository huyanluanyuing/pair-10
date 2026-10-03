package ca.en.solution.client;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import ca.en.solution.common.Rounding;
import ca.en.solution.holding.HoldingValuation;

/**
 * Task 6's totals. The household day change percent is weighted by each portfolio's previous value:
 * total day change divided by total previous-close value.
 */
public final class HouseholdCalculator {

    // Working precision for the ratio; rounding for display happens at output.
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private HouseholdCalculator() {
    }

    public static PortfolioTotals totalsOf(List<HoldingValuation> valuations) {
        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal dayChangeAmount = BigDecimal.ZERO;
        for (HoldingValuation valuation : valuations) {
            marketValue = marketValue.add(valuation.marketValue());
            dayChangeAmount = dayChangeAmount.add(valuation.dayChangeAmount());
        }
        return new PortfolioTotals(marketValue, dayChangeAmount);
    }

    public static HouseholdSummaryResponse summarize(String clientId, List<PortfolioTotals> portfolios) {
        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal dayChangeAmount = BigDecimal.ZERO;
        for (PortfolioTotals portfolio : portfolios) {
            marketValue = marketValue.add(portfolio.marketValue());
            dayChangeAmount = dayChangeAmount.add(portfolio.dayChangeAmount());
        }
        // quantity x price minus (price - previous close) x quantity is quantity x previous close.
        BigDecimal previousValue = marketValue.subtract(dayChangeAmount);
        BigDecimal dayChangePercent = previousValue.signum() == 0
                ? null
                : dayChangeAmount.divide(previousValue, PRECISION);
        return new HouseholdSummaryResponse(clientId, Rounding.money(marketValue), portfolios.size(),
                Rounding.money(dayChangeAmount), Rounding.ratio(dayChangePercent));
    }

}
