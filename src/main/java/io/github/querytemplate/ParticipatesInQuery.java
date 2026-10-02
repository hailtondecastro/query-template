package io.github.querytemplate;

/**
 * Verifies whether a property of a filter ({@link Object}) is participating in the query.
 * The filter is a generic object that can be a POJO, Map, or any other type. The property is identified by its name (String).
 */
@FunctionalInterface
public interface ParticipatesInQuery<F> {

    /**
     * Checks whether the property is participating in the query.
     *
     * @param filter    the filter being inspected.
     * @param filterPrp the property to be evaluated.
     * @return {@code true} if the property is considered participating in the query
     */
    boolean isParticipating(F filter, String filterPrp);
}
