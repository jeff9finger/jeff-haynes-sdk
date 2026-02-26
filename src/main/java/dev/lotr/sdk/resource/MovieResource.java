package dev.lotr.sdk.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lotr.sdk.config.OneApiConfig;
import dev.lotr.sdk.filter.RequestOptions;
import dev.lotr.sdk.http.HttpClient;
import dev.lotr.sdk.http.HttpResponse;
import dev.lotr.sdk.http.HttpStatus;
import dev.lotr.sdk.model.Movie;
import dev.lotr.sdk.model.MovieWithQuotes;
import dev.lotr.sdk.model.Quote;
import dev.lotr.sdk.response.PagedResponse;

/**
 * Resource accessor for the {@code /movie} endpoint.
 *
 * <p>Note: The API uses singular path ({@code /movie}), but the SDK
 * exposes a plural accessor ({@code client.movies()}) to follow
 * established Java SDK conventions (Stripe, AWS). See design.md for
 * rationale.
 *
 * <pre>{@code
 * // List all movies
 * PagedResponse<Movie> movies = client.movies().list();
 *
 * // Get a specific movie
 * Movie movie = client.movies().getById(id);
 *
 * // Get movie with its quotes (combined call)
 * MovieWithQuotes result = client.movies().getWithQuotes(id);
 * }</pre>
 */
public final class MovieResource extends BaseResource<Movie> {

    private final QuoteSubResource quoteSubResource;

    public MovieResource(OneApiConfig config, HttpClient httpClient,
                         ObjectMapper objectMapper) {
        super("/movie", Movie.class, config, httpClient, objectMapper);
        this.quoteSubResource = new QuoteSubResource(config, httpClient, objectMapper);
    }

    /**
     * Retrieves quotes for a specific movie.
     *
     * @param movieId the movie ID
     * @return paginated quotes for the movie
     */
    public PagedResponse<Quote> getQuotes(String movieId) {
        return getQuotes(movieId, null);
    }

    /**
     * Retrieves quotes for a specific movie with filter/pagination options.
     *
     * @param movieId the movie ID
     * @param options filter, sort, and pagination options
     * @return paginated quotes for the movie
     */
    public PagedResponse<Quote> getQuotes(String movieId, RequestOptions options) {
        return quoteSubResource.listForMovie(movieId, options);
    }

    /**
     * Convenience method combining a movie lookup with its quotes.
     *
     * <p>Makes two API calls internally: one for the movie, one for its quotes.
     * This addresses the assignment's guidance to "add abstractions and/or
     * combine different calls."
     *
     * @param movieId the movie ID
     * @return the movie and its first page of quotes
     */
    public MovieWithQuotes getWithQuotes(String movieId) {
        Movie movie = getById(movieId);
        PagedResponse<Quote> quotes = getQuotes(movieId);
        return new MovieWithQuotes(movie, quotes);
    }

    /**
     * Internal sub-resource for movie quotes (/movie/{id}/quote).
     */
    private static final class QuoteSubResource {

        private final OneApiConfig config;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;

        QuoteSubResource(OneApiConfig config, HttpClient httpClient,
                         ObjectMapper objectMapper) {
            this.config = config;
            this.httpClient = httpClient;
            this.objectMapper = objectMapper;
        }

        PagedResponse<Quote> listForMovie(String movieId, RequestOptions options) {
            StringBuilder url = new StringBuilder(config.getBaseUrl())
                    .append("/movie/").append(movieId).append("/quote");
            if (options != null) {
                String query = options.toQueryString();
                if (!query.isEmpty()) {
                    url.append("?").append(query);
                }
            }

            HttpResponse response = httpClient.get(url.toString(), config.getApiKey());
            if (response.getStatusCode() != HttpStatus.OK) {
                // Delegate error handling — reuse exception mapping
                throw new dev.lotr.sdk.exception.OneApiException(
                        "API error (HTTP " + response.getStatusCode() + "): "
                                + response.getBody(),
                        response.getStatusCode());
            }

            try {
                var type = objectMapper.getTypeFactory()
                        .constructParametricType(PagedResponse.class, Quote.class);
                return objectMapper.readValue(response.getBody(), type);
            } catch (Exception e) {
                throw new dev.lotr.sdk.exception.OneApiException(
                        "Failed to parse quotes response: " + e.getMessage(), e);
            }
        }
    }
}
