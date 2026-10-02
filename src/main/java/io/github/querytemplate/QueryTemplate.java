package io.github.querytemplate;

import java.util.Map;
import java.util.Set;

/**
 * TODO: Support escaping with a parameterizable escape character.<br>
 * Works over a query string, assembling the query based on the participation of 
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
 * criterion, in case any parameter (property in the filter) is participating in the query.<br>
 * .<br>
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
 * must be checked as 'participating in the query' for the criterion to be included. 
 * It is possible to use "!"
 * before the parameter name to invert its 'participating' test, which will make it be
 * considered 'participating in the query' when it is not and vice versa.<br>
 * OBS: Use the reserved word '$any$' at the beginning of the parameter list to
 * consider the criterion eligible for inclusion in the query if any of the parameters is participating.<br>
 * [repeat] = Repeats the sentence once for each element in the array.<br>
 * [criterion_token]: Any criterion. It must not contain the connector ("and").<br>
 * [extra_token]: Marks a criterion that will compose the query in case any of the
 * preceding parameters is participating in the query.<br>
 * [query_helper]: Another QueryTemplate that will be inserted into the query
 * based on the same filter items. If there is
 * "[ [param_token][and_token*][or_token*][no_operator*] *]" before the
 * [query_helper] then the use of this [query_helper] will be conditioned to the
 * respective properties being participating in the query in that case the operator
 * can be included in the preceding the [query_helper] content ('and', 'or' or 'no operator').<br>
 * 
 * @param <Q> the type of the query. It is platform-specific (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
 * @param <F> the type of the filter. It is used internally to create proxy objects and resolve properties by lambda expressions, used too for strong typing and IDE code completion.
 */
public interface QueryTemplate<Q, F> {
	void setUp();

	/**
	 * Assigns values to the parameters based on the participation of the properties in the query.
	 *
	 * @param state the state of the query template.
	 * @param query        the query to set the parameters on.
	 */
	void setParamQuery(QueryTemplateState<Q, F> state,
		Q query);

	/**
	 * Assembles the query according to the parameters that are participating in the query.
	 *
	 * @param filter the filter object.
	 * @return the assembled query.
	 */
	QueryTemplateState<Q, F> buildQueryState(F filter);

	/**
	 * Create a QueryTemplate instance with the given configuration.
	 * @param <SQ> the type of the query. It is platform-specific (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
	 * @param <SF> the type of the filter. It is used internally to create proxy objects and resolve properties by lambda expressions, used too for strong typing and IDE code completion.
	 * @param config the configuration for the QueryTemplate.
	 * @return QueryTemplate instance with the given configuration.
	 */
	public static <SQ, SF> QueryTemplate<SQ, SF> of(QueryTemplateConfig<SQ, SF> config) {
		return new QueryTemplateDefault<>(config);
	}
}