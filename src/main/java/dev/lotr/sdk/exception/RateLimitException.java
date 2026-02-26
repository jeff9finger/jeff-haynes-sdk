package dev.lotr.sdk.exception;

/**
 * Thrown when the API rate limit has been exceeded (HTTP 429).
 *
 * <p>The One API allows 100 requests per 10 minutes. When the
 * {@link dev.lotr.sdk.http.RetryingHttpClient} is in use, this exception
 * is only thrown after all retry attempts have been exhausted.
 */
public class RateLimitException extends OneApiException {

    public RateLimitException(String message) {
        super(message, 429);
    }
}
