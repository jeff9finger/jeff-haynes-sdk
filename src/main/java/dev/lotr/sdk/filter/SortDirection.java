package dev.lotr.sdk.filter;

import lombok.Getter;

/**
 * Sort direction for API result ordering.
 */
@Getter
public enum SortDirection {
    ASC("asc"),
    DESC("desc");

    private final String value;

    SortDirection(String value) {
        this.value = value;
    }
}
