package ca.en.solution.crm;

import ca.en.solution.common.Rounding;
import ca.en.solution.portfolio.PortfolioMetadata;
import ca.en.solution.portfolio.PortfolioNotFoundException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.List;

@Component
public class HttpCrmClient implements CrmClient {
    private final RestClient restClient;

    public HttpCrmClient(
            @Value("${crm.base-url:http://localhost:4002}") String baseUrl,
            @Value("${crm.connect-timeout:1s}") Duration connectTimeout,
            @Value("${crm.read-timeout:2s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public PortfolioMetadata fetchPortfolio(String portfolioId) {
        CrmResponse response;
        try {
            response = restClient.get().uri("/crm/portfolios/{id}", portfolioId)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, upstream) -> {
                        if (upstream.getStatusCode().value() == 404) {
                            throw new PortfolioNotFoundException(portfolioId);
                        }
                        throw new CrmUnavailableException();
                    })
                    .body(CrmResponse.class);
        } catch (RestClientException exception) {
            throw new CrmUnavailableException(exception);
        }

        if (response == null || response.clientRecord() == null) {
            throw new CrmUnavailableException();
        }
        ClientRecord client = response.clientRecord();
        List<Account> accounts = client.accounts();
        if (accounts == null && client.relationships() != null) {
            accounts = client.relationships().accounts();
        }
        if (accounts == null || accounts.stream().anyMatch(account -> account == null)) {
            throw new CrmUnavailableException();
        }
        Account account = accounts.stream()
                .filter(candidate -> portfolioId.equals(candidate.reference()))
                .findFirst()
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        return new PortfolioMetadata(
                account.reference(), client.clientId(), account.nickname(),
                account.currentValue() == null ? null : account.currentValue().currency(),
                Rounding.money(account.currentValue() == null ? null : account.currentValue().amount()),
                Rounding.money(account.dayChange() == null ? null : account.dayChange().amount()),
                Rounding.ratio(account.dayChange() == null ? null : account.dayChange().percent()),
                Rounding.ratio(account.inceptionPercent()),
                response.meta() == null ? null : response.meta().retrievedAt());
    }

    // Legacy names and nesting stay at the external boundary.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CrmResponse(@JsonProperty("client_record") ClientRecord clientRecord, Meta meta) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClientRecord(@JsonProperty("client_id") String clientId,
                                List<Account> accounts, Relationships relationships) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Relationships(List<Account> accounts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Account(@JsonProperty("acct_ref") String reference,
                           @JsonProperty("acct_nickname") String nickname,
                           @JsonProperty("curr_val") CurrentValue currentValue,
                           @JsonProperty("chg_1d") DayChange dayChange,
                           @JsonProperty("since_inception_pct") BigDecimal inceptionPercent) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CurrentValue(@JsonProperty("amt") BigDecimal amount,
                                @JsonProperty("ccy") String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DayChange(@JsonProperty("amt") BigDecimal amount,
                             @JsonProperty("pct") BigDecimal percent) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Meta(@JsonProperty("retrieved_at") String retrievedAt) {
    }
}
