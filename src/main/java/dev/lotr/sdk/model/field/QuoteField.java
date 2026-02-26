package dev.lotr.sdk.model.field;

import lombok.Getter;

/**
 * Type-safe field references for Quote resource filtering and sorting.
 *
 * <pre>{@code
 * Filter.where(QuoteField.DIALOG).matchesRegex("/ring/i")
 * Filter.where(QuoteField.CHARACTER).equals("5cd99d4bde30eff6ebccfbe6")
 * }</pre>
 */
@Getter
public enum QuoteField implements FilterableField {
    ID("_id"),
    DIALOG("dialog"),
    MOVIE_ID("movie"),
    CHARACTER_ID("character");

    private final String fieldName;

    QuoteField(String fieldName) {
        this.fieldName = fieldName;
    }
}
