package ca.en.solution.holding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class AllocationCalculatorTest {

    @Test
    void ordersByUnroundedValueAndKeepsAZeroValueClass() {
        List<AllocationResponse> allocation = AllocationCalculator.allocate(List.of(
                valuation("Alpha", "1.003"),
                valuation("Zebra", "1.004"),
                valuation("Cash", "0")));

        assertThat(allocation).extracting(AllocationResponse::assetClass)
                .containsExactly("Zebra", "Alpha", "Cash");
        assertThat(allocation).extracting(AllocationResponse::value)
                .containsExactly(new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("0.00"));
        assertThat(allocation).extracting(AllocationResponse::percent)
                .containsExactly(new BigDecimal("0.500249"), new BigDecimal("0.499751"), BigDecimal.ZERO.setScale(6));
    }

    private static HoldingValuation valuation(String assetClass, String marketValue) {
        return new HoldingValuation(
                new HoldingPosition(assetClass, assetClass, assetClass, BigDecimal.ONE, BigDecimal.ONE,
                        BigDecimal.ONE, BigDecimal.ONE),
                new BigDecimal(marketValue), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
