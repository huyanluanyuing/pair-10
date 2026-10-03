package ca.en.solution.client;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.en.solution.common.Rounding;
import ca.en.solution.common.SeedData;
import ca.en.solution.common.SeedData.PortfolioRow;
import ca.en.solution.holding.HoldingService;

@Service
public class ClientService {

    private final SeedData seedData;
    private final HoldingService holdingService;

    public ClientService(SeedData seedData, HoldingService holdingService) {
        this.seedData = seedData;
        this.holdingService = holdingService;
    }

    public List<PortfolioSummaryResponse> portfoliosOf(String clientId) {
        return portfolioRowsOf(clientId).stream()
                .map(portfolio -> new PortfolioSummaryResponse(portfolio.portfolioId(), portfolio.label(),
                        Rounding.money(totalsOf(portfolio).marketValue())))
                .toList();
    }

    public HouseholdSummaryResponse householdSummaryOf(String clientId) {
        List<PortfolioTotals> portfolios = portfolioRowsOf(clientId).stream().map(this::totalsOf).toList();
        return HouseholdCalculator.summarize(clientId, portfolios);
    }

    private List<PortfolioRow> portfolioRowsOf(String clientId) {
        if (!seedData.hasClient(clientId)) {
            throw new ClientNotFoundException(clientId);
        }
        return seedData.portfoliosOf(clientId).stream()
                .sorted(Comparator.comparing(PortfolioRow::portfolioId))
                .toList();
    }

    private PortfolioTotals totalsOf(PortfolioRow portfolio) {
        return HouseholdCalculator.totalsOf(holdingService.valuationsOf(portfolio.portfolioId()));
    }

}
