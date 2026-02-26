package dev.lotr.sdk.demo;

import dev.lotr.sdk.OneApiClient;
import dev.lotr.sdk.filter.Filter;
import dev.lotr.sdk.filter.RequestOptions;
import dev.lotr.sdk.filter.SortDirection;
import dev.lotr.sdk.model.Movie;
import dev.lotr.sdk.model.MovieWithQuotes;
import dev.lotr.sdk.model.Quote;
import dev.lotr.sdk.model.field.MovieField;
import dev.lotr.sdk.response.PagedResponse;

/**
 * Demonstration of the Lord of the Rings SDK.
 *
 * <p>Usage:
 * <pre>
 * mvn compile exec:java \
 *   -Dexec.mainClass="dev.lotr.sdk.demo.DemoApp" \
 *   -Dexec.args="YOUR_API_KEY"
 * </pre>
 *
 * <p>Get an API key at <a href="https://the-one-api.dev/sign-up">the-one-api.dev</a>.
 */
public class DemoApp {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: DemoApp <api-key>");
            System.err.println("Get a key at https://the-one-api.dev/sign-up");
            System.exit(1);
        }

        OneApiClient client = OneApiClient.builder()
                .apiKey(args[0])
                .build();

        System.out.println("=== Lord of the Rings SDK Demo ===\n");

        // 1. List all movies
        System.out.println("--- All Movies ---");
        PagedResponse<Movie> movies = client.movies().list();
        for (Movie movie : movies) {
            System.out.printf("  %s (runtime: %d min, budget: $%.0fM)%n",
                    movie.getName(), movie.getRuntimeInMinutes(),
                    movie.getBudgetInMillions());
        }

        // 2. Filter: movies with Academy Award wins, sorted by wins desc
        System.out.println("\n--- Academy Award Winners (sorted by wins) ---");
        RequestOptions awardFilters = RequestOptions.builder()
                .filter(Filter.where(MovieField.ACADEMY_AWARD_WINS).greaterThan(0))
                .sort(MovieField.ACADEMY_AWARD_WINS, SortDirection.DESC)
                .build();

        PagedResponse<Movie> awardWinners = client.movies().list(awardFilters);
        for (Movie movie : awardWinners) {
            System.out.printf("  %s — %d wins%n",
                    movie.getName(), movie.getAcademyAwardWins());
        }

        // 3. Get a specific movie with its quotes (combined call)
        System.out.println("\n--- Movie With Quotes (combined call) ---");
        if (!movies.getItems().isEmpty()) {
            String movieId = movies.getItems().getFirst().getId();
            MovieWithQuotes movieWithQuotes = client.movies().getWithQuotes(movieId);
            System.out.printf("  Movie: %s%n", movieWithQuotes.getMovie().getName());
            System.out.printf("  Quotes (first page, %d total):%n",
                    movieWithQuotes.getQuotes().getTotal());
            movieWithQuotes.getQuotes().getItems().stream()
                    .limit(5)
                    .forEach(q -> System.out.printf("    \"%s\"%n", q.getDialog()));
        }

        // 4. Filter quotes with regex
        System.out.println("\n--- Quotes mentioning 'ring' (regex filter) ---");
        RequestOptions ringFilter = RequestOptions.builder()
                .filter(Filter.where("dialog").matchesRegex("/ring/i"))
                .limit(5)
                .build();

        PagedResponse<Quote> ringQuotes = client.quotes().list(ringFilter);
        for (Quote quote : ringQuotes) {
            System.out.printf("  \"%s\"%n", quote.getDialog());
        }

        // 5. Auto-paginating stream
        System.out.println("\n--- Streaming all movies with budget > $50M ---");
        long count = client.movies().listAll(
                RequestOptions.builder()
                        .filter(Filter.where(MovieField.BUDGET_IN_MILLIONS)
                                .greaterThan(50))
                        .build()
        ).peek(m -> System.out.printf("  %s ($%.0fM)%n",
                m.getName(), m.getBudgetInMillions()))
                .count();
        System.out.printf("  Total: %d movies%n", count);

        System.out.println("\n=== Demo Complete ===");
    }
}
