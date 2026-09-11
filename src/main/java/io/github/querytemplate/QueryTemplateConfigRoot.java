package io.github.querytemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @param <Q>
 */
public class QueryTemplateConfigRoot<Q> implements QueryTemplateConfig<Q> {

    private static final Logger LOG = LoggerFactory.getLogger(QueryTemplateConfigRoot.class);

    Pattern filtersToken = Pattern.compile(QueryTemplateConfig.FILTERS_TOKEN);
	Pattern whereToken = Pattern.compile(QueryTemplateConfig.WHERE_TOKEN);
	Pattern andToken = Pattern.compile(QueryTemplateConfig.AND_TOKEN);
	Pattern orToken = Pattern.compile(QueryTemplateConfig.OR_TOKEN);
	Pattern noOperatorToken = Pattern.compile(QueryTemplateConfig.NO_OPERATOR_TOKEN);
	Pattern openParenthesisToken = Pattern.compile(QueryTemplateConfig.OPEN_PARENTHESIS_TOKEN);
	Pattern closeParenthesisToken = Pattern.compile(QueryTemplateConfig.CLOSE_PARENTHESIS_TOKEN);
	Pattern extraToken = Pattern.compile(QueryTemplateConfig.EXTRA_TOKEN);
	Pattern paramToken = Pattern.compile(QueryTemplateConfig.PARAM_TOKEN);
	Pattern repeatToken = Pattern.compile(QueryTemplateConfig.REPEAT_TOKEN);
	Pattern criterionToken = Pattern.compile(QueryTemplateConfig.CRITERION_TOKEN);
	Pattern paramDelimiterToken = Pattern.compile(QueryTemplateConfig.PARAM_DELIMITER_TOKEN);
	Pattern criterionDelimiterToken = Pattern.compile(QueryTemplateConfig.CRITERION_DELIMITER_TOKEN);
	Pattern queryHelperToken = Pattern.compile(QueryTemplateConfig.QUERY_HELPER_TOKEN);
	String escapeCharacter = QueryTemplateConfig.ESCAPE_CHARACTER;
	String reservedAnyParam = QueryTemplateConfig.RESERVED_ANY_PARAM;
	String targetReservedWordWhere = QueryTemplateConfig.TARGET_RESERVED_WORD_WHERE;
	String targetReservedWordAnd = QueryTemplateConfig.TARGET_RESERVED_WORD_AND;
	String targetReservedWordOr = QueryTemplateConfig.TARGET_RESERVED_WORD_OR;
	String targetReservedWordOpenParenthesis = QueryTemplateConfig.TARGET_RESERVED_WORD_OPEN_PARENTHESIS;
	String targetReservedWordCloseParenthesis = QueryTemplateConfig.TARGET_RESERVED_WORD_CLOSE_PARENTHESIS;
	String targetReservedWordPositionalParameterMarker = QueryTemplateConfig.TARGET_RESERVED_WORD_POSITIONAL_PARAMETER_MARKER;
	String targetItemListSeparatorMarker = QueryTemplateConfig.TARGET_ITEM_LIST_SEPARATOR_MARKER;
	boolean convertNamedToPositionalParameters = false;
	String parameterUsagePrefix = ":";
	String parameterNamePattern = QueryTemplateConfig.PARAMETER_NAME_PATTERN;
	int parameterBasePosition = 1;
	boolean compactQueryText = false;

    /** QueryTemplate used as Helpers to assemble inner parts of the query. */
    private Map<String, QueryTemplateConfig<Q>> queryHelpers = new LinkedHashMap<>();
    
    private String queryTextOriginal;
    
	QueryTemplateConfigRoot(String queryText) {
		this.mappersConfigMap = new LinkedHashMap<>();
		this.queryTextOriginal = queryText;
    }
    
    private Map<String, QueryTemplateConfig.PropertyMapperConfig<Q, ?>> mappersConfigMap;
    
