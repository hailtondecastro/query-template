package io.github.querytemplate;

/**
 * Enum used as a filter value to denote simply whether a query fragment should be
 * included or not, without providing any parameter value.
 */
public enum FragmentInclusion {
    /**
     * The related fragment must be included.
     */
    INCLUDE,
    /**
     * The related fragment must not be included.
     */
    DO_NOT_INCLUDE
}
