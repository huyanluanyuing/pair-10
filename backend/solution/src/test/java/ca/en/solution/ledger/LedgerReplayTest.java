package ca.en.solution.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class LedgerReplayTest {

    @Test
    void multipleBuysAndAPartialSellGiveTheWeightedAverageCost() {
        // seed h1: (100 x 190 + 50 x 220) / 150 = 30000 / 150 = 200; 150 - 30 = 120
        Position position = LedgerReplay.replay(List.of(
                buy("100", "190", "2025-01-02"),
                buy("50", "220", "2025-01-03"),
                sell("30", "230", "2025-02-01")));

        assertThat(position.currentQuantity()).isEqualByComparingTo("120");
        assertThat(position.averageCostBasisPerShare()).isEqualByComparingTo("200");
    }

    @Test
    void aSellLeavesTheAverageCostOfTheRemainingSharesUnchanged() {
        Position position = LedgerReplay.replay(List.of(
                buy("10", "10", "2025-01-01"),
                sell("4", "99", "2025-01-02")));

        assertThat(position.currentQuantity()).isEqualByComparingTo("6");
        assertThat(position.averageCostBasisPerShare()).isEqualByComparingTo("10");
    }

    @Test
    void sellingDownToExactlyZeroGivesZeroQuantityAndNoAverageCost() {
        // seed h3
        Position position = LedgerReplay.replay(List.of(
                buy("5", "10", "2025-01-02"),
                sell("5", "12", "2025-01-03")));

        assertThat(position.currentQuantity()).isEqualByComparingTo("0");
        assertThat(position.averageCostBasisPerShare()).isNull();
    }

    @Test
    void sellingMoreThanIsHeldIsRejected() {
        // seed ledgerCases.oversell
        assertThatThrownBy(() -> LedgerReplay.replay(List.of(
                buy("5", "10", "2025-01-01"),
                sell("6", "12", "2025-01-02"))))
                .isInstanceOf(InvalidLedgerException.class);
    }

    @Test
    void transactionsGivenOutOfDateOrderAreSortedBeforeReplay() {
        // seed ledgerCases.outOfOrder: 5 - 2 = 3, bought at 10
        Position position = LedgerReplay.replay(List.of(
                sell("2", "12", "2025-01-03"),
                buy("5", "10", "2025-01-01")));

        assertThat(position.currentQuantity()).isEqualByComparingTo("3");
        assertThat(position.averageCostBasisPerShare()).isEqualByComparingTo("10");
    }

    @Test
    void buyingAgainAfterClosingStartsANewAverageCost() {
        Position position = LedgerReplay.replay(List.of(
                buy("5", "10", "2025-01-01"),
                sell("5", "12", "2025-01-02"),
                buy("4", "20", "2025-01-03")));

        assertThat(position.currentQuantity()).isEqualByComparingTo("4");
        assertThat(position.averageCostBasisPerShare()).isEqualByComparingTo("20");
    }

    @Test
    void transactionsOnTheSameDateKeepTheirInputOrder() {
        // buys first would give (5 x 10 + 4 x 20) / 9 = 14.44; sells first would be an oversell
        Position position = LedgerReplay.replay(List.of(
                buy("5", "10", "2025-01-01"),
                sell("5", "12", "2025-01-01"),
                buy("4", "20", "2025-01-01")));

        assertThat(position.currentQuantity()).isEqualByComparingTo("4");
        assertThat(position.averageCostBasisPerShare()).isEqualByComparingTo("20");
    }

    @Test
    void anAverageCostThatDoesNotDivideEvenlyIsStillComputed() {
        // (1 x 10 + 2 x 20) / 3 = 50 / 3 = 16.666..., 16.67 at two decimals
        Position position = LedgerReplay.replay(List.of(
                buy("1", "10", "2025-01-01"),
                buy("2", "20", "2025-01-02")));

        assertThat(position.currentQuantity()).isEqualByComparingTo("3");
        assertThat(position.averageCostBasisPerShare().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo("16.67");
    }

    @Test
    void anEmptyHistoryGivesZeroQuantityAndNoAverageCost() {
        Position position = LedgerReplay.replay(List.of());

        assertThat(position.currentQuantity()).isEqualByComparingTo("0");
        assertThat(position.averageCostBasisPerShare()).isNull();
    }

    @Test
    void aZeroQuantityIsRejected() {
        assertThatThrownBy(() -> LedgerReplay.replay(List.of(buy("0", "10", "2025-01-01"))))
                .isInstanceOf(InvalidLedgerException.class);
    }

    @Test
    void aNegativeQuantityIsRejected() {
        assertThatThrownBy(() -> LedgerReplay.replay(List.of(buy("-1", "10", "2025-01-01"))))
                .isInstanceOf(InvalidLedgerException.class);
    }

    @Test
    void aNegativePriceIsRejected() {
        assertThatThrownBy(() -> LedgerReplay.replay(List.of(buy("1", "-10", "2025-01-01"))))
                .isInstanceOf(InvalidLedgerException.class);
    }

    @Test
    void aTransactionWithoutATypeIsRejected() {
        Transaction noType = new Transaction(null, new BigDecimal("1"), new BigDecimal("10"),
                LocalDate.parse("2025-01-01"));

        assertThatThrownBy(() -> LedgerReplay.replay(List.of(noType)))
                .isInstanceOf(InvalidLedgerException.class);
    }

    @Test
    void aTransactionWithoutADateIsRejected() {
        Transaction noDate = new Transaction(TransactionType.BUY, new BigDecimal("1"), new BigDecimal("10"), null);

        assertThatThrownBy(() -> LedgerReplay.replay(List.of(noDate)))
                .isInstanceOf(InvalidLedgerException.class);
    }

    private static Transaction buy(String quantity, String price, String date) {
        return new Transaction(TransactionType.BUY, new BigDecimal(quantity), new BigDecimal(price),
                LocalDate.parse(date));
    }

    private static Transaction sell(String quantity, String price, String date) {
        return new Transaction(TransactionType.SELL, new BigDecimal(quantity), new BigDecimal(price),
                LocalDate.parse(date));
    }

}
