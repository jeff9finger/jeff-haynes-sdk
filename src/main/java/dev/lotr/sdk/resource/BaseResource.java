package dev.lotr.sdk.resource;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lotr.sdk.config.OneApiConfig;
import dev.lotr.sdk.exception.AuthenticationException;
import dev.lotr.sdk.exception.NotFoundException;
import dev.lotr.sdk.exception.OneApiException;
import dev.lotr.sdk.exception.RateLimitException;
import dev.lotr.sdk.filter.RequestOptions;
import dev.lotr.sdk.http.HttpClient;
import dev.lotr.sdk.http.HttpResponse;
import dev.lotr.sdk.http.HttpStatus;
import dev.lotr.sdk.response.PagedResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Base class for API resource accessors.
 *
 * <p>Provides shared infrastructure for URL construction, HTTP execution,
 * JSON deserialization, and error mapping. Concrete subclasses only need
 * to specify the resource path and item type.
 *
 * <p>Extensibility: adding a new resource (e.g., {@code /book}) requires
 * one new subclass that calls {@code super("/book", Book.class, ...)}.
 *
 * @param <T> the domain model type (Movie, Quote, etc.)
 */
public abstract class BaseResource<T> {

    private final String resourcePath;
    private final Class<T> itemType;
    protected final OneApiConfig config;
    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;

    protected BaseResource(String resourcePath, Class<T> itemType,
                           OneApiConfig config, HttpClient httpClient,
                           ObjectMapper objectMapper) {
        this.resourcePath = resourcePath;
        this.itemType = itemType;
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Lists all items with default pagination.
     */
    public PagedResponse<T> list() {
        return list(null);
    }

    /**
     * Lists items with the given filter/pagination/sort options.
     */
    public PagedResponse<T> list(RequestOptions options) {
        String url = buildUrl(resourcePath, options);
        HttpResponse response = execute(url);
        return deserializePage(response.getBody());
    }

    /**
     * Retrieves a single item by ID.
     *
     * @param id the resource ID
     * @return the item
     * @throws NotFoundException if the resource does not exist
     */
    public T getById(String id) {
        String url = buildUrl(resourcePath + "/" + id, null);
        HttpResponse response = execute(url);
        PagedResponse<T> page = deserializePage(response.getBody());
        if (page.getItems().isEmpty()) {
            throw new NotFoundException("Resource not found: " + resourcePath + "/" + id);
        }
        return page.getItems().getFirst();
    }

    /**
     * Returns a lazily-paginating stream over all items matching the given options.
     *
     * <p>Pages are fetched on demand as the stream is consumed. The stream
     * respects the configured rate limiter and retries. Pagination parameters
     * in the provided options are ignored (the stream manages its own paging).
     *
     * <pre>{@code
     * client.movies().listAll()
     *     .filter(m -> m.getAcademyAwardWins() > 0)
     *     .forEach(m -> System.out.println(m.getName()));
     * }</pre>
     */
    public Stream<T> listAll() {
        return listAll(null);
    }

    /**
     * Returns a lazily-paginating stream with the given filter/sort options.
     *
     * @param options filters and sorting (pagination params are ignored)
     * @return a stream that fetches pages lazily
     */
    public Stream<T> listAll(RequestOptions options) {
        Iterator<T> iterator = new AutoPaginatingIterator(options);
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(
                iterator, Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false);
    }

    // --- Internal helpers ---

    protected String buildUrl(String path, RequestOptions options) {
        String url = null;
        if (options != null) {
            String query = options.toQueryString();
            if (!query.isEmpty()) {
                URI base = URI.create(config.getBaseUrl());
                try {
                    url = new URI(base.getScheme(),
                            base.getAuthority(),
                           base.getPath() + path,
                            query,
                           null
                    ).toASCIIString();
                } catch (URISyntaxException e) {
                    throw new OneApiException("Failed to build request URL with the specified options", e);
               }
            }
        }
        return url != null ? url : config.getBaseUrl() + path;
    }

    protected HttpResponse execute(String url) {
        HttpResponse response = httpClient.get(url, config.getApiKey());
        handleErrors(response);
        return response;
    }

    private void handleErrors(HttpResponse response) {
        switch (response.getStatusCode()) {
            case HttpStatus.OK -> { /* success */ }
            case HttpStatus.UNAUTHORIZED -> throw new AuthenticationException(
                    "Invalid or missing API key");
            case HttpStatus.NOT_FOUND -> throw new NotFoundException(
                    "Resource not found");
            case HttpStatus.TOO_MANY_REQUESTS -> throw new RateLimitException(
                    "Rate limit exceeded", response.getHeaders());
            default -> throw new OneApiException(
                    "API error (HTTP " + response.getStatusCode() + "): "
                            + response.getBody(),
                    response.getStatusCode());
        }
    }

    protected PagedResponse<T> deserializePage(String json) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(PagedResponse.class, itemType);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new OneApiException("Failed to parse API response: " + e.getMessage(), e);
        }
    }

    /**
     * Iterator that automatically fetches subsequent pages as needed.
     */
    private class AutoPaginatingIterator implements Iterator<T> {

        private final RequestOptions baseOptions;
        private PagedResponse<T> currentPage;
        private Iterator<T> currentIterator;
        private int nextPageNum = 1;
        private boolean exhausted = false;

        AutoPaginatingIterator(RequestOptions baseOptions) {
            this.baseOptions = baseOptions;
            fetchNextPage();
        }

        @Override
        public boolean hasNext() {
            if (currentIterator.hasNext()) {
                return true;
            }
            if (exhausted || !currentPage.hasNextPage()) {
                return false;
            }
            fetchNextPage();
            return currentIterator.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return currentIterator.next();
        }

        private void fetchNextPage() {
            RequestOptions.Builder pageBuilder = RequestOptions.builder()
                    .page(nextPageNum);

            // Carry over filters and sorting from base options
            if (baseOptions != null) {
                baseOptions.getFilters().forEach(pageBuilder::filter);
                if (baseOptions.getSortField() != null) {
                    pageBuilder.sort(baseOptions.getSortField(),
                            baseOptions.getSortDirection());
                }
                // Preserve limit if set (page size)
                if (baseOptions.getLimit() != null) {
                    pageBuilder.limit(baseOptions.getLimit());
                }
            }

            currentPage = list(pageBuilder.build());
            currentIterator = currentPage.getItems().iterator();
            nextPageNum++;

            if (!currentIterator.hasNext()) {
                exhausted = true;
            }
        }
    }
}
