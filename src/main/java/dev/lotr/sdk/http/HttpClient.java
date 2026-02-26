package dev.lotr.sdk.http;

/**
 * Abstraction over HTTP transport, enabling pluggable implementations.
 *
 * <p>The SDK ships with {@link DefaultHttpClient} (backed by Java's
 * {@link java.net.http.HttpClient}) and {@link RetryingHttpClient}
 * (a decorator that adds rate-limit retry logic). Consumers may also
 * provide their own implementation for custom transport needs or testing.
 *
 * @see DefaultHttpClient
 * @see RetryingHttpClient
 */
/* TODO for future versions:
 * - consider adding support for other HTTP methods (POST, PUT, etc.) and request bodies if needed by the API
 * - consider adding support for custom headers or query parameters if the API requires them
 * - consider adding asynchronous methods (e.g. CompletableFuture<HttpResponse>) for non-blocking use cases
 * - consider abstracting authentication (e.g. via an AuthProvider interface) if the API supports multiple auth schemes in the future
 * - consider abstracting HttpResponse to be more flexible (e.g. include headers, support streaming bodies) if needed by the API
 * - For now, the SDK only needs GET requests, so we keep the interface minimal.
 */
public interface HttpClient {

    /**
     * Sends a GET request to the specified URL with the given bearer token.
     *
     * @param url        the fully-qualified request URL including query parameters
     * @param bearerToken the API authentication token
     * @return the HTTP response
     */
    HttpResponse get(String url, String bearerToken);
}
