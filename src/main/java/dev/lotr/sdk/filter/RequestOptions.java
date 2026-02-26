package dev.lotr.sdk.filter;

import dev.lotr.sdk.model.field.FilterableField;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Combines filters, pagination, and sorting into a single request configuration.
 *
 * <p>Use the {@link #builder()} to construct instances fluently:
 * <pre>{@code
 * RequestOptions options = RequestOptions.builder()
 *         .filter(Filter.where("name").matchesRegex("/Ring/i"))
 *         .filter(Filter.where("budgetInMillions").greaterThan(100))
 *         .sort("name", SortDirection.ASC)
 *         .limit(10)
 *         .page(2)
 *         .build();
 * }</pre>
 */
@Getter
public final class RequestOptions {

    private final List<FilterExpression> filters;
    private final String sortField;
    private final SortDirection sortDirection;
    private final Integer limit;
    private final Integer page;
    private final Integer offset;

    private RequestOptions(Builder builder) {
        this.filters = Collections.unmodifiableList(new ArrayList<>(builder.filters));
        this.sortField = builder.sortField;
        this.sortDirection = builder.sortDirection;
        this.limit = builder.limit;
        this.page = builder.page;
        this.offset = builder.offset;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Converts these options into a query string (without leading '?').
     *
     * @return the query parameter string, or empty string if no options set
     */
    public String toQueryString() {
        StringJoiner joiner = new StringJoiner("&");

        for (FilterExpression filter : filters) {
            joiner.add(filter.toQueryParam());
        }

        if (sortField != null && sortDirection != null) {
            joiner.add("sort=" + sortField + ":" + sortDirection.getValue());
        }

        if (limit != null) {
            joiner.add("limit=" + limit);
        }

        if (page != null) {
            joiner.add("page=" + page);
        }

        if (offset != null) {
            joiner.add("offset=" + offset);
        }

        return joiner.toString();
    }

    public static final class Builder {

        private final List<FilterExpression> filters = new ArrayList<>();
        private String sortField;
        private SortDirection sortDirection;
        private Integer limit;
        private Integer page;
        private Integer offset;

        private Builder() {}

        public Builder filter(FilterExpression expression) {
            this.filters.add(expression);
            return this;
        }

        public Builder sort(String field, SortDirection direction) {
            this.sortField = field;
            this.sortDirection = direction;
            return this;
        }

        /**
         * Type-safe sort using a field enum.
         */
        public Builder sort(FilterableField field,
                            SortDirection direction) {
            this.sortField = field.getFieldName();
            this.sortDirection = direction;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public RequestOptions build() {
            return new RequestOptions(this);
        }
    }
}
