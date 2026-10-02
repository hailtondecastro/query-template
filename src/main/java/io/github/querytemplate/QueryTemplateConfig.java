package io.github.querytemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.github.querytemplate.proxy.ProxyFactoryCreator;

/**
 * Configuration interface for QueryTemplate. It allows to set the tokens and reserved words used in the query template, as well as to add property mappers and query helpers.
 * @param <Q> Plataform-specific query type (e.g., String for SQL, CriteriaQuery for JPA, etc.).
 * It is not used internally, it is only for strong typing and IDE code completion.
 * @param <F> Filter type. It is used internally to create proxy objects and resolve properties by lambda expressions, used too for strong typing and IDE code completion.
 */
public interface QueryTemplateConfig<Q, F> {

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
	 * Use the reserved word '$any$' at the beginning of the properties list to
	 * consider the criterion eligible for inclusion in the query if any of the properties is participating.<br> 
	 * Value: <code>"\\$any\\$"</code>.
	 */
	String RESERVED_ANY_PROPERTY = "\\$any\\$";
	
	/**
	 * Use the reserved word '$eval$' at the beginning of the properties
	 * to consider the criterion eligible for inclusion in the query in case the expression return <code>true</code>.<br>
	 * The expression can use the filter properties values as inner fields of <code>prpValues</code> or <code>pv</code> script variable, like <code>prpValues.propertyName</code>.<br>
	 * The the result of `participation tests` can be used as boolean inner fields of <code>prpParticipations</code> or <code>pp</code> script variable, like <code>prpParticipation.propertyName</code>.<br>  
	 * The expression will be evaluated using the {@link EvalRunner} provided by the user on {@link QueryTemplateConfig#evalRunnerCreator(Function)}, so the language depends on the {@link EvalRunner} provided, so, the language can be whatever you prefer, javascript, pyton, bean shell, etc.<br> 
	 * If no EvalRunner is provided, an exception will be thrown when the expression is evaluated as <code>true</code>.<br>
	 * Value: <code>"\\$eval\\$"</code>.
	 */
	String RESERVED_EVAL_PROPERTY = "\\$eval\\$";
	
	/**
	 * Token with the names of the properties separated by
	 * comma, surrounded by the parameter delimiters. All the properties must be
	 * participating in the query (or not participating if preceded by "!")
	 * for the criterion to be included.<br>
	 * Value: <code>"(\\[(!?\\s*\\b[\\w-]+\\b\\s*,?\\s*){1,300}\\])|(\\[\\s{0,300}%s[^\\]]{0,300}\\])|(\\[\\s{0,300}%s[^\\]]{0,300}\\])"</code>.<br>
	 * The value of {@link #getPropertiesToken()} is <code>Pattern.compile(String.format(QueryTemplateConfig.PROPERTIES_TOKEN, this.getReservedAnyProperty(), this.getReservedEvalProperty()))</code> 
	 */
	String PROPERTIES_TOKEN = "(\\[(!?\\s*\\b[\\w-]+\\b\\s*,?\\s*){1,300}\\])|(\\[\\s{0,300}%s[^\\]]{0,300}\\])|(\\[\\s{0,300}%s[^\\]]{0,300}\\])";
	
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
	 * @param <P> The type of the property to be mapped or the type of collection item in case of a repeatable or unpacked parameter property.
	 * @param <I> The type of the items in the collection to be mapped.
	 * @param filterPrp the filter property name.
	 * @param propertyClass the class of the property. Internally it used to create proxy objects and resolve properties by lambda expressions.
	 * @return the added mapper configuration.
	 */
	<P, I> PropertyMapperConfig<Q, F, P, I> addMapper(String filterPrp,
		Class<P> propertyClass);

	/**
	 * Adds a property mapper configuration for the specified filter property using a lambda expression to resolve the property.
	 * @param <P> The type of the property to be mapped.
	 * @param <I> The type of the items in the collection to be mapped.
	 * @param filterPrp the filter property name.
	 * @return the added mapper configuration.
	 */
	<P, I> PropertyMapperConfig<Q, F, P, I> addMapper(Function<F, P> filterPrp);
	
