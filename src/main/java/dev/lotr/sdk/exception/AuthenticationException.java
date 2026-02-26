package dev.lotr.sdk.exception;

import dev.lotr.sdk.http.HttpStatus;

/**
 * Thrown when the API rejects the request due to invalid or missing credentials (HTTP 401).
 */
public class AuthenticationException extends OneApiException {

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
