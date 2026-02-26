package dev.lotr.sdk.model.field;

import lombok.Getter;

/**
 * Type-safe field references for Movie resource filtering and sorting.
 *
 * <pre>{@code
 * Filter.where(MovieField.NAME).equals("The Return of the King")
 * Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(200)
 *
 * RequestOptions.builder()
 *     .sort(MovieField.RUNTIME_IN_MINUTES, SortDirection.DESC)
 *     .build();
 * }</pre>
 */
@Getter
public enum MovieField implements FilterableField {
    ID("_id"),
    NAME("name"),
    RUNTIME_IN_MINUTES("runtimeInMinutes"),
    BUDGET_IN_MILLIONS("budgetInMillions"),
    BOX_OFFICE_REVENUE_IN_MILLIONS("boxOfficeRevenueInMillions"),
    ACADEMY_AWARD_NOMINATIONS("academyAwardNominations"),
    ACADEMY_AWARD_WINS("academyAwardWins"),
    ROTTEN_TOMATOES_SCORE("rottenTomatoesScore");

    private final String fieldName;

    MovieField(String fieldName) {
        this.fieldName = fieldName;
    }
}
