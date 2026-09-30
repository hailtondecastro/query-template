package io.github.querytemplate;

import java.util.List;
import java.util.Map;

public class QueryTemplateStateDefault<Q> implements QueryTemplateStateInternal<Q> {
	private QueryTemplate<Q> templateOwner;
	private String queryString;
	private Object filter;
	private Map<PropertyMapper<Q, ?>, List<AssignedParameterInfo<?>>> propertyMapperToAssignedParameterInfo;
	private Map<PropertyMapper<Q, ?>, Map<Integer,List<AssignedParameterInfo<?>>>> propertyMapperItemIndexToAssignedParameterInfo;
	private EvalRunner evalRunner;
//	/**
//	 * See {@link #getIsRepeatablePropertyMapper()} for details.
//	 */
//	private Map<PropertyMapper<Q, ?>, Boolean> isRepeatablePropertyMapper;
	
	
	/**
	 * @param templateOwner
	 * @param queryString
	 * @param filter
	 * @param propertyMapperToAssignedParameterInfo
	 * @param propertyMapperItemIndexToAssignedParameterInfo
	 * @param isRepeatablePropertyMapper See {@link #getIsRepeatablePropertyMapper()} for details.
	 */
	QueryTemplateStateDefault(
		QueryTemplate<Q> templateOwner,
		String queryString,
		Object filter,
		Map<PropertyMapper<Q, ?>, List<AssignedParameterInfo<?>>> propertyMapperToAssignedParameterInfo,
		Map<PropertyMapper<Q, ?>, Map<Integer,List<AssignedParameterInfo<?>>>> propertyMapperItemIndexToAssignedParameterInfo,
		Map<PropertyMapper<Q, ?>, Boolean> isRepeatablePropertyMapper) {
		super();
		this.templateOwner = templateOwner;
		this.queryString = queryString;
		this.filter = filter;
		this.propertyMapperToAssignedParameterInfo = propertyMapperToAssignedParameterInfo;
		this.propertyMapperItemIndexToAssignedParameterInfo = propertyMapperItemIndexToAssignedParameterInfo;
	}


	@Override
	public QueryTemplate<Q> getTemplateOwner() {
		return templateOwner;
	}

	@Override
	public String getQueryString() {
		return queryString;
	}


	@Override
	public Object getFilter() {
		return filter;
	}
	

//	/**
//	 * If <code>true</code> the enumerable must be unfolded (set as if there were several
//     * parameters: paramName_0, paramName_1, paramName_2, etc.) instead of being set
//     * as a list using {@code Query.setParameterList} for example. Used internally when the
//     * parameter sentence repetition is used. 
//	 * 
//	 * @return the isRepeatablePropertyMapper, which is a map of property mappers to a boolean indicating 
//	 * whether the property mapper is used on Criterion that is marked as repeatable and 
//	 * propery mapper is set as {@link PropertyMapper#isParameterAsList()}.
//	 * Mapped to false if property mapper is used on Criterion that is not 
//	 * marked as repeatable or property mapper is not set as {@link PropertyMapper#isParameterAsList()}.<br>
//	 */
//	public Map<PropertyMapper<Q, ?>, Boolean> getIsRepeatablePropertyMapper() {
//		return isRepeatablePropertyMapper;
//	}
	
	@Override
	public Map<PropertyMapper<Q, ?>, List<AssignedParameterInfo<?>>> getPropertyMapperToAssignedParameterInfo() {
		return propertyMapperToAssignedParameterInfo;
	}

	@Override
	public void setPropertyMapperToAssignedParameterInfo(
		Map<PropertyMapper<Q, ?>, List<AssignedParameterInfo<?>>> propertyMapperToAssignedParameterInfo) {
		this.propertyMapperToAssignedParameterInfo = propertyMapperToAssignedParameterInfo;
	}

	@Override
	public Map<PropertyMapper<Q, ?>, Map<Integer, List<AssignedParameterInfo<?>>>> getPropertyMapperItemIndexToAssignedParameterInfo() {
		return propertyMapperItemIndexToAssignedParameterInfo;
	}

	@Override
	public void setPropertyMapperItemIndexToAssignedParameterInfo(
		Map<PropertyMapper<Q, ?>, Map<Integer, List<AssignedParameterInfo<?>>>> propertyMapperItemIndexToAssignedParameterInfo) {
		this.propertyMapperItemIndexToAssignedParameterInfo = propertyMapperItemIndexToAssignedParameterInfo;
	}

	@Override
	public void setTemplateOwner(QueryTemplate<Q> templateOwner) {
		this.templateOwner = templateOwner;
	}

	@Override
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public void setFilter(Object filter) {
		this.filter = filter;
	}

//	/**
//	 * See {@link #getIsRepeatablePropertyMapper()} for details.
//	 * @param isRepeatablePropertyMapper
//	 */
//	void setIsRepeatablePropertyMapper(Map<PropertyMapper<Q, ?>, Boolean> isRepeatablePropertyMapper) {
//		this.isRepeatablePropertyMapper = isRepeatablePropertyMapper;
//	}

	@Override
	public void makeUnmodifiable() {
        this.propertyMapperToAssignedParameterInfo = Map.copyOf(this.propertyMapperToAssignedParameterInfo);
        this.propertyMapperItemIndexToAssignedParameterInfo = Map.copyOf(this.propertyMapperItemIndexToAssignedParameterInfo);
        //this.isRepeatablePropertyMapper = Map.copyOf(this.isRepeatablePropertyMapper);
    }

	/**
	 * {@link EvalRunner} when {@link QueryTemplateConfig#RESERVED_EVAL_PROPERTY} is used in the query template.
	 * 
	 * @return
	 */
	@Override
	public EvalRunner getEvalRunner() {
		return evalRunner;
	}

	@Override
	public void setEvalRunner(EvalRunner evalRunner) {
		this.evalRunner = evalRunner;
	}
}
