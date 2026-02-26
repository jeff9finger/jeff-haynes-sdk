package dev.lotr.sdk.http;

import dev.lotr.sdk.exception.OneApiException;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

/**
 * Default HTTP client backed by Java 21's {@link java.net.http.HttpClient}.
 *
 * <p>This implementation is thread-safe and reuses a single underlying
 * {@link java.net.http.HttpClient} instance across all requests.
 */
public final class DefaultHttpClient implements HttpClient {

    private final java.net.http.HttpClient delegate;
    private final Duration timeout;

    /**
     * Creates a client with the specified request timeout.
     *
     * @param timeout maximum time to wait for each HTTP request
     */
    public DefaultHttpClient(Duration timeout) {
        this.timeout = timeout;
        this.delegate = java.net.http.HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public HttpResponse get(String url, String bearerToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Accept", "application/json")
                    .timeout(timeout)
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = delegate.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.ofString()
            );

            return new HttpResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OneApiException("Request interrupted", e);
        } catch (Exception e) {
            throw new OneApiException("HTTP request failed: " + e.getMessage(), e);
        }
    }
}
