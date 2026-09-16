package io.github.querytemplate;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration interface for QueryTemplate. It allows to set the tokens and reserved words used in the query template, as well as to add property mappers and query helpers.
 * @param <Q> Plataform-specific query type (e.g., String for SQL, CriteriaQuery for JPA, etc.).
 * It is not used internally, it is only for strong typing and IDE code completion.
 */
public interface QueryTemplateConfig<Q> {

	/** 
	 * Default token for the reserved word "filters". Value: <code>"\\[filters\\]"</code>.
	 */
	String FILTERS_TOKEN = "\\[filters\\]";
	/** 
	 * Default token for the reserved word "where". Value: <code>"\\[where\\]"</code>.
	 */
	String WHERE_TOKEN = "\\[where\\]";
	/** 
	 * Default token for the reserved word "and". Value: <code>"\\[and\\]"</code>.
	 */
	String AND_TOKEN = "\\[and\\]";
	/** 
	 * Default token for the reserved word "or". Value: <code>"\\[or\\]"</code>.
	 */
	String OR_TOKEN = "\\[or\\]";
	/** 
	 * Token meaning that the filter clause will have no operator (and, or). Value: <code>"\\[no_operator\\]"</code>.
	 */
	String NO_OPERATOR_TOKEN = "\\[no_operator\\]";
	/** 
	 * Opening parenthesis. Value: <code>"\\[\\(\\]"</code>.
	 */
	String OPEN_PARENTHESIS_TOKEN = "\\[\\(\\]";
	/** 
	 * Closing parenthesis. Value: <code>"\\[\\)\\]"</code>.
	 */
	String CLOSE_PARENTHESIS_TOKEN = "\\[\\)\\]";
	/** 
	 * Default token for the reserved word "extra". Value: <code>"\\[extra\\]"</code>.
	 */
	String EXTRA_TOKEN = "\\[extra\\]";
	/**
	 * OBS: Use the reserved word '$any$' at the beginning of the properties list to
	 * consider the criterion filled with at least one parameter instead of all of
	 * them.<br> 
	 * Value: <code>"\\$any\\$"</code>.
	 */
	String RESERVED_ANY_PROPERTY = "\\$any\\$";
	/**
	 * Token with the names of the properties separated by
	 * comma, surrounded by the parameter delimiters. All the parameters must be
	 * filled for the criterion to be included.<br>
	 * Value: <code>"\\[[a-zA-Z0-9|,|!|\\$| ]+\\]"</code>.
	 */
	String PROPERTIES_TOKEN = "\\[[a-zA-Z0-9|,|!|\\$| ]+\\]";
	/** Repeats the sentence once for each element in the array. */
	String REPEAT_TOKEN = "\\[repeat\\]";
	/** Default token for the criterion. */
	String CRITERION_TOKEN = "\\[[^\\]]*\\]";
	/** Default regular expression to remove the properties and criterion delimiters. */
	String PROPERTIES_DELIMITER_TOKEN = "\\[|\\]";
	/** Default regular expression to remove the criterion delimiters. */
	String CRITERION_DELIMITER_TOKEN = "\\[|\\]";
	/** Reused query, supports QueryTemplate "helpers". */
	String QUERY_HELPER_TOKEN = "\\[Q:[^\\]]*\\]";
	/** 
	 * Regular expression to identify parameter names used in query criteria.
	 * It matches words with letters, numbers, and hyphens.
	 * Value: <code>"\\b([\\w-]+)\\b"</code>. 
	 */
	String PARAMETER_NAME_PATTERN = "\\b([\\w-]+)\\b";
	/**
	 * Escape character. Used when a native character of the query conflicts with the
	 * delimiters of the substitution grammar of this class. OBS: It must be in
	 * regular expression format. Example: if the character is a backslash (\) then
	 * the string {@code "\\\\"} must be entered.
	 * Value: <code>"\\\\\\"</code> (double backslash).
	 */
	String ESCAPE_CHARACTER = "\\\\";
	/**
	 * Reserved word to be used for "where" on the target query environment.
	 * Value: <code>"where"</code>.
	 */
	String TARGET_RESERVED_WORD_WHERE = "where";
	/**
	 * Reserved word to be used for "and" on the target query environment.
	 * Value: <code>"and"</code>.
	 */
	String TARGET_RESERVED_WORD_AND = "and";
	/**
	 * Reserved word to be used for "or" on the target query environment.
	 * Value: <code>"or"</code>.
	 */
	String TARGET_RESERVED_WORD_OR = "or";
	/**
	 * Reserved word to be used for "(" on the target query environment.
	 * Value: <code>"("</code>.
	 */
	String TARGET_RESERVED_WORD_OPEN_PARENTHESIS = "(";
	/**
	 * Reserved word to be used for ")" on the target query environment.
	 * Value: <code>")"</code>.
	 */
	String TARGET_RESERVED_WORD_CLOSE_PARENTHESIS = ")";
	/** 
	 * Reserved word to be used for postional parameter marker on the target query environment.
	 * Value: <code>"?"</code>.
	 */
	String TARGET_RESERVED_WORD_POSITIONAL_PARAMETER_MARKER = "?";
	/**
	 * Separator marker to be used for target item list on the target query
	 * environment. Value is ", " (comma followed by a space).
	 * Value: <code>", "</code>.
	 */
	String TARGET_ITEM_LIST_SEPARATOR_MARKER = ", ";

