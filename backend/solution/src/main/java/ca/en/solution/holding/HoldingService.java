package ca.en.solution.holding;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.en.solution.common.SeedData;
import ca.en.solution.common.SeedData.HoldingRow;
import ca.en.solution.ledger.LedgerReplay;
import ca.en.solution.ledger.Position;
import ca.en.solution.ledger.Transaction;
import ca.en.solution.portfolio.PortfolioNotFoundException;

@Service
public class HoldingService {

    private final SeedData seedData;

    public HoldingService(SeedData seedData) {
        this.seedData = seedData;
    }

    public List<HoldingResponse> holdingsOf(String portfolioId) {
        if (!seedData.hasPortfolio(portfolioId)) {
            throw new PortfolioNotFoundException(portfolioId);
        }
        List<HoldingPosition> positions = seedData.holdingsOf(portfolioId).stream()
                .sorted(Comparator.comparing(HoldingRow::ticker))
                .map(this::toPosition)
                .toList();
        return HoldingCalculator.value(positions).stream().map(HoldingResponse::from).toList();
    }

    private HoldingPosition toPosition(HoldingRow holding) {
        List<Transaction> transactions = seedData.transactionsOf(holding.holdingId()).stream()
                .map(row -> new Transaction(row.type(), row.quantity(), row.price(), row.date()))
                .toList();
        Position position = LedgerReplay.replay(transactions);
        return new HoldingPosition(holding.ticker(), holding.name(), holding.assetClass(),
                position.currentQuantity(), position.averageCostBasisPerShare(), holding.price(),
                holding.previousClosePrice());
    }

}
