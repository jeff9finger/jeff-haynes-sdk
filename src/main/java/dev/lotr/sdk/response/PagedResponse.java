package dev.lotr.sdk.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Paginated response wrapper exposing both items and pagination metadata.
 *
 * <p>Implements {@link Iterable} for convenient iteration over the items
 * in the current page. Pagination is explicit — no hidden HTTP calls are
 * made when iterating. Use the metadata methods to decide whether to
 * fetch additional pages.
 *
 * <pre>{@code
 * PagedResponse<Movie> page = client.movies().list();
 * page.getTotal();       // total items across all pages
 * page.getPages();       // total number of pages
 * page.hasNextPage();    // whether more pages exist
 *
 * for (Movie movie : page) {
 *     System.out.println(movie.getName());
 * }
 * }</pre>
 *
 * @param <T> the item type (Movie, Quote, etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public final class PagedResponse<T> implements Iterable<T> {

    @JsonProperty("docs")
    private List<T> items;

    @JsonProperty("total")
    private int total;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("page")
    private int page;

    @JsonProperty("pages")
    private int pages;

    // Jackson no-arg constructor
    PagedResponse() {}

    /** Creates a PagedResponse for testing or manual construction. */
    public PagedResponse(List<T> items, int total, int limit,
                         int offset, int page, int pages) {
        this.items = items;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
        this.page = page;
        this.pages = pages;
    }

    public List<T> getItems() {
        return items != null ? Collections.unmodifiableList(items) : List.of();
    }

    public boolean hasNextPage() {
        return page < pages;
    }

    public boolean hasPreviousPage() {
        return page > 1;
    }

    @Override
    public Iterator<T> iterator() {
        return getItems().iterator();
    }
}
