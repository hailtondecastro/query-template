package io.github.querytemplate;

import java.util.Map;
import java.util.Set;

/**
 * Internal interface for QueryTemplate.
 * 
 * @param <Q> the type of the query. It is platform-specific (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
 */
public interface QueryTemplateInternal<Q> extends QueryTemplate<Q> {
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
	void setParamQueryRecursive(QueryTemplateState<Q> state,
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
	void setParamQuery(QueryTemplateState<Q> state, Q query, 
		Map<Integer, Action> positionalParameterActions);
	
	/**
	 * Internal method to create a QueryTemplate with a parent. This is used for nested templates.
	 * @param <SQ>  the type of the query. It is platform-specific (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
	 * @param config the configuration for the QueryTemplate.
	 * @param parent the parent QueryTemplate. It can be null if this is the root template.
	 * @return QueryTemplate instance with the given configuration and parent.
	 */
	static <SQ> QueryTemplate<SQ> of(QueryTemplateConfig<SQ> config, QueryTemplate<SQ> parent) {
		return new QueryTemplateDefault<>(config, (QueryTemplateInternal<SQ>)parent);
	}
	
	/**
	 * Returns the usable parameters of the query, i.e. the parameters that appear in
	 * it.
	 *
	 * @return the usable parameters.
	 */
	Set<PropertyMapper<Q, ?>> getUsableMappers();
}