package io.github.querytemplate;

import java.util.List;
import java.util.Map;

public interface QueryTemplateStateInternal<Q, F> extends QueryTemplateState<Q, F> {

	void setPropertyMapperToAssignedParameterInfo(
		Map<PropertyMapper<Q, F, ?>, List<AssignedParameterInfo<?>>> propertyMapperToAssignedParameterInfo);

	Map<PropertyMapper<Q, F, ?>, Map<Integer, List<AssignedParameterInfo<?>>>> getPropertyMapperItemIndexToAssignedParameterInfo();

	void setPropertyMapperItemIndexToAssignedParameterInfo(
		Map<PropertyMapper<Q, F, ?>, Map<Integer, List<AssignedParameterInfo<?>>>> propertyMapperItemIndexToAssignedParameterInfo);

	void setTemplateOwner(QueryTemplate<Q, F> templateOwner);

	void setQueryString(String queryString);

	void setFilter(F filter);

	void makeUnmodifiable();
	
	/**
	 * {@link EvalRunner} when {@link QueryTemplateConfig#RESERVED_EVAL_PROPERTY} is used in the query template.
	 * 
	 * @return the {@link EvalRunner} instance
	 */
	EvalRunner getEvalRunner();
	
	void setEvalRunner(EvalRunner evalRunner);
}