	/**
	 * Adds a property mapper configuration for the specified filter property using a lambda expression to resolve the property that returns a collection.<br>
	 * This must be used in case of a repeatable or unpacked parameter property, like a List, Set or Array.<br>
	 * Adding a property by here you will be able to use {@link PropertyMapperConfig#switchType()}.<br>
	 * This affects java syntax checking for {@link PropertyMapperConfig#onParticipatesNamed(AssignNamedParameter)} and {@link PropertyMapperConfig#onParticipatesPositional(AssignPositionalParameter)} 
	 * methods, which will be called for each item in the collection.
	 * @param <C> The type of the collection to be mapped (List, Set, etc.).
	 * @param <I> The type of the items in the collection to be mapped.
	 * @param filterPrp the filter property name.
	 * @return the added mapper configuration.
	 */
	<C extends Collection<I>, I> PropertyMapperConfig<Q, F, C, I> addMapperC(Function<F, Collection<I>> filterPrp);
	
	/**
	 * Similar to {@link #addMapperC(Function)}.
	 * @param <C> The type of the collection to be mapped (List, Set, etc.).
	 * @param <I> The type of the items in the collection to be mapped.
	 * @param filterPrp the filter property name. 
	 * @return the added mapper configuration.
	 */
	<C extends List<I>, I> PropertyMapperConfig<Q, F, C, I> addMapperL(Function<F, Collection<I>> filterPrp);
	
	/**
	 * Similar to {@link #addMapperC(Function)}.
	 * @param <I> The type of the items in the collection to be mapped.
	 * @param filterPrp the filter property name. 
	 * @return the added mapper configuration.
	 */
	<I> PropertyMapperConfig<Q, F, I[], I> addMapperA(Function<F, I[]> filterPrp);
	
//	<P, I> PropertyMapperConfig<Q, F, P, I> addMapperL(Function<F, List<P>> filterPrp);
//	<P, I> PropertyMapperConfig<Q, F, P, I> addMapperA(Function<F, P[]> filterPrp);
	
	/**
	 * Removes the mapper configuration for the specified filter property.
	 * 
	 * @param filterPrp the filter property name.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> removeMapper(String filterPrp);
	
	/**
	 * Removes the mapper configuration for the specified filter property using a
	 * lambda expression to resolve the property.
	 * 
	 * @param <P>       The type of the property to be mapped.
	 * @param filterPrp the filter property name.
	 * @return This instance for method chaining.
	 */
	<P> QueryTemplateConfig<Q, F> removeMapper(Function<F, P> filterPrp);
	
	/**
	 * Modifies the mapper configuration for the specified filter property.
	 * 
	 * @param filterPrp the filter property name.
	 * @param propertyClass the class of the property. Internally it used to create proxy objects and resolve properties by lambda expressions. 
	 * @param <P> The type of the property to be mapped or the type of collection item in case of a repeatable or unpacked parameter property.
	 * @param <I> The type of the items in the collection to be mapped.
	 * @return This instance for method chaining.
	 * 
	 */
	<P, I> PropertyMapperConfig<Q, F, P, I> modifyMapper(String filterPrp, Class<P> propertyClass);
	
	/**
	 * Modifies the mapper configuration for the specified filter property using a
	 * lambda expression to resolve the property.
	 * 
	 * @param <P>       The type of the property to be mapped.
	 * @param <I>       The type of the items in the collection to be mapped.
	 * @param filterPrp the filter property name.
	 * @return This instance for method chaining.
	 */
	<P, I> PropertyMapperConfig<Q, F, P, I> modifyMapper(Function<F, P> filterPrp);
	
