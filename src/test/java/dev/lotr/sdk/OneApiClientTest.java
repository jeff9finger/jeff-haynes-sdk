package dev.lotr.sdk;

import dev.lotr.sdk.http.HttpResponse;
import dev.lotr.sdk.http.HttpStatus;
import dev.lotr.sdk.http.MockHttpClient;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OneApiClient builder and configuration.
 */
class OneApiClientTest {

    @Test
    void builder_withApiKey_createsClient() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(HttpStatus.OK, TestFixtures.MOVIE_LIST_JSON, Collections.emptyMap()));

        OneApiClient client = OneApiClient.builder()
                .apiKey("test-key")
                .httpClient(mock)
                .build();

        assertThat(client.movies()).isNotNull();
        assertThat(client.quotes()).isNotNull();
    }

    @Test
    void builder_withoutApiKey_throwsException() {
        assertThatThrownBy(() -> OneApiClient.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key is required");
    }

    @Test
    void builder_withBlankApiKey_throwsException() {
        assertThatThrownBy(() -> OneApiClient.builder().apiKey("  ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key is required");
    }

    @Test
    void builder_withCustomBaseUrl_usesIt() {
        MockHttpClient mock = new MockHttpClient();
        mock.enqueue(new HttpResponse(HttpStatus.OK, TestFixtures.MOVIE_LIST_JSON, Collections.emptyMap()));

        OneApiClient client = OneApiClient.builder()
                .apiKey("test-key")
                .baseUrl("https://custom-api.example.com/v2")
                .httpClient(mock)
                .build();

        client.movies().list();

        String url = mock.getRequests().getFirst().url();
        assertThat(url).startsWith("https://custom-api.example.com/v2");
    }
}
