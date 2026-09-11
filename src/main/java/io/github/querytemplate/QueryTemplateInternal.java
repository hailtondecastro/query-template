package io.github.querytemplate;

import java.util.Map;
import java.util.Set;

/**
 * Internal interface for QueryTemplate.
 * 
 * @param <Q> the type of the query.
 */
public interface QueryTemplateInternal<Q> extends QueryTemplate<Q> {
	/**
	 * Internal method to set the parameters of a query. This method is used for nested templates.<br>
	 * Recursively assigns values to the parameters based on the fill state. This method is used for nested templates.
	 * @param state
	 * @param query
	 * @param positionalParameterActions
	 */
	void setParamQueryRecursive(QueryTemplateState<Q> state,
		Q query,
		Map<Integer, Action> positionalParameterActions);

	/**
	 * Internal method to create a QueryTemplateSpec with a parent. This is used for nested templates.
	 * @param <SQ>
	 * @param config
	 * @param parent
	 * @return
	 */
	static <SQ> QueryTemplate<SQ> of(QueryTemplateConfig<SQ> config, QueryTemplate<SQ> parent) {
		return new QueryTemplateDefault<>(config, (QueryTemplateInternal<SQ>)parent);
	}
}