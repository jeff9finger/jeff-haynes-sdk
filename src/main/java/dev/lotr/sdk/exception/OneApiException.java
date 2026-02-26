package dev.lotr.sdk.exception;

/**
 * Base exception for all errors raised by the Lord of the Rings SDK.
 *
 * <p>All SDK exceptions are unchecked to avoid polluting call sites with
 * boilerplate catch blocks. Callers who need fine-grained recovery can
 * catch the specific subtypes ({@link AuthenticationException},
 * {@link NotFoundException}, {@link RateLimitException}).
 */
public class OneApiException extends RuntimeException {

    private final int statusCode;

    public OneApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public OneApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
