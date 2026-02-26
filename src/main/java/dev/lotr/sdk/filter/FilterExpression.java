package dev.lotr.sdk.filter;

/**
 * A compiled filter expression ready to be appended as a query parameter.
 *
 * <p>Instances are created by the {@link Filter} builder and are immutable.
 * The {@link #toQueryParam()} method returns the raw query string fragment
 * (e.g., {@code "name=The+Return+of+the+King"}).
 *
 * @see Filter
 */
public final class FilterExpression {

    private final String expression;

    FilterExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Returns the raw query parameter representation of this filter.
     *
     * @return the filter as a URL query parameter fragment
     */
    public String toQueryParam() {
        return expression;
    }

    @Override
    public String toString() {
        return expression;
    }
}
