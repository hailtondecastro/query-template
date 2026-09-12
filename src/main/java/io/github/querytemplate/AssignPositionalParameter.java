package io.github.querytemplate;

@FunctionalInterface
public interface AssignPositionalParameterDelegate<Q, P> {

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
