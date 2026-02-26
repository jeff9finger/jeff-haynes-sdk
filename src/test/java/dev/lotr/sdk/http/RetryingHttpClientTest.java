package dev.lotr.sdk.http;

import dev.lotr.sdk.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the RetryingHttpClient decorator.
 */
class RetryingHttpClientTest {

    @Test
    void successfulRequest_doesNotRetry() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(200, "ok"));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> Duration.ofMillis(1));

        HttpResponse response = retrying.get("http://test.com", "token");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(mock.getRequestCount()).isEqualTo(1);
    }

    @Test
    void rateLimited_retriesAndSucceeds() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(429, "rate limited"));
        mock.enqueue(new HttpResponse(429, "rate limited"));
        mock.enqueue(new HttpResponse(200, "ok"));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> Duration.ofMillis(1));

        HttpResponse response = retrying.get("http://test.com", "token");
        assertThat(response.statusCode()).isEqualTo(200);
        // 1 initial + 2 retries = 3 total requests
        assertThat(mock.getRequestCount()).isEqualTo(3);
    }

    @Test
    void rateLimited_exhaustsRetries_throwsException() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(429, "rate limited"));
        mock.enqueue(new HttpResponse(429, "rate limited"));
        mock.enqueue(new HttpResponse(429, "rate limited"));
        mock.enqueue(new HttpResponse(429, "rate limited"));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> Duration.ofMillis(1));

        assertThatThrownBy(() -> retrying.get("http://test.com", "token"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("3 retries");
    }

    @Test
    void zeroMaxRetries_throwsImmediatelyOn429() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(429, "rate limited"));

        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 0, attempt -> Duration.ofMillis(1));

        assertThatThrownBy(() -> retrying.get("http://test.com", "token"))
                .isInstanceOf(RateLimitException.class);
        assertThat(mock.getRequestCount()).isEqualTo(1);
    }

    @Test
    void backoffStrategy_isCalledWithCorrectAttemptNumbers() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(429, "rate limited"));
        mock.enqueue(new HttpResponse(429, "rate limited"));
        mock.enqueue(new HttpResponse(200, "ok"));

        List<Integer> capturedAttempts = new ArrayList<>();
        RetryingHttpClient retrying = new RetryingHttpClient(
                mock, 3, attempt -> {
            capturedAttempts.add(attempt);
            return Duration.ofMillis(1);
        });

        retrying.get("http://test.com", "token");
        assertThat(capturedAttempts).containsExactly(0, 1);
    }
}
