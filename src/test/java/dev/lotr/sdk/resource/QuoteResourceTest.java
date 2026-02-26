package dev.lotr.sdk.resource;

import dev.lotr.sdk.OneApiClient;
import dev.lotr.sdk.TestFixtures;
import dev.lotr.sdk.filter.Filter;
import dev.lotr.sdk.filter.RequestOptions;
import dev.lotr.sdk.http.HttpResponse;
import dev.lotr.sdk.http.MockHttpClient;
import dev.lotr.sdk.model.Quote;
import dev.lotr.sdk.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QuoteResource.
 */
class QuoteResourceTest {

    private MockHttpClient mockHttp;
    private OneApiClient client;

    @BeforeEach
    void setUp() {
        mockHttp = new MockHttpClient();
        client = OneApiClient.builder()
                .apiKey("test-key")
                .httpClient(mockHttp)
                .build();
    }

    @Test
    void list_returnsQuotes() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.QUOTE_LIST_JSON));

        PagedResponse<Quote> response = client.quotes().list();

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getDialog())
                .isEqualTo("One Ring to rule them all.");
    }

    @Test
    void getById_returnsSingleQuote() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.SINGLE_QUOTE_JSON));

        Quote quote = client.quotes().getById("5cd96e05de30eff6ebcce7e9");

        assertThat(quote.getDialog()).isEqualTo("One Ring to rule them all.");
        assertThat(quote.getMovie()).isEqualTo("5cd95395de30eff6ebccde5d");
        assertThat(quote.getCharacter()).isEqualTo("5cd99d4bde30eff6ebccfbe6");
    }

    @Test
    void list_withFilter_includesQueryParams() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.QUOTE_LIST_JSON));

        RequestOptions options = RequestOptions.builder()
                .filter(Filter.where("dialog").matchesRegex("/ring/i"))
                .build();

        client.quotes().list(options);

        String url = mockHttp.getRequests().getFirst().url();
        assertThat(url).contains("dialog=/ring/i");
    }
}