	/**
	 * Adds a property mapper configuration for the specified filter property.
	 * 
	 * @param <P> The type of the property to be mapped.
	 * @param filterPrp the filter property name.
	 * @param propertyClass the class of the property. Internally it is not used, it is only for strong typing and IDE code completion.
	 * @return the added mapper configuration.
	 */
	<P> PropertyMapperConfig<Q, P> addMapper(String filterPrp,
		Class<P> propertyClass);

	/**
	 * Removes the mapper configuration for the specified filter property.
	 * 
	 * @param filterPrp the filter property name.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> removeMapper(String filterPrp);
	
	/**
	 * Modifies the mapper configuration for the specified filter property.
	 * 
	 * @param filterPrp the filter property name.
	 * @param propertyClass the class of the property. Internally it is not used, it is only for strong typing and IDE code completion.
	 * @param <P> The type of the property to be mapped. See propertyClass parameter.
	 * @return This instance for method chaining.
	 * 
	 */
	<P> PropertyMapperConfig<Q, P> modifyMapper(String filterPrp, Class<P> propertyClass);
	
	/**
	 * Adds another {@link QueryTemplateConfig} inside the current instance. Inside the
	 * marked query there is something like:
	 *
	 * <pre>
	 * [Q:RelArrolamento.DetalheHelper]
	 * </pre>
	 *
	 * @param key         the query helper key.
	 * @param queryText the query helper text.
	 * @return Child configuration instance that returns and operates all from parent, except:
	 * {@link #getQueryHelpers()}, {@link #addQueryHelper(String, String)} and 
	 * {@link #getQueryTextOriginal()}
	 */
	QueryTemplateConfig<Q> addQueryHelper(String key,
		String queryText);

