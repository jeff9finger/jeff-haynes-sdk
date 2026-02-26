package dev.lotr.sdk.http;

import dev.lotr.sdk.exception.RateLimitException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.lotr.sdk.exception.RateLimitException.X_RATE_LIMIT_LIMIT_HEADER;
import static dev.lotr.sdk.exception.RateLimitException.X_RATE_LIMIT_REMAINING_HEADER;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

/**
 * Decorator that adds automatic retry with backoff for rate-limited responses.
 *
 * <p>Wraps any {@link HttpClient} and intercepts HTTP 429 responses, retrying
 * the request according to a configurable backoff strategy. This keeps retry
 * concerns separated from transport concerns, following the decorator pattern.
 *
 * <h3>Default behavior:</h3>
 * <ul>
 *   <li>Up to 3 retries</li>
 *   <li>Exponential backoff: 1s, 2s, 4s (2^attempt seconds)</li>
 * </ul>
 *
 * <h3>Custom backoff:</h3>
 * <pre>{@code
 * // Linear 500ms backoff
 * new RetryingHttpClient(inner, 3, attempt -> Duration.ofMillis(500));
 *
 * // Disable retries
 * new RetryingHttpClient(inner, 0, attempt -> Duration.ZERO);
 * }</pre>
 *
 * @see DefaultHttpClient
 */
public final class RetryingHttpClient implements HttpClient {

    private static final Logger logger = Logger.getLogger(RetryingHttpClient.class.getName());

    private final HttpClient delegate;
    private final int maxRetries;
    private final Function<Integer, Duration> backoffStrategy;

    /**
     * Creates a retrying client with the given backoff strategy.
     *
     * @param delegate        the underlying HTTP client to delegate to
     * @param maxRetries      maximum number of retry attempts (0 to disable)
     * @param backoffStrategy function from attempt number (0-based) to wait duration
     */
    public RetryingHttpClient(HttpClient delegate, int maxRetries,
                              Function<Integer, Duration> backoffStrategy) {
        this.delegate = delegate;
        this.maxRetries = maxRetries;
        this.backoffStrategy = backoffStrategy;
    }

    @Override
    public HttpResponse get(String url, String bearerToken) {
        HttpResponse response = delegate.get(url, bearerToken);

        int attempt = 0;
        while (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < maxRetries) {
            throwIfRateLimitExhausted( response);
            Duration wait = backoffStrategy.apply(attempt);
            if (logger.isLoggable(FINE)) {
                logger.log(Level.FINE, "Rate limited (429), retrying (attempt {0}/{1}) after {2}",
                        new Object[]{attempt + 1, maxRetries, wait});
            }
            try {
                Thread.sleep(wait.toMillis()); //sleep OK here. If async desired, add new implementation with async support.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Rate limit backoff interrupted");
                throw new RateLimitException(
                        "Interrupted while waiting for rate limit backoff", response.getHeaders());
            }
            response = delegate.get(url, bearerToken);
            attempt++;
        }

        if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            // throws an exception with details if headers indicate we're out of requests, otherwise log and throw a generic rate limit exception
            throwIfRateLimitExhausted(response);
            logger.warning("Rate limit exceeded after " + maxRetries + " retries");
            throw new RateLimitException("Rate limit exceeded after " + maxRetries + " retries", response.getHeaders());
        }

        return response;
    }

    private void throwIfRateLimitExhausted(HttpResponse response) {
        final Map<String, List<String>> headers = response.getHeaders();
        int limit = extractRateLimitHeader(headers,X_RATE_LIMIT_LIMIT_HEADER);
        int remaining = extractRateLimitHeader(headers,X_RATE_LIMIT_REMAINING_HEADER);
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "429 received: x-ratelimit-limit={0}, x-ratelimit-remaining={1}",
                    new Object[]{limit, remaining});
        }
        if (limit > 0 && remaining >= limit) {
            logger.log(WARNING, "Rate limit window exhausted (remaining={0}, limit={1}), not retrying",
                    new Object[]{remaining, limit});
            throw new RateLimitException(
                    "No more requests remaining in the current rate window", response.getHeaders());
        }
    }

    private int extractRateLimitHeader(Map<String, List<String>> headers, String headerName) {
        String val = headers.getOrDefault(headerName, List.of("0")).getFirst();
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0; // default to 0 if header is missing or malformed
        }
    }
}
