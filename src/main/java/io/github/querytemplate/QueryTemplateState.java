package io.github.querytemplate;

import java.util.List;
import java.util.Map;

public interface QueryTemplateState<Q> {

	QueryTemplate<Q> getTemplateOwner();

	String getQueryString();

	Object getFilter();

	Map<PropertyMapper<Q, ?>, List<AssignedParameterInfo<?>>> getPropertyMapperToAssignedParameterInfo();
}