	/**
	 * Removes a query helper from the current instance.
	 * 
	 * @param targetReservedWordPositionalParameterMarker the target reserved word for positional parameter markers. Default is "?".
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> targetReservedWordPositionalParameterMarker(
		String targetReservedWordPositionalParameterMarker);

	/**
	 * Sets whether to convert named parameters to positional parameters. Default is false.
	 * 
	 * @param convertNamedToPositionalParameters whether to convert named parameters to positional parameters.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> convertNamedToPositionalParameters(boolean convertNamedToPositionalParameters);

	/**
	 * Sets the target reserved word for "where". Default is "where".
	 * 
	 * @param targetReservedWordWhere see {@link #TARGET_RESERVED_WORD_WHERE}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> targetReservedWordWhere(String targetReservedWordWhere);

	/**
	 * Sets the target reserved word for "and". Default is "and".
	 * 
	 * @param targetReservedWordAnd see {@link #TARGET_RESERVED_WORD_AND}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> targetReservedWordAnd(String targetReservedWordAnd);

	/**
	 * Sets the target reserved word for "or". Default is "or".
	 * 
	 * @param targetReservedWordOr see {@link #TARGET_RESERVED_WORD_OR}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> targetReservedWordOr(String targetReservedWordOr);

	/**
	 * Sets the target reserved word for open parenthesis. Default is "(".
	 * 
	 * @param targetReservedWordOpenParenthesis see {@link #TARGET_RESERVED_WORD_OPEN_PARENTHESIS}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> targetReservedWordOpenParenthesis(String targetReservedWordOpenParenthesis);

	/**
	 * Sets the target reserved word for close parenthesis. Default is ")".
	 * 
	 * @param targetReservedWordCloseParenthesis see {@link #TARGET_RESERVED_WORD_CLOSE_PARENTHESIS}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> targetReservedWordCloseParenthesis(String targetReservedWordCloseParenthesis);

	/**
	 * Sets the target reserved word for positional parameter markers. Default is "?".
	 * 
	 * @param targetItemListSeparatorMarker the separator marker for list items.
	 *                                      Default is {@link #TARGET_ITEM_LIST_SEPARATOR_MARKER}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> targetItemListSeparatorMarker(String targetItemListSeparatorMarker);

	/**
	 * Sets the prefix used for parameter usage. Default is ":".
	 * 
	 * @param parameterUsagePrefix the prefix used for parameter usage. Default is ":".
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> parameterUsagePrefix(String parameterUsagePrefix);

	/**
	 * Sets the pattern used for parameter names. Default is "\\b([\\w-]+)\\b".
	 * 
	 * @param parameterNamePattern see {@link #PARAMETER_NAME_PATTERN}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> parameterNamePattern(String parameterNamePattern);

	/**
	 * Sets the base index for parameters. Default is 1.
	 * 
	 * @param parameterBasePosition the base index for parameters. Default is 1.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> parameterBasePosition(int parameterBasePosition);

	/**
	 * Sets the filters token pattern. See {@link #FILTERS_TOKEN} for the default value.
	 * 
	 * @param filtersToken the filters token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> filtersToken(String filtersToken);

	/**
	 * Sets the where token pattern. See {@link #WHERE_TOKEN} for the default value.
	 * 
	 * @param whereToken the where token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> whereToken(String whereToken);

	/**
	 * Sets the properties token pattern. See {@link #PROPERTIES_TOKEN} for the
	 * default value.
	 * 
	 * @param propertiesToken the properties token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> propertiesToken(String propertiesToken);
	
	/**
	 * Sets the repeat token pattern. See {@link #REPEAT_TOKEN} for the default
	 * value.
	 * 
	 * @param repeatToken the repeat token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> repeatToken(String repeatToken);
	
	/**
	 * Sets the query helper token pattern. See {@link #QUERY_HELPER_TOKEN} for the
	 * default value.
	 * 
	 * @param queryHelperToken the query helper token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> queryHelperToken(String queryHelperToken);
	
	
	/**
	 * Sets the and token pattern. See {@link #AND_TOKEN} for the default value.
	 * 
	 * @param andToken the and token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> andToken(String andToken);

	/**
	 * Sets the or token pattern. See {@link #OR_TOKEN} for the default value.
	 * @param orToken the or token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> orToken(String orToken);

	/**
	 * Sets the no operator token pattern. See {@link #NO_OPERATOR_TOKEN} for the
	 * default value.
	 * 
	 * @param noOperatorToken the no operator token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> noOperatorToken(String noOperatorToken);

	/**
	 * Sets the open parenthesis token pattern. See {@link #OPEN_PARENTHESIS_TOKEN}
	 * for the default value.
	 * 
	 * @param openParenthesisToken the open parenthesis token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> openParenthesisToken(String openParenthesisToken);

	/**
	 * Sets the close parenthesis token pattern. See
	 * {@link #CLOSE_PARENTHESIS_TOKEN} for the default value.
	 * 
	 * @param closeParenthesisToken the close parenthesis token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> closeParenthesisToken(String closeParenthesisToken);

	/**
	 * Sets the extra token pattern. See {@link #EXTRA_TOKEN} for the default value.
	 * 
	 * @param extraToken the extra token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> extraToken(String extraToken);

	/**
	 * Sets the criterion token pattern. See {@link #CRITERION_TOKEN} for the
	 * default value.
	 * 
	 * @param criterionToken the criterion token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> criterionToken(String criterionToken);
	
	/**
	 * Sets the parameter delimiter token pattern. See
	 * {@link #PROPERTIES_DELIMITER_TOKEN} for the default value.
	 * 
	 * @param propertiesDelimiterToken the parameter delimiter token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> propertiesDelimiterToken(String propertiesDelimiterToken);

	/**
	 * Sets the criterion delimiter token pattern. See
	 * {@link #CRITERION_DELIMITER_TOKEN} for the default value.
	 * 
	 * @param criterionDelimiterToken the criterion delimiter token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> criterionDelimiterToken(String criterionDelimiterToken);

	/**
	 * Sets the escape character. See {@link #ESCAPE_CHARACTER} for the default
	 * value.
	 * 
	 * @param escapeCharacter the escape character.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> escapeCharacter(String escapeCharacter);
	
	/**
	 * Sets the reserved word for `any` property markers. See
	 * {@link #RESERVED_ANY_PROPERTY} for the default value.
	 * 
	 * @param reservedAnyProperty the reserved word for `any` parameter markers.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> reservedAnyProperty(String reservedAnyProperty);

	/**
	 * Sets whether to compact the query text. If true, the query text will be
	 * compacted by removing extra spaces and line breaks.
	 * 
	 * @param compactQueryText whether to compact the query text.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> compactQueryText(boolean compactQueryText);

	/**
	 * Clears all property mappers from the configuration.
	 * 
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q> clearMappers();
	
	/**
	 * Gets the parent configuration after defining a QueryHelper. If this is the root configuration, returns null.
	 * 
	 * @return Return for the parent config after define QueryHelper. If this is the root config, return null.
	 */
	QueryTemplateConfig<Q> getParent();

