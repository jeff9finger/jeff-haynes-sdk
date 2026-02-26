package dev.lotr.sdk;

import dev.lotr.sdk.exception.NotFoundException;
import dev.lotr.sdk.filter.Filter;
import dev.lotr.sdk.filter.RequestOptions;
import dev.lotr.sdk.filter.SortDirection;
import dev.lotr.sdk.model.Movie;
import dev.lotr.sdk.model.MovieWithQuotes;
import dev.lotr.sdk.model.Quote;
import dev.lotr.sdk.model.field.MovieField;
import dev.lotr.sdk.response.PagedResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Integration tests against the live One API.
 *
 * <p>Requires a valid API key set via the {@code LOTR_API_KEY} environment variable.
 * Run with: {@code LOTR_API_KEY=your-key mvn verify -Pintegration}
 */
class MovieResourceIT {

    private static OneApiClient client;

    @BeforeAll
    static void setUp() {
        String apiKey = System.getenv("LOTR_API_KEY");
        assumeThat(apiKey)
                .as("LOTR_API_KEY must be set for integration tests")
                .isNotNull()
                .isNotBlank();

        client = OneApiClient.builder()
                .apiKey(apiKey)
                .maxRetries(3)
                .build();
    }

    @Test
    void listMovies_returnsResults() {
        PagedResponse<Movie> response = client.movies().list();

        assertThat(response.getTotal()).isGreaterThan(0);
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().getFirst().getName()).isNotBlank();
    }

    @Test
    void listMovies_withPagination() {
        RequestOptions options = RequestOptions.builder()
                .limit(2)
                .page(1)
                .build();

        PagedResponse<Movie> response = client.movies().list(options);

        assertThat(response.getItems()).hasSizeLessThanOrEqualTo(2);
        assertThat(response.getLimit()).isEqualTo(2);
    }

    @Test
    void listMovies_withSort() {
        RequestOptions options = RequestOptions.builder()
                .sort(MovieField.NAME, SortDirection.ASC)
                .build();

        PagedResponse<Movie> response = client.movies().list(options);
        List<String> names = response.getItems().stream()
                .map(Movie::getName)
                .collect(Collectors.toList());

        // Verify ascending order
        for (int i = 1; i < names.size(); i++) {
            assertThat(names.get(i).compareToIgnoreCase(names.get(i - 1)))
                    .isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void listMovies_withBudgetFilter() {
        RequestOptions options = RequestOptions.builder()
                .filter(Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(100))
                .build();

        PagedResponse<Movie> response = client.movies().list(options);

        assertThat(response.getItems())
                .allMatch(m -> m.getBudgetInMillions() > 100);
    }

    @Test
    void getMovieById_returnsCorrectMovie() {
        // First, get a movie ID from the list
        PagedResponse<Movie> movies = client.movies().list(
                RequestOptions.builder().limit(1).build());
        String movieId = movies.getItems().getFirst().getId();

        Movie movie = client.movies().getById(movieId);

        assertThat(movie.getId()).isEqualTo(movieId);
        assertThat(movie.getName()).isNotBlank();
    }

    @Test
    void getMovieById_nonExistent_throwsNotFoundException() {
        assertThatThrownBy(() -> client.movies().getById("000000000000000000000000"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMovieQuotes_returnsQuotes() {
        // Use a known trilogy movie that has quotes
        PagedResponse<Movie> movies = client.movies().list(
                RequestOptions.builder()
                        .filter(Filter.where(MovieField.NAME)
                                .equals("The Return of the King"))
                        .build());
        assumeThat(movies.getItems()).isNotEmpty();

        String movieId = movies.getItems().getFirst().getId();
        PagedResponse<Quote> quotes = client.movies().getQuotes(movieId);

        assertThat(quotes.getTotal()).isGreaterThan(0);
        assertThat(quotes.getItems().getFirst().getDialog()).isNotBlank();
    }

    @Test
    void getWithQuotes_combinesBothCalls() {
        PagedResponse<Movie> movies = client.movies().list(
                RequestOptions.builder().limit(1).build());
        String movieId = movies.getItems().getFirst().getId();

        MovieWithQuotes result = client.movies().getWithQuotes(movieId);

        assertThat(result.getMovie()).isNotNull();
        assertThat(result.getMovie().getId()).isEqualTo(movieId);
        assertThat(result.getQuotes()).isNotNull();
    }

    @Test
    void listAll_streamsAcrossPages() {
        List<Movie> allMovies = client.movies().listAll(
                RequestOptions.builder().limit(3).build()
        ).collect(Collectors.toList());

        // The API has ~8 movies; streaming with limit=3 per page
        // should still return all of them
        assertThat(allMovies.size()).isGreaterThan(3);
    }
}
