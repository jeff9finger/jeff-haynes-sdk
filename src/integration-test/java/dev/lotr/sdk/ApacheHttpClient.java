package dev.lotr.sdk;

import dev.lotr.sdk.exception.OneApiException;
import dev.lotr.sdk.http.HttpClient;
import dev.lotr.sdk.http.HttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* *
 * An HttpClient implementation using Apache HttpClient.
 *
 * <p>Used in integration tests to verify the SDK works with a different HTTP client.
 */
class ApacheHttpClient implements HttpClient {

    private final CloseableHttpClient delegate = HttpClients.createDefault();

    @Override
    public HttpResponse get(String url, String bearerToken) {
        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        try {
            return delegate.execute(request, response -> {
                int statusCode = response.getCode();
                String body = EntityUtils.toString(response.getEntity());
                Map<String, List<String>> headers = Arrays.stream(response.getHeaders())
                        .collect(Collectors.groupingBy(
                                Header::getName,
                                Collectors.mapping(Header::getValue, Collectors.toList())
                        ));
                return new HttpResponse(statusCode, body, headers);
            });
        } catch (IOException e) {
            throw new OneApiException("Apache HTTP request failed: " + e.getMessage(), e);
        }
    }
}