	/**
	 * Gets the filters token pattern. See {@link #FILTERS_TOKEN} for the default value.
	 * 
	 * @return the filters token pattern.
	 */
	Pattern getFiltersToken();

	/**
	 * Gets and token pattern. See {@link #AND_TOKEN} for the default value.
	 * 
	 * @return the and token pattern.
	 */
	Pattern getAndToken();

	/**
	 * Gets the or token pattern. See {@link #OR_TOKEN} for the default value.
	 * 
	 * @return the or token pattern.
	 */
	Pattern getOrToken();

	/**
	 * Gets the `no operator` token pattern. See {@link #NO_OPERATOR_TOKEN} for the default value.`
	 * 
	 * @return the `no operator` token pattern.
	 */
	Pattern getNoOperatorToken();

	/**
	 * Gets the open parenthesis token pattern. See {@link #OPEN_PARENTHESIS_TOKEN} for the default value.
	 * 
	 * @return the open parenthesis token pattern.
	 */
	Pattern getOpenParenthesisToken();

	/**
	 * Gets the close parenthesis token pattern. See {@link #CLOSE_PARENTHESIS_TOKEN} for the default value.
	 * 
	 * @return the close parenthesis token pattern.
	 */
	Pattern getCloseParenthesisToken();

	/**
	 * Gets the extra token pattern. See {@link #EXTRA_TOKEN} for the default value.
	 * 
	 * @return the extra token pattern.
	 */
	Pattern getExtraToken();

	/**
	 * Gets the criterion token pattern. See {@link #CRITERION_TOKEN} for the default value.
	 * 
	 * @return the criterion token pattern.
	 */
	Pattern getCriterionToken();

	/**
	 * Gets the properties delimiter token pattern. See {@link #PROPERTIES_DELIMITER_TOKEN} for the default value.
	 * 
	 * @return the criterion token pattern.
	 */
	Pattern getPropertiesDelimiterToken();

	/**
	 * Gets the criterion delimiter token pattern. See {@link #CRITERION_DELIMITER_TOKEN} for the default value.
	 * 
	 * @return the criterion delimiter token pattern.
	 */
	Pattern getCriterionDelimiterToken();

	/**
	 * Gets the escape character. See {@link #ESCAPE_CHARACTER} for the default value.
	 * 
	 * @return the escape character.
	 */
	String getEscapeCharacter();

	/**
	 * Gets the map of property mappers configurations. The key is the filter property name, and the value is the corresponding PropertyMapperConfig.
	 * 
	 * @return the map of property mappers configurations.
	 */
	Map<String, PropertyMapperConfig<Q, ?>> getMappersConfigMap();

	/**
	 * Gets the query helpers map.
	 * 
	 * @return the query helpers map.
	 */
	Map<String, QueryTemplateConfig<Q>> getQueryHelpers();

	/**
	 * Gets the target reserved word for "where". See {@link #TARGET_RESERVED_WORD_WHERE} for the default value.
	 * 
	 * @return the target reserved word for "where".
	 */
	String getTargetReservedWordWhere();

	/**
	 * Gets the where token pattern. See {@link #WHERE_TOKEN} for the default value.
	 * 
	 * @return the target reserved word for "and".
	 */
	Pattern getWhereToken();

