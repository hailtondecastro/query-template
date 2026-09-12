package io.github.querytemplate;

/**
 * Functional interface for assigning a named parameter to a query.
 * @param <Q> The type of the query object.
 * @param <P> The type of the property value to be assigned to the parameter.
 */
@FunctionalInterface
public interface AssignNamedParameter<Q, P> {

	/**
	 * Sets the parameter value in the query.
	 *
	 * @param query the query to set the parameter in.
	 * @param name  the name of the parameter.
	 * @param value the value of the parameter.
	 */
	void accept(Q query,
		String name,
		P value);

}
