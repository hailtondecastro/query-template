package io.github.querytemplate;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration interface for QueryTemplate. It allows to set the tokens and reserved words used in the query template, as well as to add property mappers and query helpers.
 * @param <Q>
 */
public interface QueryTemplateConfig<Q> {

	class PropertyMapperConfig<Q, P> {
		PropertyMapper<Q, P> propertyMapper;
		QueryTemplateConfig<Q> queryTemplateConfig;
		
		PropertyMapperConfig(QueryTemplateConfig<Q> queryTemplateConfig) {
			this.queryTemplateConfig = queryTemplateConfig;
		}
		
		public QueryTemplateConfig<Q> done() {
			return this.queryTemplateConfig;
		}
		
	    private String filterPrp;
	    //Not used
	    //private String entityPrp;
	    private FillVerifier fillVerifier;
	    //private Type type = null;
	    private AssignNamedParameterDelegate<Q, P> nParameterCallback = null;
	    private AssignPositionalParameterDelegate<Q, P> pParameterCallback = null;
	    //private boolean parameterAsList = false;
	    private boolean unpackListItems = false;
	    private boolean repeater = false;
	    
		public PropertyMapperConfig<Q, P> filterPrp(String filterPrp) {
			this.filterPrp = filterPrp;
			return this;
		}
		public PropertyMapperConfig<Q, P> fillVerifier(FillVerifier fillVerifier) {
			this.fillVerifier = fillVerifier;
			return this;
		}
		
		public PropertyMapperConfig<Q, P> parameterCallback(AssignNamedParameterDelegate<Q, P> assignNamedParameterCallback) {
			this.nParameterCallback = assignNamedParameterCallback;
			return this;
		}
		public PropertyMapperConfig<Q, P> parameterCallback(AssignPositionalParameterDelegate<Q, P> assignPositionalParameterCallback) {
			this.pParameterCallback = assignPositionalParameterCallback;
			return this;
		}
//		public PropertyMapperConfig<Q, P> parameterAsList(boolean parameterAsList) {
//			this.parameterAsList = parameterAsList;
//			return this;
//		}
		public PropertyMapperConfig<Q, P> unpackListItems(boolean unpackListItems) {
			this.unpackListItems = unpackListItems;
			return this;
		}
		public PropertyMapperConfig<Q, P> repeater(boolean repeater) {
			this.repeater = repeater;
			return this;
		}
		public PropertyMapper<Q, P> getPropertyMapper() {
			return propertyMapper;
		}
		public QueryTemplateConfig<Q> getQueryTemplateConfig() {
			return queryTemplateConfig;
		}
		public String getFilterPrp() {
			return filterPrp;
		}
		public FillVerifier getFillVerifier() {
			return fillVerifier;
		}
		public AssignNamedParameterDelegate<Q, P> getAssignNamedParameterCallback() {
			return nParameterCallback;
		}
		public AssignPositionalParameterDelegate<Q, P> getAssignPositionalParameterCallback() {
			return pParameterCallback;
		}
//		public boolean isParameterAsList() {
//			return parameterAsList;
//		}
		public boolean isUnpackListItems() {
			return unpackListItems;
		}
		public boolean isRepeater() {
			return repeater;
		}
	}