	@Override
	public <P> QueryTemplateConfig.PropertyMapperConfig<Q, P> addMapper(String filterPrp, Class<P> propertyClass) {
		QueryTemplateConfig.PropertyMapperConfig<Q, P> propertyMapperConfig = new QueryTemplateConfig.PropertyMapperConfig<>(this);
		this.mappersConfigMap.put(filterPrp, propertyMapperConfig);
		return propertyMapperConfig.filterPrp(filterPrp);
	}
    
	@Override
	public QueryTemplateConfig<Q> removeMapper(String filterPrp) {
		this.mappersConfigMap.remove(filterPrp);
		return this;
	}
	
	@Override
	public <P> PropertyMapperConfig<Q, P> modifyMapper(String filterPrp, Class<P> propertyClass) {
		return (PropertyMapperConfig<Q, P>) this.mappersConfigMap.get(filterPrp);
	}
	
    /**
     * Adds another {@link QueryTemplateConfigRoot} inside the current instance. Inside the
     * marked query there is something like:
     *
     * <pre>
     * [Q:RelArrolamento.DetalheHelper]
     * </pre>
     *
     * @param key         the query helper key.
     * @param queryTextOriginal the query helper.
     * @return this instance.
     */
    @Override
	public QueryTemplateConfig<Q> addQueryHelper(String key, String queryTextOriginal) {
        if (queryTextOriginal == null) {
            throw new QueryTemplateException("'queryHelper' cannot be null: '" + key + "'");
        }
    	QueryTemplateConfig<Q> helperConfig = new QueryTemplateConfigQueryHelper<>(queryTextOriginal, this);
        if (!this.queryHelpers.containsKey(key)) {
            this.queryHelpers.put(key, helperConfig);
        } else {
            throw new QueryTemplateException("Query Helper already added before. key: '" + key + "'");
        }

        return this;
    }

    /**
     * 
     * @param targetReservedWordPositionalParameterMarker the target reserved word for positional parameter markers. Default is "?".
     * @return
     */
	@Override
	public QueryTemplateConfig<Q> targetReservedWordPositionalParameterMarker(String targetReservedWordPositionalParameterMarker) {
		this.targetReservedWordPositionalParameterMarker = targetReservedWordPositionalParameterMarker;
		return this;
	}

	/**
	 * @param convertNamedToPositionalParameters whether to convert named parameters to positional parameters.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> convertNamedToPositionalParameters(boolean convertNamedToPositionalParameters) {
		this.convertNamedToPositionalParameters = convertNamedToPositionalParameters;
		return this;
	}

	/**
	 * @param targetReservedWordWhere see {@link QueryTemplateConfig#TARGET_RESERVED_WORD_WHERE}.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> targetReservedWordWhere(String targetReservedWordWhere) {
		this.targetReservedWordWhere = targetReservedWordWhere;
		return this;
	}

	/**
	 * @param targetReservedWordAnd see {@link QueryTemplateConfig#TARGET_RESERVED_WORD_AND}.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> targetReservedWordAnd(String targetReservedWordAnd) {
		this.targetReservedWordAnd = targetReservedWordAnd;
		return this;
	}

	/**
	 * @param targetReservedWordOr see {@link QueryTemplateConfig#TARGET_RESERVED_WORD_OR}.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> targetReservedWordOr(String targetReservedWordOr) {
		this.targetReservedWordOr = targetReservedWordOr;
		return this;
	}

	/**
	 * @param targetReservedWordOpenParenthesis see {@link QueryTemplateConfig#TARGET_RESERVED_WORD_OPEN_PARENTHESIS}.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> targetReservedWordOpenParenthesis(String targetReservedWordOpenParenthesis) {
		this.targetReservedWordOpenParenthesis = targetReservedWordOpenParenthesis;
		return this;
	}

	/**
	 * @param targetReservedWordCloseParenthesis see {@link QueryTemplateConfig#TARGET_RESERVED_WORD_CLOSE_PARENTHESIS}.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> targetReservedWordCloseParenthesis(String targetReservedWordCloseParenthesis) {
		this.targetReservedWordCloseParenthesis = targetReservedWordCloseParenthesis;
		return this;
	}
	
	/**
	 * @param targetItemListSeparatorMarker the separator marker for list items.
	 *                                      Default is {@link QueryTemplateConfig#TARGET_ITEM_LIST_SEPARATOR_MARKER}.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> targetItemListSeparatorMarker(String targetItemListSeparatorMarker) {
		this.targetItemListSeparatorMarker = targetItemListSeparatorMarker;
		return this;
	}

	/**
	 * @param parameterUsagePrefix the prefix used for parameter usage. Default is ":".
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> parameterUsagePrefix(String parameterUsagePrefix) {
		this.parameterUsagePrefix = parameterUsagePrefix;
		return this;
	}

	/**
	 * @param parameterNamePattern see {@link QueryTemplateConfig#PARAMETER_NAME_PATTERN}.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> parameterNamePattern(String parameterNamePattern) {
		this.parameterNamePattern = parameterNamePattern;
		return this;
	}
	
	/**
	 * @param parameterBasePosition the base index for parameters. Default is 1.
	 * @return
	 */
	@Override
	public QueryTemplateConfig<Q> parameterBasePosition(int parameterBasePosition) {
		this.parameterBasePosition = parameterBasePosition;
		return this;
	}

