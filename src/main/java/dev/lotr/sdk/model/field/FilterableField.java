package dev.lotr.sdk.model.field;

/**
 * Interface for type-safe filter field references.
 *
 * <p>Implemented by resource-specific enums ({@link MovieField}, {@link QuoteField})
 * to enable compile-time field validation in filter expressions, while also
 * supporting raw string field names for flexibility.
 *
 * <pre>{@code
 * // Type-safe (IDE discovery, compile-time safety)
 * Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(200)
 *
 * // Flexible (works with any field, including new API additions)
 * Filter.where("budgetInMillions").greaterThan(200)
 * }</pre>
 *
 * @see MovieField
 * @see QuoteField
 */
public interface FilterableField {

    /**
     * Returns the API field name used in query parameters.
     *
     * @return the raw field name as expected by the API
     */
    String getFieldName();
}
