package io.github.querytemplate;

/**
 * Functional interface for assigning a positional parameter to a query.
 * @param <Q> The type of the query object.
 * @param <P> The type of the property value to be assigned to the parameter.
 */
@FunctionalInterface
public interface AssignPositionalParameter<Q, P> {

	/**
	 * Sets the value of a parameter in the query.
	 *
	 * @param query        the query object where the parameter will be set.
	 * @param currentIndex the index of the parameter to be set.
	 * @param value        the value to be assigned to the parameter.
	 */
	void accept(Q query,
		int currentIndex,
		P value);
}
