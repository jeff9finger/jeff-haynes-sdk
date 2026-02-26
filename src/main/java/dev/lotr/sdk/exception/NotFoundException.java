package dev.lotr.sdk.exception;

/**
 * Thrown when the requested resource does not exist (HTTP 404).
 */
public class NotFoundException extends OneApiException {

    public NotFoundException(String message) {
        super(message, 404);
    }
}
