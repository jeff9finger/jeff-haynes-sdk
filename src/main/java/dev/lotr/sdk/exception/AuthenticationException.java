package dev.lotr.sdk.exception;

/**
 * Thrown when the API rejects the request due to invalid or missing credentials (HTTP 401).
 */
public class AuthenticationException extends OneApiException {

    public AuthenticationException(String message) {
        super(message, 401);
    }
}
