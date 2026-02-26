package dev.lotr.sdk.config;

import lombok.Getter;

import java.time.Duration;
import java.util.function.Function;

/**
 * Immutable configuration for the SDK client.
 *
 * <p>Instances are created via {@link dev.lotr.sdk.OneApiClient.Builder} or
 * from a properties file using {@link dev.lotr.sdk.OneApiClient#fromProperties(String)}.
 */
@Getter
public final class OneApiConfig {

    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;
    private final Function<Integer, Duration> retryBackoff;

    public OneApiConfig(String apiKey, String baseUrl, Duration timeout,
                        int maxRetries, Function<Integer, Duration> retryBackoff) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.retryBackoff = retryBackoff;
    }
}
