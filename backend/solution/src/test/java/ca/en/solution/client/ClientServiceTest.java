package ca.en.solution.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ca.en.solution.common.NotFoundException;
import ca.en.solution.common.SeedData;
import ca.en.solution.common.SeedData.ClientRow;
import ca.en.solution.common.SeedData.HoldingRow;
import ca.en.solution.common.SeedData.PortfolioRow;
import ca.en.solution.common.SeedData.TransactionRow;
import ca.en.solution.holding.HoldingService;
import ca.en.solution.ledger.TransactionType;

class ClientServiceTest {

    // Portfolios stored out of id order; c2 belongs to someone else; c3 has no portfolios.
    private static final SeedData DATA = new SeedData(
            List.of(new ClientRow("c1", "One"), new ClientRow("c2", "Two"), new ClientRow("c3", "Three")),
            List.of(new PortfolioRow("P-B", "c1", "Second", "CAD"),
                    new PortfolioRow("P-X", "c2", "Other client", "CAD"),
                    new PortfolioRow("P-A", "c1", "First", "CAD")),
            List.of(new HoldingRow("h1", "P-A", "AAA", "AAA", "Equity", new BigDecimal("11"), new BigDecimal("10")),
                    new HoldingRow("h2", "P-B", "BBB", "BBB", "Equity", new BigDecimal("5"), new BigDecimal("5")),
                    new HoldingRow("h3", "P-X", "CCC", "CCC", "Equity", new BigDecimal("7"), new BigDecimal("7"))),
            List.of(buy("h1", "100", "10"), buy("h2", "10", "5"), buy("h3", "1000", "7")));

    private final ClientService service = new ClientService(DATA, new HoldingService(DATA));

    @Test
    void aClientsPortfoliosAreListedInPortfolioIdOrderWithTheirTotals() {
        // P-A: 100 x 11 = 1100; P-B: 10 x 5 = 50
        List<PortfolioSummaryResponse> portfolios = service.portfoliosOf("c1");

        assertThat(portfolios).extracting(PortfolioSummaryResponse::portfolioId).containsExactly("P-A", "P-B");
        assertThat(portfolios).extracting(PortfolioSummaryResponse::label).containsExactly("First", "Second");
        assertThat(portfolios.get(0).totalMarketValue()).isEqualByComparingTo("1100");
        assertThat(portfolios.get(1).totalMarketValue()).isEqualByComparingTo("50");
    }

    @Test
    void theHouseholdSummaryLeavesOutOtherClientsPortfolios() {
        // 1100 + 50 = 1150; day change (11 - 10) x 100 + 0 = 100; previous 1050; 100 / 1050 = 0.095238...
        HouseholdSummaryResponse summary = service.householdSummaryOf("c1");

        assertThat(summary.totalMarketValue()).isEqualByComparingTo("1150");
        assertThat(summary.portfolioCount()).isEqualTo(2);
        assertThat(summary.dayChangeAmount()).isEqualByComparingTo("100");
        assertThat(summary.dayChangePercent()).isEqualByComparingTo("0.095238");
    }

    @Test
    void aKnownClientWithoutPortfoliosGetsAnEmptyListAndAZeroSummary() {
        assertThat(service.portfoliosOf("c3")).isEmpty();
        assertThat(service.householdSummaryOf("c3").portfolioCount()).isZero();
        assertThat(service.householdSummaryOf("c3").dayChangePercent()).isNull();
    }

    @Test
    void anUnknownClientIsNotFoundOnBothCalls() {
        assertThatThrownBy(() -> service.portfoliosOf("UNKNOWN")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.householdSummaryOf("UNKNOWN")).isInstanceOf(NotFoundException.class);
    }

    private static TransactionRow buy(String holdingId, String quantity, String price) {
        return new TransactionRow("t", holdingId, TransactionType.BUY, new BigDecimal(quantity),
                new BigDecimal(price), LocalDate.parse("2025-01-02"));
    }

}
