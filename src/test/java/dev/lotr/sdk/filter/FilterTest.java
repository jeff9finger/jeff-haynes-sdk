package dev.lotr.sdk.filter;

import dev.lotr.sdk.model.field.MovieField;
import dev.lotr.sdk.model.field.QuoteField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the fluent filter builder and request options.
 */
class FilterTest {

    // --- Filter.where(String) ---

    @Test
    void equalsFilter_producesCorrectQueryParam() {
        FilterExpression expr = Filter.where("name").equals("The Return of the King");
        assertThat(expr.toQueryParam()).isEqualTo("name=The Return of the King");
    }

    @Test
    void notEqualsFilter_producesCorrectQueryParam() {
        FilterExpression expr = Filter.where("name").notEquals("The Hobbit");
        assertThat(expr.toQueryParam()).isEqualTo("name!=The Hobbit");
    }

    @Test
    void greaterThanFilter_producesCorrectQueryParam() {
        FilterExpression expr = Filter.where("budgetInMillions").greaterThan(200);
        assertThat(expr.toQueryParam()).isEqualTo("budgetInMillions>200");
    }

    @Test
    void greaterThanOrEqualFilter_producesCorrectQueryParam() {
        FilterExpression expr = Filter.where("budgetInMillions").greaterThanOrEqual(200);
        assertThat(expr.toQueryParam()).isEqualTo("budgetInMillions>=200");
    }

    @Test
    void lessThanFilter_producesCorrectQueryParam() {
        FilterExpression expr = Filter.where("runtimeInMinutes").lessThan(180);
        assertThat(expr.toQueryParam()).isEqualTo("runtimeInMinutes<180");
    }

    @Test
    void lessThanOrEqualFilter_producesCorrectQueryParam() {
        FilterExpression expr = Filter.where("runtimeInMinutes").lessThanOrEqual(180);
        assertThat(expr.toQueryParam()).isEqualTo("runtimeInMinutes<=180");
    }

    @Test
    void regexFilter_producesCorrectQueryParam() {
        FilterExpression expr = Filter.where("name").matchesRegex("/Ring/i");
        assertThat(expr.toQueryParam()).isEqualTo("name=/Ring/i");
    }

    @Test
    void existsFilter_producesFieldNameOnly() {
        FilterExpression expr = Filter.where("name").exists();
        assertThat(expr.toQueryParam()).isEqualTo("name");
    }

    @Test
    void doesNotExistFilter_producesNegatedFieldName() {
        FilterExpression expr = Filter.where("name").doesNotExist();
        assertThat(expr.toQueryParam()).isEqualTo("!name");
    }

    @Test
    void inFilter_joinsValuesWithComma() {
        FilterExpression expr = Filter.where("name")
                .in("The Two Towers", "The Return of the King");
        assertThat(expr.toQueryParam())
                .isEqualTo("name=The Two Towers,The Return of the King");
    }

    @Test
    void notInFilter_joinsValuesWithComma() {
        FilterExpression expr = Filter.where("name").notIn("The Hobbit");
        assertThat(expr.toQueryParam()).isEqualTo("name!=The Hobbit");
    }

    // --- Filter.where(FilterableField) ---

    @Test
    void typeSafeFilter_usesFieldEnumName() {
        FilterExpression expr = Filter.where(MovieField.BUDGET_IN_MILLIONS).greaterThan(200);
        assertThat(expr.toQueryParam()).isEqualTo("budgetInMillions>200");
    }

    @Test
    void typeSafeFilter_worksWithQuoteField() {
        FilterExpression expr = Filter.where(QuoteField.DIALOG).matchesRegex("/ring/i");
        assertThat(expr.toQueryParam()).isEqualTo("dialog=/ring/i");
    }

    // --- Validation ---

    @SuppressWarnings("DataFlowIssue")
    @Test
    void nullFieldName_throwsException() {
        assertThatThrownBy(() -> Filter.where((String) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankFieldName_throwsException() {
        assertThatThrownBy(() -> Filter.where("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- RequestOptions ---

    @Test
    void emptyOptions_producesEmptyQueryString() {
        RequestOptions options = RequestOptions.builder().build();
        assertThat(options.toQueryString()).isEmpty();
    }

    @Test
    void optionsWithFilterAndSort_producesCorrectQueryString() {
        RequestOptions options = RequestOptions.builder()
                .filter(Filter.where("name").matchesRegex("/Ring/i"))
                .sort("name", SortDirection.ASC)
                .limit(10)
                .page(2)
                .build();

        String query = options.toQueryString();
        assertThat(query).contains("name=/Ring/i");
        assertThat(query).contains("sort=name:asc");
        assertThat(query).contains("limit=10");
        assertThat(query).contains("page=2");
    }

    @Test
    void optionsWithMultipleFilters_includesAll() {
        RequestOptions options = RequestOptions.builder()
                .filter(Filter.where("name").matchesRegex("/Ring/i"))
                .filter(Filter.where("budgetInMillions").greaterThan(100))
                .build();

        String query = options.toQueryString();
        assertThat(query).contains("name=/Ring/i");
        assertThat(query).contains("budgetInMillions>100");
    }

    @Test
    void typeSafeSort_usesFieldEnumName() {
        RequestOptions options = RequestOptions.builder()
                .sort(MovieField.RUNTIME_IN_MINUTES, SortDirection.DESC)
                .build();

        assertThat(options.toQueryString()).isEqualTo("sort=runtimeInMinutes:desc");
    }
}
