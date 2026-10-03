package ca.en.solution.holding;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Task 5 allocation values, calculated before response rounding. */
public final class AllocationCalculator {

    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private AllocationCalculator() {
    }

    public static List<AllocationResponse> allocate(List<HoldingValuation> valuations) {
        Map<String, BigDecimal> valuesByClass = new TreeMap<>();
        for (HoldingValuation valuation : valuations) {
            valuesByClass.merge(valuation.position().assetClass(), valuation.marketValue(), BigDecimal::add);
        }
        BigDecimal total = valuesByClass.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return valuesByClass.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> AllocationResponse.from(entry.getKey(), entry.getValue(),
                        entry.getValue().signum() == 0 ? BigDecimal.ZERO : entry.getValue().divide(total, PRECISION)))
                .toList();
    }
}
