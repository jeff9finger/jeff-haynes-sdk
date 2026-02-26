package dev.lotr.sdk.filter;

import dev.lotr.sdk.model.field.FilterableField;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Fluent builder for constructing API filter expressions.
 *
 * <p>The One API supports several filtering operators. This builder provides
 * a type-safe, readable way to construct them without dealing with raw query
 * parameter syntax.
 *
 * <h3>Usage examples:</h3>
 * <pre>{@code
 * // Exact match
 * Filter.where("name").equals("The Return of the King")
 *
 * // Negation
 * Filter.where("name").notEquals("The Hobbit")
 *
 * // Comparison operators (for numeric fields)
 * Filter.where("budgetInMillions").greaterThan(200)
 * Filter.where("runtimeInMinutes").lessThanOrEqual(180)
 *
 * // Regex match
 * Filter.where("name").matchesRegex("/Ring/i")
 *
 * // Exists check
 * Filter.where("name").exists()
 * Filter.where("name").doesNotExist()
 *
 * // Include (value in list)
 * Filter.where("name").in("The Two Towers", "The Return of the King")
 *
 * // Exclude (value not in list)
 * Filter.where("name").notIn("The Hobbit")
 *
 * // Type-safe with field enums
 * Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(200)
 * }</pre>
 *
 * @see RequestOptions
 * @see FilterableField
 */
public final class Filter {

    private final String field;

    private Filter(String field) {
        this.field = field;
    }

    /**
     * Begins building a filter expression for the given field name.
     *
     * @param field the API field name (e.g., "name", "budgetInMillions")
     * @return a Filter builder bound to the specified field
     */
    public static Filter where(String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("Field name must not be null or blank");
        }
        return new Filter(field);
    }

    /**
     * Begins building a filter expression using a type-safe field enum.
     *
     * @param field a {@link FilterableField} enum constant
     * @return a Filter builder bound to the specified field
     */
    public static Filter where(FilterableField field) {
        if (field == null) {
            throw new IllegalArgumentException("Field must not be null");
        }
        return new Filter(field.getFieldName());
    }

    /** Exact match: {@code field=value} */
    public FilterExpression equals(String value) {
        return new FilterExpression(field + "=" + encode(value));
    }

    /** Negated match: {@code field!=value} */
    public FilterExpression notEquals(String value) {
        return new FilterExpression(field + "!=" + encode(value));
    }

    /** Include filter: {@code field=val1,val2,val3} */
    public FilterExpression in(String... values) {
        String joined = Arrays.stream(values)
                .map(this::encode)
                .collect(Collectors.joining(","));
        return new FilterExpression(field + "=" + joined);
    }

    /** Exclude filter: {@code field!=val1,val2} */
    public FilterExpression notIn(String... values) {
        String joined = Arrays.stream(values)
                .map(this::encode)
                .collect(Collectors.joining(","));
        return new FilterExpression(field + "!=" + joined);
    }

    /** Field exists (has a non-empty value). */
    public FilterExpression exists() {
        return new FilterExpression(field);
    }

    /** Field does not exist (is empty or missing): {@code !field} */
    public FilterExpression doesNotExist() {
        return new FilterExpression("!" + field);
    }

    /**
     * Regex match: {@code field=/pattern/options}
     *
     * @param regex the regex pattern including delimiters, e.g., {@code "/Ring/i"}
     */
    public FilterExpression matchesRegex(String regex) {
        return new FilterExpression(field + "=" + regex);
    }

    /** Less than: {@code field<value} */
    public FilterExpression lessThan(Number value) {
        return new FilterExpression(field + "<" + value);
    }

    /** Greater than: {@code field>value} */
    public FilterExpression greaterThan(Number value) {
        return new FilterExpression(field + ">" + value);
    }

    /** Greater than or equal to: {@code field>=value} */
    public FilterExpression greaterThanOrEqual(Number value) {
        return new FilterExpression(field + ">=" + value);
    }

    /** Less than or equal to: {@code field<=value} */
    public FilterExpression lessThanOrEqual(Number value) {
        return new FilterExpression(field + "<=" + value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
