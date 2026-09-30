package io.github.querytemplate;

import java.util.List;
import java.util.Map;

public interface QueryTemplateStateInternal<Q> extends QueryTemplateState<Q> {

	void setPropertyMapperToAssignedParameterInfo(
		Map<PropertyMapper<Q, ?>, List<AssignedParameterInfo<?>>> propertyMapperToAssignedParameterInfo);

	Map<PropertyMapper<Q, ?>, Map<Integer, List<AssignedParameterInfo<?>>>> getPropertyMapperItemIndexToAssignedParameterInfo();

	void setPropertyMapperItemIndexToAssignedParameterInfo(
		Map<PropertyMapper<Q, ?>, Map<Integer, List<AssignedParameterInfo<?>>>> propertyMapperItemIndexToAssignedParameterInfo);

	void setTemplateOwner(QueryTemplate<Q> templateOwner);

	void setQueryString(String queryString);

	void setFilter(Object filter);

	void makeUnmodifiable();
	
	/**
	 * {@link EvalRunner} when {@link QueryTemplateConfig#RESERVED_EVAL_PROPERTY} is used in the query template.
	 * 
	 * @return
	 */
	EvalRunner getEvalRunner();
	
	void setEvalRunner(EvalRunner evalRunner);
}