package dev.lotr.sdk.http;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Internal representation of an HTTP response from the API.
 *
 * <p>This thin wrapper decouples the SDK's resource layer from the
 * specific HTTP client implementation, making both testable and
 * swappable independently.
 */
@Getter
public class HttpResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    public HttpResponse(int statusCode, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers == null ? Map.of() : headers;
    }

    public String extractHeader(String headerName) {
        return headers.getOrDefault(headerName, List.of("")).getFirst();
    }

}
