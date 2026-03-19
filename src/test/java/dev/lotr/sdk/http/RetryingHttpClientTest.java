package dev.lotr.sdk.http;

import dev.lotr.sdk.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static dev.lotr.sdk.exception.RateLimitException.X_RATE_LIMIT_LIMIT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the RetryingHttpClient decorator.
 */
class RetryingHttpClientTest {

    @Test
    void successfulRequest_doesNotRetry() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(HttpStatus.OK, "ok", Collections.emptyMap()));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> Duration.ofMillis(1));

        HttpResponse response = retrying.get("https://test.com", "token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mock.getRequestCount()).isEqualTo(1);
    }

    @Test
    void rateLimited_retriesAndSucceeds() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));
        mock.enqueue(new HttpResponse(HttpStatus.OK, "ok", Collections.emptyMap()));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> Duration.ofMillis(1));

        HttpResponse response = retrying.get("https://test.com", "token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 1 initial + 2 retries = 3 total requests
        assertThat(mock.getRequestCount()).isEqualTo(3);
    }

    @Test
    void rateLimited_exhaustsRetries_throwsException() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> Duration.ofMillis(1));

        assertThatThrownBy(() -> retrying.get("https://test.com", "token"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("3 retries");
    }

    @Test
    void zeroMaxRetries_throwsImmediatelyOn429() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 0, attempt -> Duration.ofMillis(1));

        assertThatThrownBy(() -> retrying.get("https://test.com", "token"))
                .isInstanceOf(RateLimitException.class);
        assertThat(mock.getRequestCount()).isEqualTo(1);
    }

    @Test
    void backoffStrategy_isCalledWithCorrectAttemptNumbers() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));
        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", Collections.emptyMap()));
        mock.enqueue(new HttpResponse(HttpStatus.OK, "ok", Collections.emptyMap()));

        List<Integer> capturedAttempts = new ArrayList<>();
        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> {
            capturedAttempts.add(attempt);
            return Duration.ofMillis(1);
        });

        retrying.get("https://test.com", "token");
        assertThat(capturedAttempts).containsExactly(0, 1);
    }

    private static final Map<String, List<String>> RETRY_HEADERS =
            Map.of(
                    X_RATE_LIMIT_LIMIT_HEADER, List.of("100"),
                    RateLimitException.X_RATE_LIMIT_REMAINING_HEADER, List.of("0"),
                    RateLimitException.X_RATE_LIMIT_RESET_HEADER, List.of(String.valueOf(System.currentTimeMillis() + 60000)));

    @Test
    void rateLimited_skipRetries_exceedsLimit_throwsException() {
        MockHttpClient mock = new MockHttpClient();

        mock.enqueue(new HttpResponse(HttpStatus.TOO_MANY_REQUESTS, "rate limited", RETRY_HEADERS));

        RetryingHttpClient retrying = new RetryingHttpClient(
                      mock, 3, attempt -> Duration.ofMillis(1));

        assertThatThrownBy(() -> retrying.get("https://test.com", "token"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("No more requests remaining");

        assertThat(mock.getRequestCount()).isEqualTo(1);
    }
}
