package ca.en.solution.currency;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FrankfurterCurrencyRateClient implements CurrencyRateClient {

    private final RestClient restClient;

    public FrankfurterCurrencyRateClient(
            @Value("${currency-rate.base-url:https://api.frankfurter.dev}") String baseUrl,
            @Value("${currency-rate.connect-timeout:1s}") Duration connectTimeout,
            @Value("${currency-rate.read-timeout:2s}") Duration readTimeout) {
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
    public BigDecimal cadToUsdRate() {
        RateResponse response;
        try {
            response = restClient.get().uri("/v2/rate/cad/usd")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, upstream) -> {
                        throw new CurrencyRateUnavailableException();
                    })
                    .body(RateResponse.class);
        } catch (RestClientException exception) {
            throw new CurrencyRateUnavailableException(exception);
        }
        if (response == null
                || !"CAD".equals(response.base())
                || !"USD".equals(response.quote())
                || response.rate() == null
                || response.rate().signum() <= 0) {
            throw new CurrencyRateUnavailableException();
        }
        return response.rate();
    }

    private record RateResponse(String base, String quote, BigDecimal rate) {
    }
}
