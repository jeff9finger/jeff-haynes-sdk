package dev.lotr.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lotr.sdk.config.OneApiConfig;
import dev.lotr.sdk.http.DefaultHttpClient;
import dev.lotr.sdk.http.HttpClient;
import dev.lotr.sdk.http.RetryingHttpClient;
import dev.lotr.sdk.resource.MovieResource;
import dev.lotr.sdk.resource.QuoteResource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.function.Function;

/**
 * Entry point for the Lord of the Rings SDK.
 *
 * <p>Use the {@link #builder()} for programmatic configuration, or
 * {@link #fromProperties(String)} for file-based configuration.
 *
 * <h3>Builder (recommended):</h3>
 * <pre>{@code
 * OneApiClient client = OneApiClient.builder()
 *         .apiKey("your-api-key")
 *         .build();
 *
 * // Access resources
 * client.movies().list();
 * client.quotes().getById(id);
 * }</pre>
 *
 * <h3>Properties file:</h3>
 * <pre>{@code
 * OneApiClient client = OneApiClient.fromProperties("lotr-sdk.properties");
 * }</pre>
 *
 * @see MovieResource
 * @see QuoteResource
 */
public final class OneApiClient {

    private static final String DEFAULT_BASE_URL = "https://the-one-api.dev/v2";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Function<Integer, Duration> DEFAULT_BACKOFF =
            attempt -> Duration.ofSeconds((long) Math.pow(2, attempt));

    private final MovieResource movieResource;
    private final QuoteResource quoteResource;

    private OneApiClient(OneApiConfig config, HttpClient httpClient) {
        ObjectMapper objectMapper = new ObjectMapper();
        this.movieResource = new MovieResource(config, httpClient, objectMapper);
        this.quoteResource = new QuoteResource(config, httpClient, objectMapper);
    }

    /**
     * Returns the movie resource accessor.
     *
     * @return the {@link MovieResource} for interacting with /movie endpoints
     */
    public MovieResource movies() {
        return movieResource;
    }

    /**
     * Returns the quote resource accessor.
     *
     * @return the {@link QuoteResource} for interacting with /quote endpoints
     */
    public QuoteResource quotes() {
        return quoteResource;
    }

    /**
     * Creates a new builder for configuring the client.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a client from a properties file on the classpath.
     *
     * <p>Supported properties:
     * <ul>
     *   <li>{@code lotr.api.key} — (required) API key</li>
     *   <li>{@code lotr.api.base-url} — base URL (default: https://the-one-api.dev/v2)</li>
     *   <li>{@code lotr.api.timeout-seconds} — request timeout (default: 10)</li>
     *   <li>{@code lotr.api.max-retries} — max retry attempts (default: 3)</li>
     * </ul>
     *
     * @param resourcePath classpath resource path to the properties file
     * @return a configured client
     * @throws IllegalArgumentException if the file cannot be loaded or key is missing
     */
    public static OneApiClient fromProperties(String resourcePath) {
        Properties props = new Properties();
        try (InputStream is = OneApiClient.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "Properties file not found on classpath: " + resourcePath);
            }
            props.load(is);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to load properties file: " + resourcePath, e);
        }

        String apiKey = props.getProperty("lotr.api.key");
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_API_KEY_HERE")) {
            throw new IllegalArgumentException(
                    "Property 'lotr.api.key' is required in " + resourcePath);
        }

        Builder builder = builder().apiKey(apiKey);

        String baseUrl = props.getProperty("lotr.api.base-url");
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        String timeoutStr = props.getProperty("lotr.api.timeout-seconds");
        if (timeoutStr != null && !timeoutStr.isBlank()) {
            builder.timeout(Duration.ofSeconds(Long.parseLong(timeoutStr.trim())));
        }

        String retriesStr = props.getProperty("lotr.api.max-retries");
        if (retriesStr != null && !retriesStr.isBlank()) {
            builder.maxRetries(Integer.parseInt(retriesStr.trim()));
        }

        return builder.build();
    }

    /**
     * Fluent builder for {@link OneApiClient}.
     */
    public static final class Builder {

        private String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = DEFAULT_TIMEOUT;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private Function<Integer, Duration> retryBackoff = DEFAULT_BACKOFF;
        private HttpClient httpClient;

        private Builder() {}

        /** Sets the API authentication key (required). */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /** Sets the base URL (default: https://the-one-api.dev/v2). */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** Sets the HTTP request timeout (default: 10 seconds). */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Sets the maximum number of retries on HTTP 429 (default: 3, 0 to disable). */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the retry backoff strategy (default: exponential 2^attempt seconds).
         *
         * @param backoff function from attempt number (0-based) to wait duration
         */
        public Builder retryBackoff(Function<Integer, Duration> backoff) {
            this.retryBackoff = backoff;
            return this;
        }

        /**
         * Overrides the HTTP client implementation (for testing or custom transport).
         *
         * <p>When set, the client is used as-is — no retry decorator is applied.
         * Use this when you need full control over the HTTP layer.
         *
         * @param httpClient a custom {@link HttpClient} implementation
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Builds the client.
         *
         * @return a configured {@link OneApiClient}
         * @throws IllegalArgumentException if apiKey is not set
         */
        public OneApiClient build() {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("API key is required");
            }

            OneApiConfig config = new OneApiConfig(
                    apiKey, baseUrl, timeout, maxRetries, retryBackoff);

            HttpClient client;
            if (this.httpClient != null) {
                // Custom client provided — use as-is (caller controls retry behavior)
                client = this.httpClient;
            } else {
                HttpClient defaultClient = new DefaultHttpClient(timeout);
                if (maxRetries > 0) {
                    client = new RetryingHttpClient(defaultClient, maxRetries, retryBackoff);
                } else {
                    client = defaultClient;
                }
            }

            return new OneApiClient(config, client);
        }
    }
}