	/**
	 * Modify a property mapper configuration for the specified filter property using a lambda expression to resolve the property that returns a collection.<br>
	 * This must be used in case of a repeatable or unpacked parameter property, like a List, Set or Array.<br>
	 * Adding a property by here you will be able to use {@link PropertyMapperConfig#switchType()}.<br>
	 * This affects java syntax checking for {@link PropertyMapperConfig#onParticipatesNamed(AssignNamedParameter)} and {@link PropertyMapperConfig#onParticipatesPositional(AssignPositionalParameter)} 
	 * methods, which will be called for each item in the collection.
	 * @param <C> The type of the collection to be mapped (List, Set, etc.).
	 * @param <I> The type of the items in the collection to be mapped.
	 * @param filterPrp the filter property name.
	 * @return This instance for method chaining.
	 */
	<C extends Collection<I>, I> PropertyMapperConfig<Q, F, C, I> modifyMapperC(Function<F, Collection<I>> filterPrp);
	
	/**
	 * Similar to {@link #modifyMapperC(Function)}.
	 * 
	 * @param <C>       The type of the collection to be mapped (List, Set, etc.).
	 * @param <I>       The type of the items in the collection to be mapped.
	 * @param filterPrp Filter property name resolver function.
	 * @return The modified mapper configuration.
	 */
	<C extends List<I>, I> PropertyMapperConfig<Q, F, C, I> modifyMapperL(Function<F, Collection<I>> filterPrp);

	/**
	 * Similar to {@link #modifyMapperC(Function)}.
	 * 
	 * @param <I>       The type of the items in the collection to be mapped.
	 * @param filterPrp Filter property name resolver function.
	 * @return The modified mapper configuration.
	 */
	<I> PropertyMapperConfig<Q, F, I[], I> modifyMapperA(Function<F, I[]> filterPrp);
	
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
	QueryTemplateConfig<Q, F> addQueryHelper(String key,
		String queryText);

