package dev.lotr.sdk;

import dev.lotr.sdk.filter.Filter;
import dev.lotr.sdk.filter.RequestOptions;
import dev.lotr.sdk.model.Quote;
import dev.lotr.sdk.model.field.QuoteField;
import dev.lotr.sdk.response.PagedResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class QuoteResourceITBase {

    protected OneApiClient client;

    abstract OneApiClient createClient(String apiKey);

    @BeforeAll
    void setUp() {
        String apiKey = System.getenv("LOTR_API_KEY");
        assumeThat(apiKey)
                .as("LOTR_API_KEY must be set for integration tests")
                .isNotNull()
                .isNotBlank();

        client = createClient(apiKey);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "RUN_RATE_LIMIT_TEST", matches = "true")
    void listQuotes_returnsResults() {
        PagedResponse<Quote> response = client.quotes().list(
                RequestOptions.builder().limit(5).build());

        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().getFirst().getDialog()).isNotBlank();
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "RUN_RATE_LIMIT_TEST", matches = "true")
    void getQuoteById_returnsCorrectQuote() {
        PagedResponse<Quote> quotes = client.quotes().list(
                RequestOptions.builder().limit(1).build());
        String quoteId = quotes.getItems().getFirst().getId();

        Quote quote = client.quotes().getById(quoteId);

        assertThat(quote.getId()).isEqualTo(quoteId);
        assertThat(quote.getDialog()).isNotBlank();
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "RUN_RATE_LIMIT_TEST", matches = "true")
    void listQuotes_withRegexFilter() {
        RequestOptions options = RequestOptions.builder()
                .filter(Filter.where(QuoteField.DIALOG).matchesRegex("/ring/i"))
                .limit(10)
                .build();

        PagedResponse<Quote> response = client.quotes().list(options);

        assertThat(response.getItems())
                .allMatch(q -> q.getDialog().toLowerCase().contains("ring"));
    }
}
