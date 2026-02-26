package dev.lotr.sdk.model;

import dev.lotr.sdk.response.PagedResponse;
import lombok.Getter;
import lombok.ToString;

/**
 * Composite model combining a movie with its associated quotes.
 *
 * <p>This is a convenience abstraction that fulfills the assignment's
 * guidance to "add abstractions and/or combine different calls." Rather
 * than requiring two separate API calls, {@code MovieWithQuotes} is
 * returned by {@link dev.lotr.sdk.resource.MovieResource#getWithQuotes(String)}.
 *
 * <pre>{@code
 * MovieWithQuotes result = client.movies().getWithQuotes(movieId);
 * Movie movie = result.getMovie();
 * PagedResponse<Quote> quotes = result.getQuotes();
 * }</pre>
 */
@Getter
@ToString
public final class MovieWithQuotes {

    private final Movie movie;
    private final PagedResponse<Quote> quotes;

    public MovieWithQuotes(Movie movie, PagedResponse<Quote> quotes) {
        this.movie = movie;
        this.quotes = quotes;
    }
}