	/**
	 * Sets the filters token pattern. See {@link QueryTemplateConfig#FILTERS_TOKEN} for the default value.
	 * 
	 * @param filtersToken the filters token pattern.
	 * @return this instance for method chaining.
	 */
	@Override
	public QueryTemplateConfig<Q> filtersToken(String filtersToken) {
		this.filtersToken = Pattern.compile(filtersToken);
		return this;
	}
	
	/**
	 * Sets the where token pattern. See {@link QueryTemplateConfig#WHERE_TOKEN} for the default value.
	 * 
	 * @param whereToken the where token pattern.
	 * @return this instance for method chaining.
	 */
	@Override
	public QueryTemplateConfig<Q> whereToken(String whereToken) {
		this.whereToken = Pattern.compile(whereToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> andToken(String andToken) {
		this.andToken = Pattern.compile(andToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> orToken(String orToken) {
		this.orToken = Pattern.compile(orToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> noOperatorToken(String noOperatorToken) {
		this.noOperatorToken = Pattern.compile(noOperatorToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> openParenthesisToken(String openParenthesisToken) {
		this.openParenthesisToken = Pattern.compile(openParenthesisToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> closeParenthesisToken(String closeParenthesisToken) {
		this.closeParenthesisToken = Pattern.compile(closeParenthesisToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> extraToken(String extraToken) {
		this.extraToken = Pattern.compile(extraToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> criterionToken(String criterionToken) {
		this.criterionToken = Pattern.compile(criterionToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> paramDelimiterToken(String paramDelimiterToken) {
		this.paramDelimiterToken = Pattern.compile(paramDelimiterToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> criterionDelimiterToken(String criterionDelimiterToken) {
		this.criterionDelimiterToken = Pattern.compile(criterionDelimiterToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> escapeCharacter(String escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> compactQueryText(boolean compactQueryText) {
		this.compactQueryText = compactQueryText;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q>  clearMappers() {
		this.mappersConfigMap.clear();
		return this;
	}
	
	@Override
	public Pattern getFiltersToken() {
		return filtersToken;
	}

	@Override
	public Pattern getAndToken() {
		return andToken;
	}

	@Override
	public Pattern getOrToken() {
		return orToken;
	}

	@Override
	public Pattern getNoOperatorToken() {
		return noOperatorToken;
	}

	@Override
	public Pattern getOpenParenthesisToken() {
		return openParenthesisToken;
	}

	@Override
	public Pattern getCloseParenthesisToken() {
		return closeParenthesisToken;
	}

	@Override
	public Pattern getExtraToken() {
		return extraToken;
	}

	@Override
	public Pattern getCriterionToken() {
		return criterionToken;
	}

	@Override
	public Pattern getParamDelimiterToken() {
		return paramDelimiterToken;
	}

	@Override
	public Pattern getCriterionDelimiterToken() {
		return criterionDelimiterToken;
	}

	@Override
	public String getEscapeCharacter() {
		return escapeCharacter;
	}

	@Override
	public Map<String, QueryTemplateConfig.PropertyMapperConfig<Q, ?>> getMappersConfigMap() {
		return mappersConfigMap;
	}

	@Override
	public Map<String, QueryTemplateConfig<Q>> getQueryHelpers() {
		return Collections.unmodifiableMap(this.queryHelpers);
	}

	@Override
	public String getTargetReservedWordWhere() {
		return targetReservedWordWhere;
	}

	@Override
	public Pattern getWhereToken() {
		return whereToken;
	}

	@Override
	public Pattern getParamToken() {
		return paramToken;
	}

	@Override
	public Pattern getRepeatToken() {
		return repeatToken;
	}

	@Override
	public Pattern getQueryHelperToken() {
		return queryHelperToken;
	}

	@Override
	public String getReservedAnyParam() {
		return reservedAnyParam;
	}

	@Override
	public String getTargetReservedWordAnd() {
		return targetReservedWordAnd;
	}

	@Override
	public String getTargetReservedWordOr() {
		return targetReservedWordOr;
	}

	@Override
	public String getTargetReservedWordOpenParenthesis() {
		return targetReservedWordOpenParenthesis;
	}

	@Override
	public String getTargetReservedWordCloseParenthesis() {
		return targetReservedWordCloseParenthesis;
	}

	@Override
	public String getTargetReservedWordPositionalParameterMarker() {
		return targetReservedWordPositionalParameterMarker;
	}

	@Override
	public String getTargetItemListSeparatorMarker() {
		return targetItemListSeparatorMarker;
	}

	@Override
	public boolean isConvertNamedToPositionalParameters() {
		return convertNamedToPositionalParameters;
	}

	@Override
	public String getParameterUsagePrefix() {
		return parameterUsagePrefix;
	}

	@Override
	public String getParameterNamePattern() {
		return parameterNamePattern;
	}

	@Override
	public int getParameterBasePosition() {
		return parameterBasePosition;
	}

	@Override
	public boolean isCompactQueryText() {
		return compactQueryText;
	}

	@Override
	public String getQueryTextOriginal() {
		return queryTextOriginal;
	}

	@Override
	public QueryTemplateConfig<Q> getParent() {
		return null;
	}
	
//	this.filtersToken = Pattern.compile(filtersTokenParam);
//    this.whereToken = Pattern.compile(whereTokenParam);
//    this.andToken = Pattern.compile(andTokenParam);
//    this.orToken = Pattern.compile(orTokenParam);
//    this.noOperatorToken = Pattern.compile(noOperatorTokenParam);
//    this.openParenthesisToken = Pattern.compile(openParenthesisTokenParam);
//    this.closeParenthesisToken = Pattern.compile(closeParenthesisTokenParam);
//    this.extraToken = Pattern.compile(extraTokenParam);
//    this.paramToken = Pattern.compile(paramTokenParam);
//    this.repeatToken = Pattern.compile(repeatTokenParam);
//    this.criterionToken = Pattern.compile(criterionTokenParam);
//    this.paramDelimiterToken = Pattern.compile(paramDelimiterTokenParam);
//    this.criterionDelimiterToken = Pattern.compile(criterionDelimiterTokenParam);
//    this.queryHelperToken = Pattern.compile(queryHelperTokenParam);
//    this.escapeCharacter = escapeCharacterParam;
//    this.reservedAnyParam = reservedAnyParamParam;
//    this.sqlHqlQueryOriginal = sqlHqlQuery;
//    
//    this.compactSqlHqlQuery = compactSqlHqlQueryParam;
}
