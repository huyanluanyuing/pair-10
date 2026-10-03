package ca.en.solution.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ca.en.solution.holding.HoldingPosition;
import ca.en.solution.holding.HoldingValuation;

class HouseholdCalculatorTest {

    @Test
    void aPortfoliosTotalsAreTheSumsOverItsHoldings() {
        // Seed P-9001: 27300 + 21630 + 0 = 48930; 300 - 270 + 0 = 30
        PortfolioTotals totals = HouseholdCalculator.totalsOf(List.of(
                valuation("27300", "300"), valuation("21630", "-270"), valuation("0", "0")));

        assertThat(totals.marketValue()).isEqualByComparingTo("48930");
        assertThat(totals.dayChangeAmount()).isEqualByComparingTo("30");
    }

    @Test
    void aPortfolioWithoutHoldingsHasZeroTotals() {
        PortfolioTotals totals = HouseholdCalculator.totalsOf(List.of());

        assertThat(totals.marketValue()).isEqualByComparingTo("0");
        assertThat(totals.dayChangeAmount()).isEqualByComparingTo("0");
    }

    @Test
    void theHouseholdSumsItsPortfoliosAndCountsEmptyOnes() {
        // Seed client abc123: P-9001 (48930, +30), P-9002 (500, +500), P-EMPTY (0, 0)
        HouseholdSummaryResponse summary = HouseholdCalculator.summarize("abc123", List.of(
                totals("48930", "30"), totals("500", "500"), totals("0", "0")));

        assertThat(summary.clientId()).isEqualTo("abc123");
        // 48930 + 500 + 0 = 49430
        assertThat(summary.totalMarketValue()).isEqualByComparingTo("49430");
        assertThat(summary.portfolioCount()).isEqualTo(3);
        // 30 + 500 + 0 = 530
        assertThat(summary.dayChangeAmount()).isEqualByComparingTo("530");
        // previous value 49430 - 530 = 48900; 530 / 48900 = 0.0108384...
        assertThat(summary.dayChangePercent()).isEqualByComparingTo("0.010838");
    }

    @Test
    void aSinglePortfolioHouseholdEqualsThatPortfolio() {
        // Seed client single-client: P-SINGLE (2275, +25); 25 / 2250 = 0.011111...
        HouseholdSummaryResponse summary = HouseholdCalculator.summarize("single-client",
                List.of(totals("2275", "25")));

        assertThat(summary.totalMarketValue()).isEqualByComparingTo("2275");
        assertThat(summary.portfolioCount()).isEqualTo(1);
        assertThat(summary.dayChangeAmount()).isEqualByComparingTo("25");
        assertThat(summary.dayChangePercent()).isEqualByComparingTo("0.011111");
    }

    @Test
    void dayChangePercentIsWeightedByValueNotAPlainAverage() {
        // A was 1000 and gains 100 (+10%); B was 9000 and loses 90 (-1%).
        // Weighted: (100 - 90) / (1000 + 9000) = 10 / 10000 = 0.001. Plain average would be 0.045.
        HouseholdSummaryResponse summary = HouseholdCalculator.summarize("c",
                List.of(totals("1100", "100"), totals("8910", "-90")));

        assertThat(summary.totalMarketValue()).isEqualByComparingTo("10010");
        assertThat(summary.dayChangeAmount()).isEqualByComparingTo("10");
        assertThat(summary.dayChangePercent()).isEqualByComparingTo("0.001");
    }

    @Test
    void aHouseholdWorthNothingAtThePreviousCloseHasNoDayChangePercent() {
        // 500 today, +500 today: the previous value was 0
        HouseholdSummaryResponse summary = HouseholdCalculator.summarize("c", List.of(totals("500", "500")));

        assertThat(summary.dayChangeAmount()).isEqualByComparingTo("500");
        assertThat(summary.dayChangePercent()).isNull();
    }

    @Test
    void aClientWithoutPortfoliosGetsZerosAndNoDayChangePercent() {
        HouseholdSummaryResponse summary = HouseholdCalculator.summarize("c", List.of());

        assertThat(summary.totalMarketValue()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.portfolioCount()).isZero();
        assertThat(summary.dayChangeAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.dayChangePercent()).isNull();
    }

    private static PortfolioTotals totals(String marketValue, String dayChangeAmount) {
        return new PortfolioTotals(new BigDecimal(marketValue), new BigDecimal(dayChangeAmount));
    }

    private static HoldingValuation valuation(String marketValue, String dayChangeAmount) {
        return new HoldingValuation(
                new HoldingPosition("T", "T", "Equity", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE),
                new BigDecimal(marketValue), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(dayChangeAmount),
                BigDecimal.ZERO);
    }

}
