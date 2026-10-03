package ca.en.solution.holding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ca.en.solution.common.NotFoundException;
import ca.en.solution.common.SeedData;
import ca.en.solution.common.SeedData.HoldingRow;
import ca.en.solution.common.SeedData.PortfolioRow;
import ca.en.solution.common.SeedData.TransactionRow;
import ca.en.solution.ledger.TransactionType;

class HoldingServiceTest {

    // Stored out of ticker order on purpose, with a second portfolio that also holds AAPL.
    private static final SeedData DATA = new SeedData(
            List.of(new PortfolioRow("P-1", "c1", "Main", "CAD"),
                    new PortfolioRow("P-2", "c1", "Other", "CAD"),
                    new PortfolioRow("P-EMPTY", "c1", "Empty", "CAD")),
            List.of(holding("h2", "P-1", "BND", "72.1", "73"),
                    holding("h3", "P-1", "ZERO", "12", "10"),
                    holding("h1", "P-1", "AAPL", "227.5", "225"),
                    holding("h5", "P-2", "AAPL", "227.5", "225")),
            List.of(transaction("h1", TransactionType.BUY, "100", "190", "2025-01-02"),
                    transaction("h1", TransactionType.BUY, "50", "220", "2025-01-03"),
                    transaction("h1", TransactionType.SELL, "30", "230", "2025-02-01"),
                    transaction("h2", TransactionType.BUY, "300", "74", "2025-01-02"),
                    transaction("h3", TransactionType.BUY, "5", "10", "2025-01-02"),
                    transaction("h3", TransactionType.SELL, "5", "12", "2025-01-03"),
                    transaction("h5", TransactionType.BUY, "10", "200", "2025-01-02")));

    private final HoldingService service = new HoldingService(DATA);

    @Test
    void quantityAndCostBasisComeFromReplayingTheTransactions() {
        HoldingResponse aapl = service.holdingsOf("P-1").get(0);

        // 100 + 50 - 30 = 120; (100 x 190 + 50 x 220) / 150 = 200
        assertThat(aapl.ticker()).isEqualTo("AAPL");
        assertThat(aapl.quantity()).isEqualByComparingTo("120");
        assertThat(aapl.costBasisPerShare()).isEqualByComparingTo("200");
    }

    @Test
    void holdingsAreReturnedInTickerOrder() {
        assertThat(service.holdingsOf("P-1")).extracting(HoldingResponse::ticker)
                .containsExactly("AAPL", "BND", "ZERO");
    }

    @Test
    void aClosedPositionHasZeroQuantityAndNoCostBasis() {
        HoldingResponse zero = service.holdingsOf("P-1").get(2);

        assertThat(zero.quantity()).isEqualByComparingTo("0");
        assertThat(zero.costBasisPerShare()).isNull();
    }

    @Test
    void holdingsOfOtherPortfoliosAreLeftOutOfTheListAndTheWeights() {
        List<HoldingResponse> holdings = service.holdingsOf("P-1");

        // 27300 / (27300 + 21630): the 2275 held in P-2 is not in the total
        assertThat(holdings).hasSize(3);
        assertThat(holdings.get(0).weightPercent()).isEqualByComparingTo("0.557940");
    }

    @Test
    void aPortfolioWithoutHoldingsGivesAnEmptyList() {
        assertThat(service.holdingsOf("P-EMPTY")).isEmpty();
    }

    @Test
    void anUnknownPortfolioIsNotFound() {
        assertThatThrownBy(() -> service.holdingsOf("UNKNOWN")).isInstanceOf(NotFoundException.class);
    }

    private static HoldingRow holding(String holdingId, String portfolioId, String ticker, String price,
            String previousClose) {
        return new HoldingRow(holdingId, portfolioId, ticker, ticker + " name", "Equity", new BigDecimal(price),
                new BigDecimal(previousClose));
    }

    private static TransactionRow transaction(String holdingId, TransactionType type, String quantity, String price,
            String date) {
        return new TransactionRow("t", holdingId, type, new BigDecimal(quantity), new BigDecimal(price),
                LocalDate.parse(date));
    }

}
