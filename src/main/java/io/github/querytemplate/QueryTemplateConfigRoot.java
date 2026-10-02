package io.github.querytemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.querytemplate.proxy.LambdaFieldNameDiscovery;
import io.github.querytemplate.proxy.LambdaFieldNameDiscovery.CallChainInfo;
import io.github.querytemplate.proxy.LambdaFieldNameDiscovery.CallInfo;
import io.github.querytemplate.proxy.ProxyFactory;
import io.github.querytemplate.proxy.ProxyFactoryCreator;

/**
 * Root configuration for the QueryTemplate. It holds the configuration for 
 * the query template, including the tokens used in the query, the mappers 
 * for the filter properties, and any query helpers.
 */
public class QueryTemplateConfigRoot<Q, F> implements QueryTemplateConfig<Q, F> {

    @SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(QueryTemplateConfigRoot.class);

    Pattern filtersToken = Pattern.compile(QueryTemplateConfig.FILTERS_TOKEN);
	Pattern whereToken = Pattern.compile(QueryTemplateConfig.WHERE_TOKEN);
	Pattern andToken = Pattern.compile(QueryTemplateConfig.AND_TOKEN);
	Pattern orToken = Pattern.compile(QueryTemplateConfig.OR_TOKEN);
	Pattern noOperatorToken = Pattern.compile(QueryTemplateConfig.NO_OPERATOR_TOKEN);
	Pattern openParenthesisToken = Pattern.compile(QueryTemplateConfig.OPEN_PARENTHESIS_TOKEN);
	Pattern closeParenthesisToken = Pattern.compile(QueryTemplateConfig.CLOSE_PARENTHESIS_TOKEN);
	Pattern extraToken = Pattern.compile(QueryTemplateConfig.EXTRA_TOKEN);
	String reservedAnyProperty = QueryTemplateConfig.RESERVED_ANY_PROPERTY;
	String reservedEvalProperty = QueryTemplateConfig.RESERVED_EVAL_PROPERTY;
	Pattern propertiesToken = Pattern.compile(String.format(QueryTemplateConfig.PROPERTIES_TOKEN, this.getReservedAnyProperty(), this.getReservedEvalProperty()));
	Pattern repeatToken = Pattern.compile(QueryTemplateConfig.REPEAT_TOKEN);
	Pattern criterionToken = Pattern.compile(QueryTemplateConfig.CRITERION_TOKEN);
	Pattern propertiesDelimiterToken = Pattern.compile(QueryTemplateConfig.PROPERTIES_DELIMITER_TOKEN);
	Pattern criterionDelimiterToken = Pattern.compile(QueryTemplateConfig.CRITERION_DELIMITER_TOKEN);
	Pattern queryHelperToken = Pattern.compile(QueryTemplateConfig.QUERY_HELPER_TOKEN);
	String escapeCharacter = QueryTemplateConfig.ESCAPE_CHARACTER;
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
    private Map<String, QueryTemplateConfig<Q, F>> queryHelpers = new LinkedHashMap<>();
    
    private String queryTextOriginal;
    
    private Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator;
    private ProxyFactoryCreator proxyFactoryCreator;
    private Class<F> filterClass;
    
	QueryTemplateConfigRoot(String queryText, Class<F> filterClass) {
		this.filterClass = filterClass;
		this.mappersConfigMap = new LinkedHashMap<>();
		this.queryTextOriginal = queryText;
    }
    
    private Map<String, PropertyMapperConfig<Q, F, ?, ?>> mappersConfigMap;
    
	@Override
	public <P, I> PropertyMapperConfig<Q, F, P, I> addMapper(String filterPrp, Class<P> propertyClass) {
		PropertyMapperConfig<Q, F, P, I> propertyMapperConfig = new PropertyMapperConfigDefault<>(this);
		this.mappersConfigMap.put(filterPrp, propertyMapperConfig);
		return propertyMapperConfig.filterPrp(filterPrp);
	}
	
	@Override
	public <P, I> PropertyMapperConfig<Q, F, P, I> addMapper(Function<F, P> filterPrp) {
		if (this.proxyFactoryCreator == null) {
            throw new QueryTemplateException("ProxyFactoryCreator is not set. Please set it using 'proxyFactoryCreator' method.");
        }
		ProxyFactory proxyFactory = this.proxyFactoryCreator.apply(this, this.filterClass);
		CallChainInfo callChainInfo = LambdaFieldNameDiscovery.fieldByGetMethodChainInfo(proxyFactory, filterPrp, this.filterClass);
		if (callChainInfo.getCallInfos().size() > 1) {
			throw new QueryTemplateException("Only one level of property mapping is allowed. Please use a single property reference.");
		}
		CallInfo callInfo = callChainInfo.getCallInfos().get(0);
		return (PropertyMapperConfig<Q, F, P, I>) this.addMapper(callInfo.getFieldName(), callInfo.getMethod().getReturnType());
	}
	
