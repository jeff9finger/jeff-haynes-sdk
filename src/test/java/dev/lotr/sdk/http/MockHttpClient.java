package dev.lotr.sdk.http;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Test double for {@link HttpClient} that records requests and returns
 * enqueued responses.
 *
 * <p>Usage:
 * <pre>{@code
 * MockHttpClient mock = new MockHttpClient();
 * mock.enqueue(new HttpResponse(200, "{...}"));
 *
 * // Use mock with client under test
 * OneApiClient client = OneApiClient.builder()
 *         .apiKey("test-key")
 *         .httpClient(mock)
 *         .build();
 *
 * // Verify after test
 * assertThat(mock.getRequests()).hasSize(1);
 * assertThat(mock.getRequests().get(0).url()).contains("/movie");
 * }</pre>
 */
public class MockHttpClient implements HttpClient {

    private final Queue<HttpResponse> responses = new LinkedList<>();
    private final List<RecordedRequest> requests = new ArrayList<>();

    /**
     * Enqueues a response to return for the next request.
     */
    public void enqueue(HttpResponse response) {
        responses.add(response);
    }

    /**
     * Returns all recorded requests in order.
     */
    public List<RecordedRequest> getRequests() {
        return List.copyOf(requests);
    }

    /**
     * Returns the number of requests made.
     */
    public int getRequestCount() {
        return requests.size();
    }

    /**
     * Clears recorded requests and enqueued responses.
     */
    public void reset() {
        responses.clear();
        requests.clear();
    }

    @Override
    public HttpResponse get(String url, String bearerToken) {
        requests.add(new RecordedRequest(url, bearerToken));
        HttpResponse response = responses.poll();
        if (response == null) {
            throw new IllegalStateException(
                    "No more enqueued responses. Call enqueue() before making requests. "
                            + "Request #" + requests.size() + " to: " + url);
        }
        return response;
    }

    /**
     * Recorded details of an HTTP request made through this mock.
     */
    public record RecordedRequest(String url, String bearerToken) {}
}
