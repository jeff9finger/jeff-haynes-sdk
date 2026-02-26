package dev.lotr.sdk.exception;

/**
 * Thrown when the API rejects the request due to invalid or missing credentials (HTTP 403).
 */
public class AuthorizationException extends OneApiException {

    public AuthorizationException(String message) {
        super(message, 403);
    }
}
