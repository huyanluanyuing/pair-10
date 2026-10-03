package ca.en.solution.client;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/clients/{clientId}/portfolios")
    public List<PortfolioSummaryResponse> portfolios(@PathVariable String clientId) {
        return clientService.portfoliosOf(clientId);
    }

    @GetMapping("/clients/{clientId}/household-summary")
    public HouseholdSummaryResponse householdSummary(@PathVariable String clientId) {
        return clientService.householdSummaryOf(clientId);
    }

}