	/** Default token for the reserved word "filters". Value: <code>"\\[filters\\]"</code> */
	String FILTERS_TOKEN = "\\[filters\\]";
	/** Default token for the reserved word "where". Value: <code>"\\[where\\]"</code> */
	String WHERE_TOKEN = "\\[where\\]";
	/** Default token for the reserved word "and". */
	String AND_TOKEN = "\\[and\\]";
	/** Default token for the reserved word "or". */
	String OR_TOKEN = "\\[or\\]";
	/** Token meaning that the filter clause will have no operator (and, or). */
	String NO_OPERATOR_TOKEN = "\\[no_operator\\]";
	/** Opening parenthesis. */
	String OPEN_PARENTHESIS_TOKEN = "\\[\\(\\]";
	/** Closing parenthesis. */
	String CLOSE_PARENTHESIS_TOKEN = "\\[\\)\\]";
	/** Default token for the reserved word "extra". */
	String EXTRA_TOKEN = "\\[extra\\]";
	/**
	 * OBS: Use the reserved word '$any$' at the beginning of the parameter list to
	 * consider the criterion filled with at least one parameter instead of all of
	 * them.<br>
	 */
	String RESERVED_ANY_PARAM = "\\$any\\$";
	/**
	 * Token with the names of the parameters (property in the filter) separated by
	 * comma, surrounded by the parameter delimiters. All the parameters must be
	 * filled for the criterion to be included.
	 */
	String PARAM_TOKEN = "\\[[a-zA-Z0-9|,|!|\\$| ]+\\]";
	/** Repeats the sentence once for each element in the array. */
	String REPEAT_TOKEN = "\\[repeat\\]";
	/** Default token for the criterion. */
	String CRITERION_TOKEN = "\\[[^\\]]*\\]";
	/** Default regular expression to remove the parameter and criterion delimiters. */
	String PARAM_DELIMITER_TOKEN = "\\[|\\]";
	/** Default regular expression to remove the criterion delimiters. */
	String CRITERION_DELIMITER_TOKEN = "\\[|\\]";
	/** Reused query, supports QueryTemplate "helpers". */
	String QUERY_HELPER_TOKEN = "\\[Q:[^\\]]*\\]";
	/** 
	 * Regular expression to match parameter names in the query. It matches words with letters, numbers, and hyphens. Value: <code>"\\b([\\w-]+)\\b"</code>. 
	 */
	String PARAMETER_NAME_PATTERN = "\\b([\\w-]+)\\b";
	/**
	 * Escape character. Used when a native character of the query conflicts with the
	 * delimiters of the substitution grammar of this class. OBS: It must be in
	 * regular expression format. Example: if the character is a backslash (\) then
	 * the string {@code "\\\\"} must be entered.
	 */
	String ESCAPE_CHARACTER = "\\\\";
	/**
	 * Reserved word to be used for "where" on the target query environment.
	 */
	String TARGET_RESERVED_WORD_WHERE = "where";
	/**
	 * Reserved word to be used for "and" on the target query environment.
	 */
	String TARGET_RESERVED_WORD_AND = "and";
	/**
	 * Reserved word to be used for "or" on the target query environment.
	 */
	String TARGET_RESERVED_WORD_OR = "or";
	/**
	 * Reserved word to be used for "(" on the target query environment.
	 */
	String TARGET_RESERVED_WORD_OPEN_PARENTHESIS = "(";
	/**
	 * Reserved word to be used for ")" on the target query environment.
	 */
	String TARGET_RESERVED_WORD_CLOSE_PARENTHESIS = ")";
	/** 
	 * Reserved word to be used for postional parameter marker on the target query environment.
	 */
	String TARGET_RESERVED_WORD_POSITIONAL_PARAMETER_MARKER = "?";
	/**
	 * Separator marker to be used for target item list on the target query
	 * environment. Value is ", " (comma followed by a space).
	 */
	String TARGET_ITEM_LIST_SEPARATOR_MARKER = ", ";

	<P> QueryTemplateConfig.PropertyMapperConfig<Q, P> addMapper(String filterPrp,
		Class<P> propertyClass);

	/**
	 * Removes the mapper configuration for the specified filter property.
	 * 
	 * @param filterPrp the filter property name.
	 * @return the removed mapper configuration.
	 */
	QueryTemplateConfig<Q> removeMapper(String filterPrp);
	
	/**
	 * Modifies the mapper configuration for the specified filter property.
	 * 
	 * @param filterPrp the filter property name.
	 * @return the modified mapper configuration.
	 */
	<P> QueryTemplateConfig.PropertyMapperConfig<Q, P> modifyMapper(String filterPrp, Class<P> propertyClass);
	
	/**
	 * Adds another {@link QueryTemplateConfig} inside the current instance. Inside the
	 * marked query there is something like:
	 *
	 * <pre>
	 * [Q:RelArrolamento.DetalheHelper]
	 * </pre>
	 *
	 * @param key         the query helper key.
	 * @param queryText the query helper.
	 * @return Child configuration instance that returns and operates all from parent, except:
	 * {@link #getQueryHelpers()}, {@link #addQueryHelper(String, String)} and 
	 * {@link #getQueryTextOriginal()}
	 * @param queryText
	 * @return
	 */
	QueryTemplateConfig<Q> addQueryHelper(String key,
		String queryText);

	/**
	 * 
	 * @param targetReservedWordPositionalParameterMarker the target reserved word for positional parameter markers. Default is "?".
	 * @return
	 */
	QueryTemplateConfig<Q> targetReservedWordPositionalParameterMarker(
		String targetReservedWordPositionalParameterMarker);

	/**
	 * @param convertNamedToPositionalParameters whether to convert named parameters to positional parameters.
	 * @return
	 */
	QueryTemplateConfig<Q> convertNamedToPositionalParameters(boolean convertNamedToPositionalParameters);

	/**
	 * @param targetReservedWordWhere see {@link #TARGET_RESERVED_WORD_WHERE}.
	 * @return
	 */
	QueryTemplateConfig<Q> targetReservedWordWhere(String targetReservedWordWhere);

	/**
	 * @param targetReservedWordAnd see {@link #TARGET_RESERVED_WORD_AND}.
	 * @return
	 */
	QueryTemplateConfig<Q> targetReservedWordAnd(String targetReservedWordAnd);