	@Override
	public <C extends Collection<I>, I> PropertyMapperConfig<Q, F, C, I> addMapperC(Function<F, Collection<I>> filterPrp) {
		return (PropertyMapperConfig<Q, F, C, I>) this.addMapper(filterPrp);
	}
	
	@Override
	public <C extends List<I>, I> PropertyMapperConfig<Q, F, C, I> addMapperL(Function<F, Collection<I>> filterPrp) {
		return (PropertyMapperConfig<Q, F, C, I>) (Object)this.addMapper(filterPrp);
	}
    
	@Override
	public <I> PropertyMapperConfig<Q, F, I[], I> addMapperA(Function<F, I[]> filterPrp) {
		return this.addMapper(filterPrp);
	}
	
	@Override
	public QueryTemplateConfig<Q, F> removeMapper(String filterPrp) {
		this.mappersConfigMap.remove(filterPrp);
		return this;
	}
	
	@Override
	public <P> QueryTemplateConfig<Q, F> removeMapper(Function<F, P> filterPrp) {
		if (this.proxyFactoryCreator == null) {
            throw new QueryTemplateException("ProxyFactoryCreator is not set. Please set it using 'proxyFactoryCreator' method.");
        }
		ProxyFactory proxyFactory = this.proxyFactoryCreator.apply(this, this.filterClass);
		CallChainInfo callChainInfo = LambdaFieldNameDiscovery.fieldByGetMethodChainInfo(proxyFactory, filterPrp, this.filterClass);
		if (callChainInfo.getCallInfos().size() > 1) {
			throw new QueryTemplateException("Only one level of property mapping is allowed. Please use a single property reference.");
		}
		CallInfo callInfo = callChainInfo.getCallInfos().get(0);
		return this.removeMapper(callInfo.getFieldName());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <P, I> PropertyMapperConfig<Q, F, P, I> modifyMapper(String filterPrp, Class<P> propertyClass) {
		return (PropertyMapperConfig<Q, F, P, I>) this.mappersConfigMap.get(filterPrp);
	}
	
	@Override
	public <P, I> PropertyMapperConfig<Q, F, P, I> modifyMapper(Function<F, P> filterPrp) {
		if (this.proxyFactoryCreator == null) {
            throw new QueryTemplateException("ProxyFactoryCreator is not set. Please set it using 'proxyFactoryCreator' method.");
        }
		ProxyFactory proxyFactory = this.proxyFactoryCreator.apply(this, this.filterClass);
		CallChainInfo callChainInfo = LambdaFieldNameDiscovery.fieldByGetMethodChainInfo(proxyFactory, filterPrp, this.filterClass);
		if (callChainInfo.getCallInfos().size() > 1) {
			throw new QueryTemplateException("Only one level of property mapping is allowed. Please use a single property reference.");
		}
		CallInfo callInfo = callChainInfo.getCallInfos().get(0);
		return (PropertyMapperConfig<Q, F, P, I>) this.modifyMapper(callInfo.getFieldName(), callInfo.getMethod().getReturnType());
	}
	
	@Override
	public <C extends Collection<I>, I> PropertyMapperConfig<Q, F, C, I> modifyMapperC(
		Function<F, Collection<I>> filterPrp) {
		return (PropertyMapperConfig<Q, F, C, I>) this;
	}
	
	@Override
	public <C extends List<I>, I> PropertyMapperConfig<Q, F, C, I> modifyMapperL(
		Function<F, Collection<I>> filterPrp) {
		return (PropertyMapperConfig<Q, F, C, I>) this;
	}
	
	@Override
	public <I> PropertyMapperConfig<Q, F, I[], I> modifyMapperA(Function<F, I[]> filterPrp) {
		return (PropertyMapperConfig<Q, F, I[], I>) this;
	}
	
    @Override
	public QueryTemplateConfig<Q, F> addQueryHelper(String key, String queryTextOriginal) {
        if (queryTextOriginal == null) {
            throw new QueryTemplateException("'queryHelper' cannot be null: '" + key + "'");
        }
    	QueryTemplateConfig<Q, F> helperConfig = new QueryTemplateConfigQueryHelper<>(queryTextOriginal, this);
        if (!this.queryHelpers.containsKey(key)) {
            this.queryHelpers.put(key, helperConfig);
        } else {
            throw new QueryTemplateException("Query Helper already added before. key: '" + key + "'");
        }

        return this;
    }

	@Override
	public QueryTemplateConfig<Q, F> targetReservedWordPositionalParameterMarker(String targetReservedWordPositionalParameterMarker) {
		this.targetReservedWordPositionalParameterMarker = targetReservedWordPositionalParameterMarker;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> convertNamedToPositionalParameters(boolean convertNamedToPositionalParameters) {
		this.convertNamedToPositionalParameters = convertNamedToPositionalParameters;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> targetReservedWordWhere(String targetReservedWordWhere) {
		this.targetReservedWordWhere = targetReservedWordWhere;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> targetReservedWordAnd(String targetReservedWordAnd) {
		this.targetReservedWordAnd = targetReservedWordAnd;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> targetReservedWordOr(String targetReservedWordOr) {
		this.targetReservedWordOr = targetReservedWordOr;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> targetReservedWordOpenParenthesis(String targetReservedWordOpenParenthesis) {
		this.targetReservedWordOpenParenthesis = targetReservedWordOpenParenthesis;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> targetReservedWordCloseParenthesis(String targetReservedWordCloseParenthesis) {
		this.targetReservedWordCloseParenthesis = targetReservedWordCloseParenthesis;
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> targetItemListSeparatorMarker(String targetItemListSeparatorMarker) {
		this.targetItemListSeparatorMarker = targetItemListSeparatorMarker;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> parameterUsagePrefix(String parameterUsagePrefix) {
		this.parameterUsagePrefix = parameterUsagePrefix;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> parameterNamePattern(String parameterNamePattern) {
		this.parameterNamePattern = parameterNamePattern;
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> parameterBasePosition(int parameterBasePosition) {
		this.parameterBasePosition = parameterBasePosition;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> filtersToken(String filtersToken) {
		this.filtersToken = Pattern.compile(filtersToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> whereToken(String whereToken) {
		this.whereToken = Pattern.compile(whereToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> propertiesToken(String propertiesToken) {
		this.propertiesToken = Pattern.compile(propertiesToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> repeatToken(String repeatToken) {
		this.repeatToken = Pattern.compile(repeatToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> queryHelperToken(String queryHelperToken) {
		this.queryHelperToken = Pattern.compile(queryHelperToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> andToken(String andToken) {
		this.andToken = Pattern.compile(andToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> orToken(String orToken) {
		this.orToken = Pattern.compile(orToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> noOperatorToken(String noOperatorToken) {
		this.noOperatorToken = Pattern.compile(noOperatorToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> openParenthesisToken(String openParenthesisToken) {
		this.openParenthesisToken = Pattern.compile(openParenthesisToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> closeParenthesisToken(String closeParenthesisToken) {
		this.closeParenthesisToken = Pattern.compile(closeParenthesisToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> extraToken(String extraToken) {
		this.extraToken = Pattern.compile(extraToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> criterionToken(String criterionToken) {
		this.criterionToken = Pattern.compile(criterionToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> propertiesDelimiterToken(String propertiesDelimiterToken) {
		this.propertiesDelimiterToken = Pattern.compile(propertiesDelimiterToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> criterionDelimiterToken(String criterionDelimiterToken) {
		this.criterionDelimiterToken = Pattern.compile(criterionDelimiterToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> escapeCharacter(String escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> reservedAnyProperty(String reservedAnyProperty) {
		this.reservedAnyProperty = reservedAnyProperty;
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> reservedEvalProperty(String reservedEvalProperty) {
		this.reservedEvalProperty = reservedEvalProperty;
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> compactQueryText(boolean compactQueryText) {
		this.compactQueryText = compactQueryText;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F>  clearMappers() {
		this.mappersConfigMap.clear();
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> evalRunnerCreator(Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator) {
		this.evalRunnerCreator = evalRunnerCreator;
		return this;
	}

	@Override
	public QueryTemplateConfig<Q, F> proxyFactoryCreator(ProxyFactoryCreator proxyFactoryCreator) {
		this.proxyFactoryCreator = proxyFactoryCreator;
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
	public Pattern getPropertiesDelimiterToken() {
		return propertiesDelimiterToken;
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
	public Map<String, PropertyMapperConfig<Q, F, ?, ?>> getMappersConfig() {
		return mappersConfigMap;
	}

	@Override
	public Map<String, QueryTemplateConfig<Q, F>> getQueryHelpers() {
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
	public Pattern getPropertiesToken() {
		return propertiesToken;
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
	public String getReservedAnyProperty() {
		return reservedAnyProperty;
	}
	
	@Override
	public String getReservedEvalProperty() {
		return this.reservedEvalProperty;
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
	public Function<QueryTemplateState<?, ?>, EvalRunner> getEvalRunnerCreator() {
		return evalRunnerCreator;
	}
	
	@Override
	public ProxyFactoryCreator getProxyFactoryCreator() {
		return proxyFactoryCreator;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> getParent() {
		return null;
	}

	@Override
	public String toString() {
		return "QueryTemplateConfigRoot ["
				+ "convertNamedToPositionalParameters=" + convertNamedToPositionalParameters 
				+ ", mappersConfigMap=" + mappersConfigMap + ", queryTextOriginal=" + queryTextOriginal
				+ ", queryHelpers=" + queryHelpers + "]";
	}
}
