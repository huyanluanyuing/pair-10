package ca.en.solution.holding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class HoldingCalculatorTest {

    // Seed portfolio P-9001. Total market value: 27300 + 21630 + 0 = 48930.
    private static final HoldingPosition AAPL = position("AAPL", "120", "200", "227.5", "225");
    private static final HoldingPosition BND = position("BND", "300", "74", "72.1", "73");
    private static final HoldingPosition ZERO = position("ZERO", "0", null, "12", "10");

    @Test
    void aHoldingIsValuedFromItsQuantityPricesAndCost() {
        HoldingResponse aapl = valued(AAPL, BND, ZERO).get(0);

        // 120 x 227.5 = 27300
        assertThat(aapl.marketValue()).isEqualByComparingTo("27300");
        // (227.5 - 200) x 120 = 27.5 x 120 = 3300
        assertThat(aapl.unrealizedGainLoss()).isEqualByComparingTo("3300");
        // (227.5 - 225) x 120 = 2.5 x 120 = 300
        assertThat(aapl.dayChangeAmount()).isEqualByComparingTo("300");
        // 2.5 / 225 = 0.011111...
        assertThat(aapl.dayChangePercent()).isEqualByComparingTo("0.011111");
    }

    @Test
    void aHoldingBelowItsCostAndPreviousCloseHasNegativeChanges() {
        HoldingResponse bnd = valued(AAPL, BND, ZERO).get(1);

        // 300 x 72.1 = 21630
        assertThat(bnd.marketValue()).isEqualByComparingTo("21630");
        // (72.1 - 74) x 300 = -1.9 x 300 = -570
        assertThat(bnd.unrealizedGainLoss()).isEqualByComparingTo("-570");
        // (72.1 - 73) x 300 = -0.9 x 300 = -270
        assertThat(bnd.dayChangeAmount()).isEqualByComparingTo("-270");
        // -0.9 / 73 = -0.0123287..., -0.012329 at six decimals
        assertThat(bnd.dayChangePercent()).isEqualByComparingTo("-0.012329");
    }

    @Test
    void weightIsTheHoldingsShareOfThePortfolioTotal() {
        List<HoldingResponse> holdings = valued(AAPL, BND, ZERO);

        // 27300 / 48930 = 0.5579399..., 21630 / 48930 = 0.4420600...
        assertThat(holdings.get(0).weightPercent()).isEqualByComparingTo("0.557940");
        assertThat(holdings.get(1).weightPercent()).isEqualByComparingTo("0.442060");
    }

    @Test
    void weightsAreNotCorrectedToSumToOne() {
        HoldingPosition third = position("A", "1", "10", "10", "10");

        List<HoldingResponse> holdings = valued(third, third, third);

        // 10 / 30 = 0.333333 each; the sum is 0.999999 and stays that way
        assertThat(holdings).extracting(HoldingResponse::weightPercent)
                .allSatisfy(weight -> assertThat(weight).isEqualByComparingTo("0.333333"));
    }

    @Test
    void aZeroQuantityHoldingHasZeroAmountsAndKeepsItsDayChangePercent() {
        HoldingResponse zero = valued(AAPL, BND, ZERO).get(2);

        assertThat(zero.marketValue()).isEqualByComparingTo("0");
        assertThat(zero.weightPercent()).isEqualByComparingTo("0");
        assertThat(zero.unrealizedGainLoss()).isEqualByComparingTo("0");
        assertThat(zero.dayChangeAmount()).isEqualByComparingTo("0");
        // (12 - 10) / 10 = 0.2, which does not depend on quantity
        assertThat(zero.dayChangePercent()).isEqualByComparingTo("0.2");
        assertThat(zero.costBasisPerShare()).isNull();
    }

    @Test
    void aZeroPreviousCloseGivesNoDayChangePercent() {
        // Seed holding NEW, alone in P-9002
        HoldingResponse held = valued(position("NEW", "10", "40", "50", "0")).get(0);

        // 10 x 50 = 500
        assertThat(held.marketValue()).isEqualByComparingTo("500");
        // 500 / 500 = 1
        assertThat(held.weightPercent()).isEqualByComparingTo("1");
        // (50 - 40) x 10 = 100
        assertThat(held.unrealizedGainLoss()).isEqualByComparingTo("100");
        // (50 - 0) x 10 = 500
        assertThat(held.dayChangeAmount()).isEqualByComparingTo("500");
        assertThat(held.dayChangePercent()).isNull();
    }

    @Test
    void aPortfolioWorthNothingGivesZeroWeightsWithoutDividingByZero() {
        HoldingResponse zero = valued(ZERO).get(0);

        assertThat(zero.weightPercent()).isEqualByComparingTo("0");
    }

    @Test
    void noHoldingsGiveAnEmptyList() {
        assertThat(valued()).isEmpty();
    }

    @Test
    void outputMoneyHasTwoDecimalsAndRatiosHaveSix() {
        HoldingResponse aapl = valued(AAPL, BND, ZERO).get(0);

        assertThat(aapl.price()).isEqualTo(new BigDecimal("227.50"));
        assertThat(aapl.marketValue()).isEqualTo(new BigDecimal("27300.00"));
        assertThat(aapl.weightPercent()).isEqualTo(new BigDecimal("0.557940"));
    }

    private static List<HoldingResponse> valued(HoldingPosition... positions) {
        return HoldingCalculator.value(List.of(positions)).stream().map(HoldingResponse::from).toList();
    }

    private static HoldingPosition position(String ticker, String quantity, String cost, String price,
            String previousClose) {
        return new HoldingPosition(ticker, ticker + " name", "Equity", new BigDecimal(quantity),
                cost == null ? null : new BigDecimal(cost), new BigDecimal(price), new BigDecimal(previousClose));
    }

}