	/**
	 * @param targetReservedWordOr see {@link #TARGET_RESERVED_WORD_OR}.
	 * @return
	 */
	QueryTemplateConfig<Q> targetReservedWordOr(String targetReservedWordOr);

	/**
	 * @param targetReservedWordOpenParenthesis see {@link #TARGET_RESERVED_WORD_OPEN_PARENTHESIS}.
	 * @return
	 */
	QueryTemplateConfig<Q> targetReservedWordOpenParenthesis(String targetReservedWordOpenParenthesis);

	/**
	 * @param targetReservedWordCloseParenthesis see {@link #TARGET_RESERVED_WORD_CLOSE_PARENTHESIS}.
	 * @return
	 */
	QueryTemplateConfig<Q> targetReservedWordCloseParenthesis(String targetReservedWordCloseParenthesis);

	/**
	 * @param targetItemListSeparatorMarker the separator marker for list items.
	 *                                      Default is {@link #TARGET_ITEM_LIST_SEPARATOR_MARKER}.
	 * @return
	 */
	QueryTemplateConfig<Q> targetItemListSeparatorMarker(String targetItemListSeparatorMarker);

	/**
	 * @param parameterUsagePrefix the prefix used for parameter usage. Default is ":".
	 * @return
	 */
	QueryTemplateConfig<Q> parameterUsagePrefix(String parameterUsagePrefix);

	/**
	 * @param parameterNamePattern see {@link #PARAMETER_NAME_PATTERN}.
	 * @return
	 */
	QueryTemplateConfig<Q> parameterNamePattern(String parameterNamePattern);

	/**
	 * @param parameterBasePosition the base index for parameters. Default is 1.
	 * @return
	 */
	QueryTemplateConfig<Q> parameterBasePosition(int parameterBasePosition);

	/**
	 * Sets the filters token pattern. See {@link #FILTERS_TOKEN} for the default value.
	 * 
	 * @param filtersToken the filters token pattern.
	 * @return this instance for method chaining.
	 */
	QueryTemplateConfig<Q> filtersToken(String filtersToken);

	/**
	 * Sets the where token pattern. See {@link #WHERE_TOKEN} for the default value.
	 * 
	 * @param whereToken the where token pattern.
	 * @return this instance for method chaining.
	 */
	QueryTemplateConfig<Q> whereToken(String whereToken);

	QueryTemplateConfig<Q> andToken(String andToken);

	QueryTemplateConfig<Q> orToken(String orToken);

	QueryTemplateConfig<Q> noOperatorToken(String noOperatorToken);

	QueryTemplateConfig<Q> openParenthesisToken(String openParenthesisToken);

	QueryTemplateConfig<Q> closeParenthesisToken(String closeParenthesisToken);

	QueryTemplateConfig<Q> extraToken(String extraToken);

	QueryTemplateConfig<Q> criterionToken(String criterionToken);

	QueryTemplateConfig<Q> paramDelimiterToken(String paramDelimiterToken);

	QueryTemplateConfig<Q> criterionDelimiterToken(String criterionDelimiterToken);

	QueryTemplateConfig<Q> escapeCharacter(String escapeCharacter);

	QueryTemplateConfig<Q> compactQueryText(boolean compactQueryText);

	QueryTemplateConfig<Q> clearMappers();
	
	QueryTemplateConfig<Q> getParent();

	Pattern getFiltersToken();

	Pattern getAndToken();

	Pattern getOrToken();

	Pattern getNoOperatorToken();

	Pattern getOpenParenthesisToken();

	Pattern getCloseParenthesisToken();

	Pattern getExtraToken();

	Pattern getCriterionToken();

	Pattern getParamDelimiterToken();

	Pattern getCriterionDelimiterToken();

	String getEscapeCharacter();

	Map<String, QueryTemplateConfig.PropertyMapperConfig<Q, ?>> getMappersConfigMap();

	Map<String, QueryTemplateConfig<Q>> getQueryHelpers();

	String getTargetReservedWordWhere();

	Pattern getWhereToken();

	Pattern getParamToken();

	Pattern getRepeatToken();

	Pattern getQueryHelperToken();

	String getReservedAnyParam();

	String getTargetReservedWordAnd();

	String getTargetReservedWordOr();

	String getTargetReservedWordOpenParenthesis();

	String getTargetReservedWordCloseParenthesis();

	String getTargetReservedWordPositionalParameterMarker();

	String getTargetItemListSeparatorMarker();

	boolean isConvertNamedToPositionalParameters();

	String getParameterUsagePrefix();

	String getParameterNamePattern();

	int getParameterBasePosition();

	boolean isCompactQueryText();

	String getQueryTextOriginal();

	static <SQ> QueryTemplateConfig<SQ> of(String queryText, Class<SQ> queryClass) {
		return new QueryTemplateConfigRoot<SQ>(queryText);
	}
}