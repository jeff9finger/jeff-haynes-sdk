package dev.lotr.sdk.http;

/**
 * Internal representation of an HTTP response from the API.
 *
 * <p>This thin wrapper decouples the SDK's resource layer from the
 * specific HTTP client implementation, making both testable and
 * swappable independently.
 */
public record HttpResponse(int statusCode, String body) {

}
