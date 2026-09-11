package io.github.querytemplate;

import java.util.Map;
import java.util.Set;

/**
 * TODO: Support escaping with a parameterizable escape character.<br>
 * Works over a query string, assembling the query based on the filled
 * parameters.<br>
 * The query must have the following form ('substitution grammar'):<br>
 *
 * <pre>
 *
 * [fixed part of the query]
 * [ [param_token][and_token*][or_token*][no_operator*] *][query_helper*]
 * [filters_token]
 * [where_token*]
 * [open_parenthesis*]
 * [close_parenthesis*]
 * [query_helper*]
 * [param_token][repeat*][and_token*][or_token*][no_operator*][criterion_token]
 * [ [param_token][and_token*][or_token*][no_operator*] *][query_helper*]
 * [open_parenthesis*]
 * [close_parenthesis*]
 * [param_token][repeat*][and_token*][or_token*][no_operator*][criterion_token]
 * [ [param_token][and_token*][or_token*][no_operator*] *][query_helper*]
 * ...
 * [open_parenthesis*]
 * [close_parenthesis*]
 * [extra_token][and_token*][or_token*][no_operator*][criterion_token]
 * [open_parenthesis*]
 * [close_parenthesis*]
 * [query_helper*]
 * [fixed part of the query]
 * ...
 * </pre>
 *
 * * Optional<br>
 * Example:<br>
 *
 * <pre>
 * select *
 * from empl e
 * [filters]
 * [where]
 * [filterPrp1][e.att1 = :filterPrp1]
 * [filterPrp2][and][e.att2 &gt; :filterPrp2]
 * [(]
 *   [filterPrp3][e.att3 &lt; :filterPrp3]
 *   [filterPrp4][or][e.att4 &lt; :filterPrp4]
 *   [and][(]
 *     [filterPrp5][repeat][or][e.att5 = :filterPrp5]
 *   [)]
 * [)]
 * [extra][e.att4 = &quot;foo&quot;]
 * union
 * select *
 * from outsourced o
 * where
 * o.att1 = &quot;foo&quot;
 * and o.att2 = &quot;baa&quot;
 * and o.att3 = &quot;foo&quot;
 * [filters]
 * [filterPrp4][o.att4 = :filterPrp4]
 * </pre>
 *
 * Meanings:<br>
 * [filters_token]: Token that marks the beginning of the part to be assembled
 * conditionally.<br>
 * [where_token]: Optional token. Must be present when there is no preexisting
 * fixed criterion. It means the word "where" will be added before the first
 * criterion, in case any parameter (property in the filter) is filled.<br>
 * [and_token]: Optional token. Must be used when the logical operator that
 * precedes the filter clause is to be specified.<br>
 * [or_token]: Optional token. Must be used when the logical operator that
 * precedes the filter clause is to be specified.<br>
 * [no_operator]: Token meaning that the filter clause will have no operator (and,
 * or).<br>
 * [open_parenthesis]: Opening parenthesis. Only appears if it has content.<br>
 * [close_parenthesis]: Closing parenthesis. Only appears if it has content.<br>
 * [param_token]: Token with the names of the parameters (property in the filter)
 * separated by comma, surrounded by the parameter delimiters. All the parameters
 * must be filled for the criterion to be included. It is possible to use "!"
 * before the parameter name to invert its fill test, which will make it be
 * considered 'filled' when it is not and vice versa.<br>
 * OBS: Use the reserved word '$any$' at the beginning of the parameter list to
 * consider the criterion filled with at least one parameter instead of all of
 * them.<br>
 * [repeat] = Repeats the sentence once for each element in the array.<br>
 * [criterion_token]: Any criterion. It must not contain the connector ("and").<br>
 * [extra_token]: Marks a criterion that will compose the query in case any of the
 * preceding parameters is filled.<br>
 * [query_helper]: Another QueryTemplate that will be inserted into the query
 * based on the same filter items. If there is
 * "[ [param_token][and_token*][or_token*][no_operator*] *]" before the
 * [query_helper] then the use of this [query_helper] will be conditioned to the
 * respective parameter being 'filled'; in that case the operator can be included
 * in the final query ('and', 'or' or 'no operator').<br>
 */
public interface QueryTemplate<Q> {
	void setUp();

	/**
	 * Assigns values to the parameters based on the fill state..
	 *
	 * @param filterObject the filter object.
	 * @param query        the query to set the parameters on.
	 */
	void setParamQuery(QueryTemplateState<Q> state,
		Q query);

	/**
	 * Assembles the query according to the parameters that are filled.
	 * @param <Q>
	 *
	 * @param filter the filter object.
	 * @return the assembled query.
	 */
	QueryTemplateState<Q> buildQueryState(Object filter);

	/**
	 * Returns the usable parameters of the query, i.e. the parameters that appear in
	 * it.
	 *
	 * @return the usable parameters.
	 */
	Set<String> getUsableParameters();
	
	/**
	 * Internal method to set the parameters of a query. This method is used for nested templates.<br>
	 * 
	 * Assigns values to the parameters based on the fill state..
	 *
	 * @param filterObject the filter object.
	 * @param query        the query to set the parameters on.
	 */
	void setParamQuery(QueryTemplateState<Q> state, Q query, 
		Map<Integer, Action> positionalParameterActions);

	/**
	 * Internal method to create a QueryTemplateSpec with a parent. This is used for nested templates.
	 * @param <SQ>
	 * @param config
	 * @param parent
	 * @return
	 */
	public static <SQ> QueryTemplate<SQ> of(QueryTemplateConfig<SQ> config) {
		return new QueryTemplateDefault<>(config);
	}
}