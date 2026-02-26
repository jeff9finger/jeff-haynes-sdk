package dev.lotr.sdk.exception;

import dev.lotr.sdk.http.HttpStatus;
import lombok.Getter;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * Thrown when the API rate limit has been exceeded (HTTP 429).
 *
 * <p>The One API allows 100 requests per 10 minutes. When the
 * {@link dev.lotr.sdk.http.RetryingHttpClient} is in use, this exception
 * is only thrown after all retry attempts have been exhausted.
 * The exception message may include details from the API response, such as
 * the rate limit, remaining requests, and reset time. These are typically provided in the response headers:
 * <ul>
 *   <li><code>x-ratelimit-limit</code>: the total number of requests allowed in the current window (e.g. 100)</li>
 *   <li><code>x-ratelimit-remaining</code>: the number of requests remaining in the current window (e.g. 95)</li>
 *   <li><code>x-ratelimit-reset</code>: the UNIX timestamp when the current rate limit window resets (e.g. 1772123552)
 */
@Getter
public class RateLimitException extends OneApiException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int limit;
    private final int remaining;
    private final long resetTime;

    /**
     * <code>x-ratelimit-limit</code> the total number of requests allowed in the current window (e.g. 100)
     */
    public static final String X_RATE_LIMIT_LIMIT_HEADER = "x-ratelimit-limit";
    /**
     * <code>x-ratelimit-remaining</code>: the number of requests remaining in the current window (e.g. 95)
     */
    public static final String X_RATE_LIMIT_REMAINING_HEADER = "x-ratelimit-remaining";
    /**
     * <code>x-ratelimit-reset</code>: the UNIX timestamp when the current rate limit window resets (e.g. 1772123552)
     */
    public static final String X_RATE_LIMIT_RESET_HEADER = "x-ratelimit-reset";

    public RateLimitException(String message, Map<String, List<String>> headers) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
        this.limit = Integer.parseInt(extractHeader(headers,X_RATE_LIMIT_LIMIT_HEADER));
        this.remaining = Integer.parseInt(extractHeader(headers,X_RATE_LIMIT_REMAINING_HEADER));
        this.resetTime = Long.parseLong(extractHeader(headers,X_RATE_LIMIT_RESET_HEADER));
    }

    private String extractHeader(Map<String, List<String>> headers, String headerName) {
        return headers.getOrDefault(headerName, List.of("0")).getFirst();
    }
}
