package io.github.querytemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class QueryTemplateConfigQueryHelper<Q> implements QueryTemplateConfig<Q> {

	private QueryTemplateConfig<Q> parent;
    private String queryTextOriginal;
    /** QueryTemplate used as Helpers to assemble inner parts of the query. */
    private Map<String, QueryTemplateConfig<Q>> queryHelpers = new LinkedHashMap<>();
	
	QueryTemplateConfigQueryHelper(String queryTextOriginal,
			QueryTemplateConfig<Q> parent) {
		this.parent = parent;
		this.queryTextOriginal = queryTextOriginal;
	}
	
	@Override
	public QueryTemplateConfig<Q> addQueryHelper(String key,
		String content) {
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

	@Override
	public Map<String, QueryTemplateConfig<Q>> getQueryHelpers() {
		return Collections.unmodifiableMap(this.queryHelpers);
	}
	
	@Override
	public String getQueryTextOriginal() {
		return this.queryTextOriginal;
	}
	
	@Override
	public <P> PropertyMapperConfig<Q, P> addMapper(String filterPrp,
		Class<P> propertyClass) {
		throw new QueryTemplateException("QueryHelper cannot add mappers. Use the parent QueryTemplateConfig to add mappers.");
	}
	
	@Override
	public QueryTemplateConfig<Q> removeMapper(String filterPrp) {
		throw new QueryTemplateException("QueryHelper cannot remove mappers. Use the parent QueryTemplateConfig to remove mappers.");
	}
	
	@Override
	public <P> PropertyMapperConfig<Q, P> modifyMapper(String filterPrp, Class<P> propertyClass) {
		throw new QueryTemplateException("QueryHelper cannot modify mappers. Use the parent QueryTemplateConfig to modify mappers.");
	}
	
	@Override
	public QueryTemplateConfig<Q> targetReservedWordPositionalParameterMarker(
		String targetReservedWordPositionalParameterMarker) {
		this.parent.targetReservedWordPositionalParameterMarker(targetReservedWordPositionalParameterMarker);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> convertNamedToPositionalParameters(boolean convertNamedToPositionalParameters) {
		this.parent.convertNamedToPositionalParameters(convertNamedToPositionalParameters);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> targetReservedWordWhere(String targetReservedWordWhere) {
		this.parent.targetReservedWordWhere(targetReservedWordWhere);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> targetReservedWordAnd(String targetReservedWordAnd) {
		this.parent.targetReservedWordAnd(targetReservedWordAnd);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> targetReservedWordOr(String targetReservedWordOr) {
		this.parent.targetReservedWordOr(targetReservedWordOr);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> targetReservedWordOpenParenthesis(String targetReservedWordOpenParenthesis) {
		this.parent.targetReservedWordOpenParenthesis(targetReservedWordOpenParenthesis);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> targetReservedWordCloseParenthesis(String targetReservedWordCloseParenthesis) {
		this.parent.targetReservedWordCloseParenthesis(targetReservedWordCloseParenthesis);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> targetItemListSeparatorMarker(String targetItemListSeparatorMarker) {
		this.parent.targetItemListSeparatorMarker(targetItemListSeparatorMarker);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> parameterUsagePrefix(String parameterUsagePrefix) {
		this.parent.parameterUsagePrefix(parameterUsagePrefix);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> parameterNamePattern(String parameterNamePattern) {
		this.parent.parameterNamePattern(parameterNamePattern);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> parameterBasePosition(int parameterBasePosition) {
		this.parent.parameterBasePosition(parameterBasePosition);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> filtersToken(String filtersToken) {
		this.parent.filtersToken(filtersToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> whereToken(String whereToken) {
		this.parent.whereToken(whereToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> andToken(String andToken) {
		this.parent.andToken(andToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> orToken(String orToken) {
		this.parent.orToken(orToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> noOperatorToken(String noOperatorToken) {
		this.parent.noOperatorToken(noOperatorToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> openParenthesisToken(String openParenthesisToken) {
		this.parent.openParenthesisToken(openParenthesisToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> closeParenthesisToken(String closeParenthesisToken) {
		this.parent.closeParenthesisToken(closeParenthesisToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> extraToken(String extraToken) {
		this.parent.extraToken(extraToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> propertiesToken(String propertiesToken) {
		this.parent.propertiesToken(propertiesToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> repeatToken(String repeatToken) {
		this.parent.repeatToken(repeatToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> criterionToken(String criterionToken) {
		this.parent.criterionToken(criterionToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> propertiesDelimiterToken(String propertiesDelimiterToken) {
		this.parent.propertiesDelimiterToken(propertiesDelimiterToken);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> criterionDelimiterToken(String criterionDelimiterToken) {
		this.parent.criterionDelimiterToken(criterionDelimiterToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> queryHelperToken(String queryHelperToken) {
		this.parent.queryHelperToken(queryHelperToken);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> reservedAnyProperty(String reservedAnyProperty) {
		this.parent.reservedAnyProperty(reservedAnyProperty);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> reservedEvalProperty(String reservedEvalProperty) {
		this.parent.reservedEvalProperty(reservedEvalProperty);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> escapeCharacter(String escapeCharacter) {
		this.parent.escapeCharacter(escapeCharacter);
		return this;
	}
	
	@Override
	public QueryTemplateConfig<Q> compactQueryText(boolean compactQueryText) {
		this.parent.compactQueryText(compactQueryText);
		return this;
	}

	@Override
	public QueryTemplateConfig<Q> clearMappers() {
		throw new QueryTemplateException("QueryHelper cannot clear mappers. Use the parent QueryTemplateConfig to clear mappers.");
	}
	
	@Override
	public QueryTemplateConfig<Q> evalRunnerCreator(Function<QueryTemplateState<?>, EvalRunner> evalRunnerCreator) {
		this.parent.evalRunnerCreator(evalRunnerCreator);
		return this;
	}

	@Override
	public Pattern getFiltersToken() {
		return this.parent.getFiltersToken();
	}

	@Override
	public Pattern getAndToken() {
		return this.parent.getAndToken();
	}

	@Override
	public Pattern getOrToken() {
		return this.parent.getOrToken();
	}

	@Override
	public Pattern getNoOperatorToken() {
		return this.parent.getNoOperatorToken();
	}

	@Override
	public Pattern getOpenParenthesisToken() {
		return this.parent.getOpenParenthesisToken();
	}

	@Override
	public Pattern getCloseParenthesisToken() {
		return this.parent.getCloseParenthesisToken();
	}

	@Override
	public Pattern getExtraToken() {
		return this.parent.getExtraToken();
	}

	@Override
	public Pattern getCriterionToken() {
		return this.parent.getCriterionToken();
	}

	@Override
	public Pattern getPropertiesDelimiterToken() {
		return this.parent.getPropertiesDelimiterToken();
	}

	@Override
	public Pattern getCriterionDelimiterToken() {
		return this.parent.getCriterionDelimiterToken();
	}

	@Override
	public String getEscapeCharacter() {
		return this.parent.getEscapeCharacter();
	}

	@Override
	public Map<String, PropertyMapperConfig<Q, ?>> getMappersConfig() {
		return this.parent.getMappersConfig();
	}

	@Override
	public String getTargetReservedWordWhere() {
		return this.parent.getTargetReservedWordWhere();
	}

	@Override
	public Pattern getWhereToken() {
		return this.parent.getWhereToken();
	}

	@Override
	public Pattern getPropertiesToken() {
		return this.parent.getPropertiesToken();
	}

	@Override
	public Pattern getRepeatToken() {
		return this.parent.getRepeatToken();
	}

	@Override
	public Pattern getQueryHelperToken() {
		return this.parent.getQueryHelperToken();
	}

	@Override
	public String getReservedAnyProperty() {
		return this.parent.getReservedAnyProperty();
	}
	
	@Override
	public String getReservedEvalProperty() {
		return this.parent.getReservedEvalProperty();
	}

	@Override
	public String getTargetReservedWordAnd() {
		return this.parent.getTargetReservedWordAnd();
	}

	@Override
	public String getTargetReservedWordOr() {
		return this.parent.getTargetReservedWordOr();
	}

	@Override
	public String getTargetReservedWordOpenParenthesis() {
		return this.parent.getTargetReservedWordOpenParenthesis();
	}

	@Override
	public String getTargetReservedWordCloseParenthesis() {
		return this.parent.getTargetReservedWordCloseParenthesis();
	}

	@Override
	public String getTargetReservedWordPositionalParameterMarker() {
		return this.parent.getTargetReservedWordPositionalParameterMarker();
	}

	@Override
	public String getTargetItemListSeparatorMarker() {
		return this.parent.getTargetItemListSeparatorMarker();
	}

	@Override
	public boolean isConvertNamedToPositionalParameters() {
		return this.parent.isConvertNamedToPositionalParameters();
	}

	@Override
	public String getParameterUsagePrefix() {
		return this.parent.getParameterUsagePrefix();
	}

	@Override
	public String getParameterNamePattern() {
		return this.parent.getParameterNamePattern();
	}

	@Override
	public int getParameterBasePosition() {
		return this.parent.getParameterBasePosition();
	}
	
	@Override
	public boolean isCompactQueryText() {
		return this.parent.isCompactQueryText();
	}

	@Override
	public Function<QueryTemplateState<?>, EvalRunner> getEvalRunnerCreator() {
		return this.parent.getEvalRunnerCreator();
	}

	@Override
	public QueryTemplateConfig<Q> getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		return "QueryTemplateConfigQueryHelper [queryTextOriginal=" + queryTextOriginal 
				+ ", queryHelpers=" + queryHelpers + "]";
	}
	
	
}
