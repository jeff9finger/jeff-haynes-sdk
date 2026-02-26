package dev.lotr.sdk.resource;

import dev.lotr.sdk.OneApiClient;
import dev.lotr.sdk.TestFixtures;
import dev.lotr.sdk.exception.AuthenticationException;
import dev.lotr.sdk.exception.NotFoundException;
import dev.lotr.sdk.filter.Filter;
import dev.lotr.sdk.filter.RequestOptions;
import dev.lotr.sdk.filter.SortDirection;
import dev.lotr.sdk.http.HttpResponse;
import dev.lotr.sdk.http.MockHttpClient;
import dev.lotr.sdk.model.Movie;
import dev.lotr.sdk.model.MovieWithQuotes;
import dev.lotr.sdk.model.field.MovieField;
import dev.lotr.sdk.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MovieResource.
 */
class MovieResourceTest {

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

    // --- list() ---

    @Test
    void list_returnsMovies() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.MOVIE_LIST_JSON));

        PagedResponse<Movie> response = client.movies().list();

        assertThat(response.getTotal()).isEqualTo(3);
        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems().getFirst().getName())
                .isEqualTo("The Fellowship of the Ring");
    }

    @Test
    void list_sendsAuthorizationHeader() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.MOVIE_LIST_JSON));

        client.movies().list();

        assertThat(mockHttp.getRequests().getFirst().bearerToken()).isEqualTo("test-key");
    }

    @Test
    void list_withOptions_includesQueryParams() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.MOVIE_LIST_JSON));

        RequestOptions options = RequestOptions.builder()
                .filter(Filter.where("name").matchesRegex("/Ring/i"))
                .sort("name", SortDirection.ASC)
                .limit(10)
                .build();

        client.movies().list(options);

        String url = mockHttp.getRequests().getFirst().url();
        assertThat(url).contains("name=/Ring/i");
        assertThat(url).contains("sort=name:asc");
        assertThat(url).contains("limit=10");
    }

    @Test
    void list_withTypeSafeFilter_includesQueryParams() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.MOVIE_LIST_JSON));

        RequestOptions options = RequestOptions.builder()
                .filter(Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(200))
                .build();

        client.movies().list(options);

        String url = mockHttp.getRequests().getFirst().url();
        assertThat(url).contains("budgetInMillions>200");
    }

    // --- getById() ---

    @Test
    void getById_returnsSingleMovie() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.SINGLE_MOVIE_JSON));

        Movie movie = client.movies().getById("5cd95395de30eff6ebccde5d");

        assertThat(movie.getName()).isEqualTo("The Return of the King");
        assertThat(movie.getAcademyAwardWins()).isEqualTo(11);
    }

    @Test
    void getById_withEmptyDocs_throwsNotFoundException() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.EMPTY_RESPONSE_JSON));

        assertThatThrownBy(() -> client.movies().getById("nonexistent"))
                .isInstanceOf(NotFoundException.class);
    }

    // --- getWithQuotes() ---

    @Test
    void getWithQuotes_combinesMovieAndQuotes() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.SINGLE_MOVIE_JSON));
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.QUOTE_LIST_JSON));

        MovieWithQuotes result = client.movies()
                .getWithQuotes("5cd95395de30eff6ebccde5d");

        assertThat(result.getMovie().getName()).isEqualTo("The Return of the King");
        assertThat(result.getQuotes().getItems()).hasSize(2);
        assertThat(result.getQuotes().getItems().getFirst().getDialog())
                .isEqualTo("One Ring to rule them all.");
    }

    // --- listAll() (auto-pagination) ---

    @Test
    void listAll_streamsAcrossPages() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.PAGE_1_OF_3_JSON));
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.PAGE_2_OF_3_JSON));
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.PAGE_3_OF_3_JSON));

        List<String> names = client.movies().listAll()
                .map(Movie::getName)
                .collect(Collectors.toList());

        assertThat(names).containsExactly(
                "The Fellowship of the Ring",
                "The Two Towers",
                "The Return of the King"
        );
        assertThat(mockHttp.getRequestCount()).isEqualTo(3);
    }

    // --- Error handling ---

    @Test
    void unauthorizedResponse_throwsAuthenticationException() {
        mockHttp.enqueue(new HttpResponse(401, "Unauthorized"));

        assertThatThrownBy(() -> client.movies().list())
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void notFoundResponse_throwsNotFoundException() {
        mockHttp.enqueue(new HttpResponse(404, "Not Found"));

        assertThatThrownBy(() -> client.movies().getById("bad-id"))
                .isInstanceOf(NotFoundException.class);
    }

    // --- Pagination metadata ---

    @Test
    void pagedResponse_exposesMetadata() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.PAGE_1_OF_3_JSON));

        PagedResponse<Movie> page = client.movies().list();

        assertThat(page.getTotal()).isEqualTo(3);
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getPages()).isEqualTo(3);
        assertThat(page.hasNextPage()).isTrue();
        assertThat(page.hasPreviousPage()).isFalse();
    }

    @Test
    void pagedResponse_isIterable() {
        mockHttp.enqueue(new HttpResponse(200, TestFixtures.MOVIE_LIST_JSON));

        PagedResponse<Movie> page = client.movies().list();
        int count = 0;
        for (Movie movie : page) {
            assertThat(movie.getName()).isNotBlank();
            count++;
        }
        assertThat(count).isEqualTo(3);
    }
}
