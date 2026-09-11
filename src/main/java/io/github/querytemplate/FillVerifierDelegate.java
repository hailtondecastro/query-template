package io.github.querytemplate;

/**
 * Functional interface used by {@link FillVerifierByDelegate}. Allows building a
 * {@link FillVerifier} with inline code (a lambda).
 */
@FunctionalInterface
public interface FillVerifierDelegate {

    /**
     * @param filter    the filter being inspected.
     * @param filterPrp the property to be evaluated.
     * @return {@code true} if the property is considered filled.
     */
    boolean isFill(Object filter, String filterPrp);
}