	/**
	 * Removes a query helper from the current instance.
	 * 
	 * @param targetReservedWordPositionalParameterMarker the target reserved word for positional parameter markers. Default is "?".
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> targetReservedWordPositionalParameterMarker(
		String targetReservedWordPositionalParameterMarker);

	/**
	 * Sets whether to convert named parameters to positional parameters. Default is false.
	 * 
	 * @param convertNamedToPositionalParameters whether to convert named parameters to positional parameters.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> convertNamedToPositionalParameters(boolean convertNamedToPositionalParameters);

	/**
	 * Sets the target reserved word for "where". Default is "where".
	 * 
	 * @param targetReservedWordWhere see {@link #TARGET_RESERVED_WORD_WHERE}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> targetReservedWordWhere(String targetReservedWordWhere);

	/**
	 * Sets the target reserved word for "and". Default is "and".
	 * 
	 * @param targetReservedWordAnd see {@link #TARGET_RESERVED_WORD_AND}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> targetReservedWordAnd(String targetReservedWordAnd);

	/**
	 * Sets the target reserved word for "or". Default is "or".
	 * 
	 * @param targetReservedWordOr see {@link #TARGET_RESERVED_WORD_OR}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> targetReservedWordOr(String targetReservedWordOr);

	/**
	 * Sets the target reserved word for open parenthesis. Default is "(".
	 * 
	 * @param targetReservedWordOpenParenthesis see {@link #TARGET_RESERVED_WORD_OPEN_PARENTHESIS}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> targetReservedWordOpenParenthesis(String targetReservedWordOpenParenthesis);

	/**
	 * Sets the target reserved word for close parenthesis. Default is ")".
	 * 
	 * @param targetReservedWordCloseParenthesis see {@link #TARGET_RESERVED_WORD_CLOSE_PARENTHESIS}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> targetReservedWordCloseParenthesis(String targetReservedWordCloseParenthesis);

	/**
	 * Sets the target reserved word for positional parameter markers. Default is "?".
	 * 
	 * @param targetItemListSeparatorMarker the separator marker for list items.
	 *                                      Default is {@link #TARGET_ITEM_LIST_SEPARATOR_MARKER}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> targetItemListSeparatorMarker(String targetItemListSeparatorMarker);

	/**
	 * Sets the prefix used for parameter usage. Default is ":".
	 * 
	 * @param parameterUsagePrefix the prefix used for parameter usage. Default is ":".
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> parameterUsagePrefix(String parameterUsagePrefix);

	/**
	 * Sets the pattern used for parameter names. Default is "\\b([\\w-]+)\\b".
	 * 
	 * @param parameterNamePattern see {@link #PARAMETER_NAME_PATTERN}.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> parameterNamePattern(String parameterNamePattern);

	/**
	 * Sets the base index for parameters. Default is 1.
	 * 
	 * @param parameterBasePosition the base index for parameters. Default is 1.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> parameterBasePosition(int parameterBasePosition);

	/**
	 * Sets the filters token pattern. See {@link #FILTERS_TOKEN} for the default value.
	 * 
	 * @param filtersToken the filters token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> filtersToken(String filtersToken);

	/**
	 * Sets the where token pattern. See {@link #WHERE_TOKEN} for the default value.
	 * 
	 * @param whereToken the where token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> whereToken(String whereToken);

	/**
	 * Sets the properties token pattern. See {@link #PROPERTIES_TOKEN} for the
	 * default value.
	 * 
	 * @param propertiesToken the properties token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> propertiesToken(String propertiesToken);
	
	/**
	 * Sets the repeat token pattern. See {@link #REPEAT_TOKEN} for the default
	 * value.
	 * 
	 * @param repeatToken the repeat token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> repeatToken(String repeatToken);
	
	/**
	 * Sets the query helper token pattern. See {@link #QUERY_HELPER_TOKEN} for the
	 * default value.
	 * 
	 * @param queryHelperToken the query helper token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> queryHelperToken(String queryHelperToken);
	
	
	/**
	 * Sets the and token pattern. See {@link #AND_TOKEN} for the default value.
	 * 
	 * @param andToken the and token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> andToken(String andToken);

	/**
	 * Sets the or token pattern. See {@link #OR_TOKEN} for the default value.
	 * @param orToken the or token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> orToken(String orToken);

	/**
	 * Sets the no operator token pattern. See {@link #NO_OPERATOR_TOKEN} for the
	 * default value.
	 * 
	 * @param noOperatorToken the no operator token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> noOperatorToken(String noOperatorToken);

	/**
	 * Sets the open parenthesis token pattern. See {@link #OPEN_PARENTHESIS_TOKEN}
	 * for the default value.
	 * 
	 * @param openParenthesisToken the open parenthesis token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> openParenthesisToken(String openParenthesisToken);

	/**
	 * Sets the close parenthesis token pattern. See
	 * {@link #CLOSE_PARENTHESIS_TOKEN} for the default value.
	 * 
	 * @param closeParenthesisToken the close parenthesis token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> closeParenthesisToken(String closeParenthesisToken);

	/**
	 * Sets the extra token pattern. See {@link #EXTRA_TOKEN} for the default value.
	 * 
	 * @param extraToken the extra token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> extraToken(String extraToken);

	/**
	 * Sets the criterion token pattern. See {@link #CRITERION_TOKEN} for the
	 * default value.
	 * 
	 * @param criterionToken the criterion token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> criterionToken(String criterionToken);
	
	/**
	 * Sets the parameter delimiter token pattern. See
	 * {@link #PROPERTIES_DELIMITER_TOKEN} for the default value.
	 * 
	 * @param propertiesDelimiterToken the parameter delimiter token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> propertiesDelimiterToken(String propertiesDelimiterToken);

	/**
	 * Sets the criterion delimiter token pattern. See
	 * {@link #CRITERION_DELIMITER_TOKEN} for the default value.
	 * 
	 * @param criterionDelimiterToken the criterion delimiter token pattern.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> criterionDelimiterToken(String criterionDelimiterToken);

	/**
	 * Sets the escape character. See {@link #ESCAPE_CHARACTER} for the default
	 * value.
	 * 
	 * @param escapeCharacter the escape character.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> escapeCharacter(String escapeCharacter);
	
	/**
	 * Sets the reserved word for `any` property markers. See
	 * {@link #RESERVED_ANY_PROPERTY} for the default value.
	 * 
	 * @param reservedAnyProperty the reserved word for `any` parameter markers.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> reservedAnyProperty(String reservedAnyProperty);
	
	/**
	 * Sets the reserved word for `eval` property markers. See
	 * {@link #RESERVED_EVAL_PROPERTY} for the default value.
	 * 
	 * @param reservedEvalProperty the reserved word for `eval` parameter markers.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> reservedEvalProperty(String reservedEvalProperty);

	/**
	 * Sets whether to compact the query text. If true, the query text will be
	 * compacted by removing extra spaces and line breaks.
	 * 
	 * @param compactQueryText whether to compact the query text.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> compactQueryText(boolean compactQueryText);

	/**
	 * Clears all property mappers from the configuration.
	 * 
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> clearMappers();
	
	/**
	 * Sets the EvalRunner creator function. This function is used to create an EvalRunner instance based on the QueryTemplateState.
	 * @param evalRunnerCreator the EvalRunner creator function.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> evalRunnerCreator(Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator);
	
	/**
	 * Sets the ProxyFactoryCreator instance. This instance is used to create proxy
	 * objects for the filter type.<br>
	 * See {@link #proxyFactoryCreator(ProxyFactoryCreator)} for more details.
	 * 
	 * @param proxyFactoryCreator the ProxyFactoryCreator instance.
	 * @return This instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> proxyFactoryCreator(ProxyFactoryCreator proxyFactoryCreator);
	
	/**
	 * Gets the parent configuration after defining a QueryHelper. If this is the root configuration, returns null.
	 * 
	 * @return Return for the parent config after define QueryHelper. If this is the root config, return null.
	 */
	QueryTemplateConfig<Q, F> getParent();

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
	Map<String, PropertyMapperConfig<Q, F, ?, ?>> getMappersConfig();

