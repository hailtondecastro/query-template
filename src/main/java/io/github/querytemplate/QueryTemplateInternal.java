package io.github.querytemplate;

import java.util.Map;
import java.util.Set;

/**
 * Internal interface for QueryTemplate.
 * 
 * @param <Q> the type of the query. It is platform-specific (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
 * @param <F> the type of the filter. It is used internally to create proxy objects and resolve properties by lambda expressions, used too for strong typing and IDE code completion.
 */
public interface QueryTemplateInternal<Q, F> extends QueryTemplate<Q, F> {
	/**
	 * Internal method to set the parameters of a query. This method is used for nested templates.<br>
	 * Recursively assigns values to the parameters based on the particip
	 * 
	 *  
	 * This method is used for nested templates.
	 * @param state the state of the query template.
	 * @param query the query to set the parameters on.
	 * @param positionalParameterActions a map of positional parameter actions to be applied to the query.
	 */
	void setParamQueryRecursive(QueryTemplateState<Q, F> state,
		Q query,
		Map<Integer, Action> positionalParameterActions);

	/**
	 * Internal method to set the parameters of a query. This method is used for nested templates.<br>
	 * 
	 * Assigns values to the parameters based on the participating mappers and the provided state. This method is used for nested templates.
	 * 
	 *
	 * @param state        the state of the query template.
	 * @param query        the query to set the parameters on.
	 * @param positionalParameterActions a map of positional parameter actions to be applied to the query.
	 */
	void setParamQuery(QueryTemplateState<Q, F> state, Q query, 
		Map<Integer, Action> positionalParameterActions);
	
	/**
	 * Internal method to create a QueryTemplate with a parent. This is used for nested templates.
	 * @param <SQ>  the type of the query. It is platform-specific (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
	 * @param <SF>  the type of the filter. It is used internally to create proxy objects and resolve properties by lambda expressions, used too for strong typing and IDE code completion.
	 * @param config the configuration for the QueryTemplate.
	 * @param parent the parent QueryTemplate. It can be null if this is the root template.
	 * @return QueryTemplate instance with the given configuration and parent.
	 */
	static <SQ, SF> QueryTemplate<SQ, SF> of(QueryTemplateConfig<SQ, SF> config, QueryTemplate<SQ, SF> parent) {
		return new QueryTemplateDefault<>(config, (QueryTemplateInternal<SQ, SF>)parent);
	}
	
	/**
	 * Returns the usable parameters of the query, i.e. the parameters that appear in
	 * it.
	 *
	 * @return the usable parameters.
	 */
	Set<PropertyMapper<Q, F, ?>> getUsableMappers();
}