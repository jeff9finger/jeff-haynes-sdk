package dev.lotr.sdk.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lotr.sdk.config.OneApiConfig;
import dev.lotr.sdk.http.HttpClient;
import dev.lotr.sdk.model.Quote;

/**
 * Resource accessor for the {@code /quote} endpoint.
 *
 * <p>Note: The API uses singular path ({@code /quote}), but the SDK
 * exposes a plural accessor ({@code client.quotes()}) to follow
 * established Java SDK conventions. See design.md for rationale.
 *
 * <pre>{@code
 * // List all quotes
 * PagedResponse<Quote> quotes = client.quotes().list();
 *
 * // Get a specific quote
 * Quote quote = client.quotes().getById(id);
 *
 * // Stream all quotes with a filter
 * client.quotes().listAll(
 *     RequestOptions.builder()
 *         .filter(Filter.where("dialog").matchesRegex("/ring/i"))
 *         .build()
 * ).forEach(q -> System.out.println(q.getDialog()));
 * }</pre>
 */
public final class QuoteResource extends BaseResource<Quote> {

    public QuoteResource(OneApiConfig config, HttpClient httpClient,
                         ObjectMapper objectMapper) {
        super("/quote", Quote.class, config, httpClient, objectMapper);
    }
}