	/**
	 * Gets the query helpers map.
	 * 
	 * @return the query helpers map.
	 */
	Map<String, QueryTemplateConfig<Q, F>> getQueryHelpers();

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
	 * Gets the reserved word for `eval` property markers. See
	 * {@link #RESERVED_EVAL_PROPERTY} for the default value.
	 * 
	 * @return the reserved word for `eval` parameter markers.
	 */
	String getReservedEvalProperty();

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
	 * Gets the EvalRunner creator function. This function is used to create an
	 * EvalRunner instance based on the QueryTemplateState. It means that the 
	 * EvalRunner will be created for each evaluation of the query template, 
	 * allowing to use the current state of the query template to create the EvalRunner.
	 * 
	 * @return the EvalRunner creator function.
	 */
	Function<QueryTemplateState<?, ?>, EvalRunner> getEvalRunnerCreator();
	
	/**
	 * Gets the ProxyFactoryCreator instance. This instance is used to create proxy
	 * objects for the filter class, allowing to resolve properties by lambda
	 * expressions.<br>
	 * Its can be implemented using javassit, cglib, jdk dynamic proxy, etc. 
	 * There is no default implementation to keep the library light and flexible.
	 * 
	 * @return the ProxyFactoryCreator instance.
	 */
	ProxyFactoryCreator getProxyFactoryCreator();
	
	/**
	 * Creates a new instance of QueryTemplateConfig with the specified query text and query class.
	 * 
	 * @param <SQ> The type of the query. It is not used internally, it is only for strong typing and IDE code completion.
	 * @param <SF> The type of the filter. It is used internally to create proxy objects and resolve properties by lambda expressions, it is only for strong typing and IDE code completion.
	 * @param queryText the query text.
	 * @param queryClass the class of the query. Internally it is not used, it is only for strong typing and IDE code completion.
	 * @param filterClass the class of the filter. It is used internally to create proxy objects and resolve properties by lambda expressions, used too for strong typing and IDE code completion.
	 * @return a new instance of QueryTemplateConfig.
	 */
	static <SQ, SF> QueryTemplateConfig<SQ, SF> of(String queryText, Class<SQ> queryClass, Class<SF> filterClass) {
		return new QueryTemplateConfigRoot<SQ, SF>(queryText, filterClass);
	}
}