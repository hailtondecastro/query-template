package io.github.querytemplate;

import java.util.List;
import java.util.Map;

public interface QueryTemplateState<Q, F> {

	QueryTemplate<Q, F> getTemplateOwner();

	String getQueryString();

	F getFilter();

	Map<PropertyMapper<Q, F, ?>, List<AssignedParameterInfo<?>>> getPropertyMapperToAssignedParameterInfo();
}