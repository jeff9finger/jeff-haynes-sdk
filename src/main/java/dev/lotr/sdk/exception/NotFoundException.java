package dev.lotr.sdk.exception;

import dev.lotr.sdk.http.HttpStatus;

/**
 * Thrown when the requested resource does not exist (HTTP 404).
 */
public class NotFoundException extends OneApiException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
