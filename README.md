# Lord of the Rings SDK

A Java SDK for [The One API](https://the-one-api.dev), providing type-safe access to Lord of the Rings movie and quote data.

## Requirements

- Java 21+
- Maven 3.9+
- API key from [the-one-api.dev](https://the-one-api.dev/sign-up)

## Quick Start

```java
OneApiClient client = OneApiClient.builder()
        .apiKey("your-api-key")
        .build();

// List all movies
PagedResponse<Movie> movies = client.movies().list();

// Get a specific movie
Movie movie = client.movies().getById("5cd95395de30eff6ebccde5d");

// Get a quote
Quote quote = client.quotes().getById("5cd96e05de30eff6ebcce7e9");
```

## Features

### Filtering

The SDK provides a fluent filter builder that supports all API filter operators:

```java
// String-based (flexible, works with any field)
Filter.where("budgetInMillions").greaterThan(200)

// Enum-based (type-safe, IDE-discoverable)
Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(200)

// All operators
Filter.where("name").equals("The Return of the King")
Filter.where("name").notEquals("The Hobbit")
Filter.where("name").in("The Two Towers", "The Return of the King")
Filter.where("name").matchesRegex("/Ring/i")
Filter.where("name").exists()
Filter.where("runtimeInMinutes").lessThanOrEqual(180)
```

### Pagination & Sorting

```java
RequestOptions options = RequestOptions.builder()
        .filter(Filter.where("name").matchesRegex("/Ring/i"))
        .sort(MovieField.ACADEMY_AWARD_WINS, SortDirection.DESC)
        .limit(10)
        .page(2)
        .build();

PagedResponse<Movie> response = client.movies().list(options);
response.getTotal();       // total items across all pages
response.hasNextPage();    // check for more pages
```

### Auto-Paginating Stream

Lazily fetch all pages as a Java Stream:

```java
client.movies().listAll()
        .filter(m -> m.getAcademyAwardWins() > 0)
        .forEach(m -> System.out.println(m.getName()));
```

### Combined Calls

Fetch a movie and its quotes in one call:

```java
MovieWithQuotes result = client.movies().getWithQuotes(movieId);
result.getMovie().getName();
result.getQuotes().getItems();
```

### Configuration

**Builder (recommended):**

```java
OneApiClient client = OneApiClient.builder()
        .apiKey("your-key")
        .baseUrl("https://the-one-api.dev/v2")  // default
        .timeout(Duration.ofSeconds(10))          // default
        .maxRetries(3)                            // default, 0 to disable
        .retryBackoff(attempt -> Duration.ofSeconds((long) Math.pow(2, attempt)))
        .build();
```

**Properties file:**

```java
OneApiClient client = OneApiClient.fromProperties("lotr-sdk.properties");
```

See `src/main/resources/lotr-sdk.properties.example` for available properties.

### Error Handling

The SDK throws typed unchecked exceptions:

```java
try {
    client.movies().getById("nonexistent");
} catch (AuthenticationException e) {
    // 401 — invalid API key
} catch (NotFoundException e) {
    // 404 — resource not found
} catch (RateLimitException e) {
    // 429 — rate limit exceeded (after retries)
} catch (OneApiException e) {
    // Base class for all SDK exceptions
}
```

## Running Tests

**Unit tests** (no API key required):

```bash
mvn test
```

**Integration tests** (requires API key):

```bash
LOTR_API_KEY=your-key mvn verify -Pintegration
```

## Running the Demo

```bash
mvn compile exec:java \
  -Dexec.mainClass="dev.lotr.sdk.main.DemoApp" \
  -Dexec.args="your-api-key"
```

## Project Structure

```
src/main/java/dev/lotr/sdk/
├── OneApiClient.java              # Entry point, builder
├── config/OneApiConfig.java       # Immutable configuration
├── model/
│   ├── Movie.java, Quote.java     # Domain models
│   ├── MovieWithQuotes.java       # Composite (combined call)
│   └── field/                     # Type-safe filter field enums
├── filter/
│   ├── Filter.java                # Fluent filter builder
│   ├── RequestOptions.java        # Combines filters/pagination/sorting
│   └── SortDirection.java
├── resource/
│   ├── BaseResource.java          # Shared: URL building, deserialization, listAll()
│   ├── MovieResource.java         # /movie endpoints + getWithQuotes()
│   └── QuoteResource.java         # /quote endpoints
├── http/
│   ├── HttpClient.java            # Interface (pluggable)
│   ├── DefaultHttpClient.java     # Java 21 HttpClient implementation
│   └── RetryingHttpClient.java    # Decorator: rate-limit retry with backoff
├── response/PagedResponse.java    # Pagination metadata + Iterable
├── exception/                     # Typed exception hierarchy
└── main/DemoApp.java              # Runnable demonstration
```

## Design

See [DESIGN.md](DESIGN.md) for architectural decisions and rationale.
