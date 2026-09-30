package io.github.querytemplate;

import java.util.List;
import java.util.Map;

public class QueryTemplateState<Q> {
	private QueryTemplate<Q> templateOwner;
	private String queryString;
	private Object filter;
	private Map<PropertyMapper<Q, ?>, List<Integer>> propertyMapperToParameterPositions;
	private Map<PropertyMapper<Q, ?>, Map<Integer,List<Integer>>> propertyMapperItemIndexToParameterPositions;
//	/**
//	 * See {@link #getIsRepeatablePropertyMapper()} for details.
//	 */
//	private Map<PropertyMapper<Q, ?>, Boolean> isRepeatablePropertyMapper;
	
	
	/**
	 * @param templateOwner
	 * @param queryString
	 * @param filter
	 * @param propertyMapperToParameterPositions
	 * @param propertyMapperItemIndexToParameterPositions
	 * @param isRepeatablePropertyMapper See {@link #getIsRepeatablePropertyMapper()} for details.
	 */
	QueryTemplateState(
		QueryTemplate<Q> templateOwner,
		String queryString,
		Object filter,
		Map<PropertyMapper<Q, ?>, List<Integer>> propertyMapperToParameterPositions,
		Map<PropertyMapper<Q, ?>, Map<Integer,List<Integer>>> propertyMapperItemIndexToParameterPositions,
		Map<PropertyMapper<Q, ?>, Boolean> isRepeatablePropertyMapper) {
		super();
		this.templateOwner = templateOwner;
		this.queryString = queryString;
		this.filter = filter;
		this.propertyMapperToParameterPositions = propertyMapperToParameterPositions;
		this.propertyMapperItemIndexToParameterPositions = propertyMapperItemIndexToParameterPositions;
	}


	public QueryTemplate<Q> getTemplateOwner() {
		return templateOwner;
	}


	public String getQueryString() {
		return queryString;
	}


	public Object getFilter() {
		return filter;
	}


	public Map<PropertyMapper<Q, ?>, List<Integer>> getPropertyMapperToParameterPositions() {
		return propertyMapperToParameterPositions;
	}


	public Map<PropertyMapper<Q, ?>, Map<Integer, List<Integer>>> getPropertyMapperItemIndexToParameterPositions() {
		return propertyMapperItemIndexToParameterPositions;
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


	void setTemplateOwner(QueryTemplate<Q> templateOwner) {
		this.templateOwner = templateOwner;
	}


	void setQueryString(String queryString) {
		this.queryString = queryString;
	}


	void setFilter(Object filter) {
		this.filter = filter;
	}


	void setPropertyMapperToParameterPositions(
		Map<PropertyMapper<Q, ?>, List<Integer>> propertyMapperToParameterPositions) {
		this.propertyMapperToParameterPositions = propertyMapperToParameterPositions;
	}


	void setPropertyMapperItemIndexToParameterPositions(
		Map<PropertyMapper<Q, ?>, Map<Integer, List<Integer>>> propertyMapperItemIndexToParameterPositions) {
		this.propertyMapperItemIndexToParameterPositions = propertyMapperItemIndexToParameterPositions;
	}

//	/**
//	 * See {@link #getIsRepeatablePropertyMapper()} for details.
//	 * @param isRepeatablePropertyMapper
//	 */
//	void setIsRepeatablePropertyMapper(Map<PropertyMapper<Q, ?>, Boolean> isRepeatablePropertyMapper) {
//		this.isRepeatablePropertyMapper = isRepeatablePropertyMapper;
//	}
	
	void makeUnmodifiable() {
        this.propertyMapperToParameterPositions = Map.copyOf(this.propertyMapperToParameterPositions);
        this.propertyMapperItemIndexToParameterPositions = Map.copyOf(this.propertyMapperItemIndexToParameterPositions);
        //this.isRepeatablePropertyMapper = Map.copyOf(this.isRepeatablePropertyMapper);
    }
}