	/**
	 * Gets the properties token pattern. See {@link #PROPERTIES_TOKEN} for the default value.
	 * 
	 * @return the properties token pattern.
	 */
	Pattern getPropertiesToken();

	/**
	 * Gets the repeat token pattern. See {@link #REPEAT_TOKEN} for the default value.
	 * 
	 * @return the repeat token pattern.
	 */
	Pattern getRepeatToken();

	/**
	 * Gets the query helper token pattern. See {@link #QUERY_HELPER_TOKEN} for the default value.
	 * 
	 * @return the query helper token pattern.
	 */
	Pattern getQueryHelperToken();

	/**
	 * Gets the reserved word for `any` property markers. See {@link #RESERVED_ANY_PROPERTY} for the default value.
	 * 
	 * @return the reserved word for `any` parameter markers.
	 */
	String getReservedAnyProperty();

	/**
	 * Gets the target reserved word for "and". See {@link #TARGET_RESERVED_WORD_AND} for the default value.
	 * 
	 * @return the target reserved word for "and".
	 */
	String getTargetReservedWordAnd();

	/**
	 * Gets the target reserved word for "or". See {@link #TARGET_RESERVED_WORD_OR} for the default value.
	 * 
	 * @return the target reserved word for "or".
	 */
	String getTargetReservedWordOr();

	/**
	 * Gets the target reserved word for open parenthesis. See {@link #TARGET_RESERVED_WORD_OPEN_PARENTHESIS} for the default value.
	 * 
	 * @return the target reserved word for open parenthesis.
	 */
	String getTargetReservedWordOpenParenthesis();

	/**
	 * Gets the target reserved word for close parenthesis. See {@link #TARGET_RESERVED_WORD_CLOSE_PARENTHESIS} for the default value.
	 * 
	 * @return the target reserved word for close parenthesis.
	 */
	String getTargetReservedWordCloseParenthesis();

	/**
	 * Gets the target reserved word for positional parameter markers. See {@link #TARGET_RESERVED_WORD_POSITIONAL_PARAMETER_MARKER} for the default value.
	 * 
	 * @return the target reserved word for positional parameter markers.
	 */
	String getTargetReservedWordPositionalParameterMarker();

	/**
	 * Gets the separator marker for list items. See {@link #TARGET_ITEM_LIST_SEPARATOR_MARKER} for the default value.
	 * 
	 * @return the separator marker for list items.
	 */
	String getTargetItemListSeparatorMarker();

	/**
	 * Gets whether to convert named parameters to positional parameters. See {@link #convertNamedToPositionalParameters(boolean)} for more information.
	 * 
	 * @return whether to convert named parameters to positional parameters.
	 */
	boolean isConvertNamedToPositionalParameters();

	/**
	 * Gets the prefix used for parameter usage. See {@link #parameterUsagePrefix(String)} for more information.
	 * 
	 * @return the prefix used for parameter usage.
	 */
	String getParameterUsagePrefix();

	/**
	 * Gets the pattern used for parameter names. See {@link #parameterNamePattern(String)} for more information. 
	 * 
	 * @return the pattern used for parameter names.
	 */
	String getParameterNamePattern();

	/**
	 * Gets the base index for parameters. See {@link #parameterBasePosition(int)} for more information.
	 * 
	 * @return the base index for parameters.
	 */
	int getParameterBasePosition();

	/**
	 * Gets whether to compact the query text. See {@link #compactQueryText(boolean)} for more information.
	 * 
	 * @return whether to compact the query text.
	 */
	boolean isCompactQueryText();

	/**
	 * Gets the original query text before any processing. See {@link #getQueryTextOriginal()} for the processed query text.
	 * 
	 * @return the original query text.
	 */
	String getQueryTextOriginal();

	/**
	 * Creates a new instance of QueryTemplateConfig with the specified query text and query class.
	 * 
	 * @param <SQ> The type of the query. It is not used internally, it is only for strong typing and IDE code completion.
	 * @param queryText the query text.
	 * @param queryClass the class of the query. Internally it is not used, it is only for strong typing and IDE code completion.
	 * @return a new instance of QueryTemplateConfig.
	 */
	static <SQ> QueryTemplateConfig<SQ> of(String queryText, Class<SQ> queryClass) {
		return new QueryTemplateConfigRoot<SQ>(queryText);
	}
}