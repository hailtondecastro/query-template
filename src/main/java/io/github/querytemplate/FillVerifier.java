package io.github.querytemplate;

/**
 * Verifies whether a property of a filter ({@link Object}) is filled.
 */
public interface FillVerifier {

    /**
     * Checks whether the property is filled.
     *
     * @param filter    the filter being inspected.
     * @param filterPrp the property to be evaluated.
     * @return {@code true} if the property is considered filled.
     */
    boolean isFilled(Object filter, String filterPrp);
}
