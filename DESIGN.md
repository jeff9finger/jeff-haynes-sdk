# SDK Design

## Overview

This SDK provides a Java client for [The One API](https://the-one-api.dev), covering the movie and quote endpoints. It is designed for production use with extensibility in mind — new resources (books, characters, chapters) can be added by following the established patterns without modifying existing code.

## Design Philosophy

Three principles guided every decision:

1. **Developer experience first** — The API a developer sees should read like intent, not infrastructure. `client.movies().list()` is immediately understandable; raw HTTP calls are not.

2. **Testability without trade-offs** — Every layer is interface-based. Tests use a `MockHttpClient` with zero external dependencies — no WireMock, no test containers, no network calls. The mock records requests for assertion and returns enqueued responses in order.

3. **Extensibility through abstraction** — The `BaseResource<T>` pattern means adding a new resource type (e.g., `BookResource`) requires only a new class with a constructor — URL building, error handling, deserialization, auto-pagination, and streaming are inherited.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    OneApiClient                           │
│  (Entry point — builder pattern, resource accessors)     │
├──────────────┬───────────────────────────────────────────┤
│ MovieResource│  QuoteResource  │  (future resources)     │
├──────────────┴───────────────────────────────────────────┤
│                  BaseResource<T>                          │
│  (URL construction, error mapping, deserialization,      │
│   listAll() auto-pagination)                             │
├──────────────────────────────────────────────────────────┤
│             HttpClient (interface)                        │
│  ┌─────────────────┐  ┌──────────────────┐               │
│  │DefaultHttpClient│  │RetryingHttpClient│               │
│  │(Java 21)        │  │(decorator)       │               │
│  └─────────────────┘  └──────────────────┘               │
│                        ┌──────────────────┐              │
│                        │ MockHttpClient   │  (tests)     │
│                        └──────────────────┘              │
└──────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Responsibility | Key Classes |
|-------|---------------|-------------|
| **Client** | Configuration, resource wiring | `OneApiClient`, `OneApiConfig` |
| **Resource** | Endpoint-specific logic, URL paths | `MovieResource`, `QuoteResource`, `BaseResource<T>` |
| **Filter** | Query parameter construction | `Filter`, `FilterExpression`, `RequestOptions` |
| **HTTP** | Transport abstraction + retry | `HttpClient`, `DefaultHttpClient`, `RetryingHttpClient` |
| **Response** | Pagination metadata | `PagedResponse<T>` |
| **Exception** | Error hierarchy | `OneApiException` and subtypes |
| **Model** | Domain objects + field enums | `Movie`, `Quote`, `MovieField`, `QuoteField` |


## Key Design Decisions

### 1. Builder Pattern for Client Construction

The `OneApiClient.Builder` collects all configuration before creating an immutable client. This avoids partially-initialized objects and provides a natural place for validation (API key is required) and optional overrides.

```java
OneApiClient client = OneApiClient.builder()
    .apiKey("your-key")
    .timeout(Duration.ofSeconds(15))
    .maxRetries(3)
    .retryBackoff(attempt -> Duration.ofSeconds((long) Math.pow(2, attempt)))
    .build();
```

An alternative `fromProperties()` factory method supports externalized configuration for teams that prefer properties files, without coupling the SDK to any specific config framework (Spring, Quarkus, etc.).

**Why:** Builder pattern is idiomatic Java for objects with multiple optional parameters. The builder is also the injection point for the custom HTTP client, which is what makes the entire SDK testable.

### 2. Resource-Oriented Access

Rather than flat methods (`client.getMovies()`, `client.getQuoteById()`), the SDK groups operations by REST resource:

```java
client.movies().list()
client.movies().getById(id)
client.movies().getQuotes(movieId)
client.quotes().list()
```

**Why:** This mirrors the REST resource model and follows the pattern used by well-regarded SDKs (Stripe, Twilio, AWS). It scales cleanly — adding `client.books()` follows the same pattern — and enables IDE autocompletion to guide discovery.

### 3. Fluent Filter Builder with Progressive Disclosure

The filter API accepts both type-safe enums and raw strings:

```java
// Type-safe — IDE discovery, compile-time validation
Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(200)

// Flexible — works with any field, including new API additions
Filter.where("budgetInMillions").greaterThan(200)
```

Both paths produce the same `FilterExpression`. The `FilterableField` interface is the shared contract that resource-specific enums (`MovieField`, `QuoteField`) implement.

| Approach | Compile-time safety | IDE discovery | Works with new API fields |
|----------|-------------------|---------------|--------------------------|
| Enum | Yes | Yes | No — requires SDK update |
| String | No | No | Yes — immediate |
| Both | Best of each | Beginners get enums, advanced users get strings | Yes |

**Why:** This "progressive disclosure" pattern provides guardrails for new users while preserving full flexibility for advanced use. It also shows that enum-based filtering is coupled to a specific API version — a tradeoff worth making explicit.

### 4. Pluggable HTTP Transport

The `HttpClient` interface has a single method:

```java
HttpResponse get(String url, String bearerToken);
```

The default implementation uses Java 21's built-in `java.net.http.HttpClient`. Consumers can provide their own implementation for testing, alternate HTTP libraries (OkHttp, Apache HttpClient), or custom behavior (logging, metrics, circuit breaking).

**Why:** This is the seam that makes the entire SDK testable without network access. Tests run in milliseconds with full control over responses via `MockHttpClient`, which enqueues responses and records requests for assertion.

### 5. Rate Limiting with Configurable Backoff

The API limits requests to 100 per minute. The SDK addresses this with a `RetryingHttpClient` decorator that wraps any `HttpClient` implementation:

- **Default:** Exponential backoff (1s, 2s, 4s) up to 3 retries
- **Configurable:** Injectable `Function<Integer, Duration>` for custom backoff strategies
- **Disableable:** `maxRetries(0)` skips the decorator entirely

```java
// Custom: fixed 500ms delay
.retryBackoff(attempt -> Duration.ofMillis(500))

// Custom: exponential with jitter
.retryBackoff(attempt -> {
    long base = (long) Math.pow(2, attempt) * 1000;
    long jitter = ThreadLocalRandom.current().nextLong(0, base / 2);
    return Duration.ofMillis(base + jitter);
})
```

The decorator pattern keeps retry concerns separated from transport concerns. The `RetryingHttpClient` delegates to the underlying `HttpClient` and only intercepts 429 responses. When a custom `HttpClient` is injected via the builder, the retry decorator is not applied — the caller controls the full HTTP pipeline.

**Why:** Transparent auto-retry can hide latency in request-handling contexts. Making the backoff strategy injectable lets callers choose the right tradeoff between convenience and control.

### 6. Typed Exception Hierarchy

```
OneApiException (base, unchecked)
├── AuthenticationException  (401)
├── NotFoundException        (404)
└── RateLimitException       (429)
```

Unchecked exceptions avoid polluting the API with checked exception noise. Callers who want to handle specific error cases can catch subtypes; callers who don't can let them propagate.

**Why:** The API has a small, well-defined set of error conditions. Typed exceptions let callers write targeted recovery logic (retry on 429, re-auth on 401) without parsing error messages.

### 7. Pagination: Explicit Control + Auto-Paginating Stream

`PagedResponse<T>` wraps the API's pagination metadata (`total`, `page`, `pages`, `limit`, `offset`) and implements `Iterable<T>` for for-each loops. Callers control page navigation explicitly — no hidden HTTP calls from the `list()` method.

For callers who want to iterate all items without managing pagination, `BaseResource<T>.listAll()` returns a lazily-paginating `Stream<T>` that fetches pages on demand:

```java
client.movies().listAll()
    .filter(m -> m.getAcademyAwardWins() > 0)
    .forEach(m -> System.out.println(m.getName()));
```

The stream respects the configured rate limiter (same `HttpClient` pipeline) and accepts `RequestOptions` for server-side filtering and sorting while managing pagination internally.

**Why:** Two levels of abstraction for two different use cases. `list()` gives full control to callers who need it (pagination-aware UIs, batch processing). `listAll()` provides convenience for callers who want all results without managing pages. Both are explicit about what they do — there are no hidden HTTP calls from "simple" operations.

### 8. Convenience Abstractions

The assignment states: *"The SDK does not have to mirror the API. You can add abstractions and/or combine different calls."*

Two abstractions address this:

**`MovieWithQuotes`** — A composite model returned by `client.movies().getWithQuotes(id)`. Makes two API calls internally (movie + quotes) and bundles the results. This eliminates the common pattern of fetching a movie and then separately fetching its quotes.

**`listAll()`** — The auto-paginating stream described above. Transforms a paginated API into a standard Java Stream, hiding the mechanical concern of page management.

Both demonstrate the SDK's value beyond raw HTTP: they provide idioms that feel natural to Java developers while encapsulating API-specific details.

### 9. Naming Convention: Plural Accessors for Singular API Paths

The API uses singular resource paths (`/movie`, `/quote`), which deviates from REST convention. The SDK uses plural accessors (`client.movies()`, `client.quotes()`) to follow established Java SDK conventions.

This is a deliberate divergence. The SDK abstracts away the API's unconventional naming — callers never see the raw URL paths. If someone debugs network traffic, the Javadoc on `MovieResource` notes the mapping. The plural form reads more naturally ("give me the movies resource") and aligns with the patterns developers expect from SDKs like Stripe (`client.customers()`) and AWS.

## Extensibility

### Adding a New Resource

To add support for the `/book` endpoint:

1. Create a `Book` model class with Jackson annotations
2. Create `BookResource extends BaseResource<Book>`:

```java
public final class BookResource extends BaseResource<Book> {
    public BookResource(OneApiConfig config, HttpClient httpClient,
                        ObjectMapper mapper) {
        super("/book", Book.class, config, httpClient, mapper);
    }
}
```

3. Add `books()` accessor to `OneApiClient`

The new resource immediately inherits: list with pagination, get by ID, filter/sort support, `listAll()` streaming, error handling, and deserialization.

### Adding a New Filter Field Enum

1. Create `BookField implements FilterableField`
2. Populate with the API's field names

The existing `Filter.where(FilterableField)` overload picks it up with no changes to the filter package.

## Testing Strategy

Tests are organized into three tiers:

**Unit tests** (`FilterTest`, `RetryingHttpClientTest`) — Verify isolated logic: filter expression generation, URL encoding, retry backoff behavior. No HTTP, no deserialization.

**Resource tests** (`MovieResourceTest`, `QuoteResourceTest`, `OneApiClientTest`) — Verify correct URL construction, response deserialization, error mapping, and builder behavior using `MockHttpClient`. These are integration tests at the component level — they exercise the full path from resource accessor to deserialized response.

**Live integration tests** (`MovieResourceIT`, `QuoteResourceIT`) — Hit the real API with a real API key. Run via `mvn verify -Pintegration`. Separated into `src/integration-test/` with a Maven profile so `mvn test` stays fast and self-contained.

All unit and resource tests use `MockHttpClient`, which:
- Enqueues responses in order for deterministic behavior
- Records requests for URL and authorization header assertions
- Requires zero external dependencies or network access

## Dependencies

The SDK has one runtime dependency beyond the Java 21 standard library:

| Dependency | Purpose | Justification |
|-----------|---------|---------------|
| Jackson Databind 2.17 | JSON deserialization | Industry standard; handles generics and parameterized types cleanly |

HTTP transport uses `java.net.http.HttpClient` from the standard library — no additional dependency needed.

Test dependencies: JUnit 5 and AssertJ.

## Trade-offs

| Decision | Trade-off | Rationale |
|----------|-----------|-----------|
| Unchecked exceptions | Callers might not realize exceptions are thrown | Cleaner API; consistent with Java SDK conventions (Jackson, Spring) |
| `listAll()` hides HTTP calls | Callers may not realize network calls are happening behind a Stream | The method name and Javadoc make this explicit; explicit `list()` remains available for full control |
| Jackson for JSON | Adds ~2.5MB to classpath | Most Java projects already include it; zero-config deserialization of generics |
| Java 21 minimum | Excludes Java 8-16 projects | Enables modern language features (switch expressions, `List.getFirst()`, text blocks); Java 21 is current LTS |
| Synchronous API only | No async support | Keeps the API simple; callers can wrap in `CompletableFuture` if needed |
| Enums + strings for filter fields | Two ways to do the same thing | Progressive disclosure: enums for safety, strings for flexibility. Documented and tested |
| Singular API paths, plural SDK accessors | Minor mismatch if debugging network traffic | Follows Java SDK convention; SDK callers never see raw paths |
| Default retry on 429 | Callers may not expect automatic retries | Configurable via `maxRetries(0)` to disable; default behavior matches production expectations |

## API Contract

The SDK was built against the API contract documented in [openapi.yaml](openapi.yaml). This OpenAPI 3.0 specification covers the five endpoints in scope, their request parameters (pagination, sorting, filtering), and response schemas. It serves as a reference for future maintainers and documents the exact contract the SDK was designed against.

The OpenAPI document is **not used for code generation** — it exists purely as documentation of the API surface. This is consistent with the assignment's constraint against code generation tools, while demonstrating an API-first development mindset.
