package io.github.querytemplate;

@FunctionalInterface
public interface AssignNamedParameterDelegate<Q, P> {

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
