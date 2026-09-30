package io.github.querytemplate;

import java.util.Map;

/**
 * Mapper used to relate:<br>
 * FilterProperty X ParameterName X EntityProperty X ParticipatesInQuery X CriteriaHandler.
 */
public class PropertyMapper<Q, P> {

    private String filterPrp;
    //private String parameterName;
    //Not used
    //private String entityPrp;
    private ParticipatesInQuery participatesInQuery;
    //private Type type = null;
    private AssignNamedParameter<Q, P> onParticipatesNamed = null;
    private AssignPositionalParameter<Q, P> onParticipatesPositional = null;
    //private boolean parameterAsList = false;
//    private boolean unpackListItems = false;
//    private boolean isRepeater = false;
    private Map<String, ParameterMapper<Q, P>> parameterMappers;    
    
	public ParameterMapper<Q, P> addParameter(String parameterName) {
		ParameterMapper<Q, P> parameterMapper = new ParameterMapper<Q, P>(this)
				.parameterName(parameterName);
		this.parameterMappers.put(parameterName, parameterMapper);
		return parameterMapper;
	}
	
	public PropertyMapper<Q, P> removeParameter(String filterPrp) {
		this.parameterMappers.remove(filterPrp);
		return this;
	}
	
//	public ParameterMapper<Q, P> modifyParameter(String parameterName) {
//        return this.parameterMappers.get(parameterName);
//	}
    
//	public static <SQ, SP> PropertyMapper<SQ, SP> of(String filterPrp, Class<SP> propertyClass) {
//		return new PropertyMapper<SQ, SP>(filterPrp);
//	}
    
    public Map<String, ParameterMapper<Q, P>> getParameterMappers() {
		return parameterMappers;
	}

	/**
     * Constructor.
     *
     * @param filterPrp    the property name in the filter.
     */
    PropertyMapper(String filterPrp) {
        this.filterPrp = filterPrp;
        this.parameterMappers = new java.util.LinkedHashMap<>();
    }

    /**
     * @return the property name in the filter.
     */
    public String getFilterPrp() {
        return filterPrp;
    }

    public PropertyMapper<Q, P>  filterPrp(String filterPrp) {
        this.filterPrp = filterPrp;
        return this;
    }
    
//	public String getParameterName() {
//		return parameterName;
//	}
	
//	public PropertyMapper<Q, P> parameterName(String parameterName) {
//		this.parameterName = parameterName;
//		return this;
//	}

//    /**
//     * @return the property name in the searched entity.
//     */
//    public String getEntityPrp() {
//        return entityPrp;
//    }

//    public void setEntityPrp(String entityPrp) {
//        this.entityPrp = entityPrp;
//    }

    /**
     * @return the ParticipatesInQuery instance that verifies whether the property participates in the query.
     */
    public ParticipatesInQuery getParticipatesInQuery() {
        return participatesInQuery;
    }

    /**
     * Sets the ParticipatesInQuery instance that verifies whether the property participates in the query.
     * @param participatesInQuery the ParticipatesInQuery instance
     * @return this PropertyMapper instance for method chaining.
     */
    public PropertyMapper<Q, P> participatesInQuery(ParticipatesInQuery participatesInQuery) {
        this.participatesInQuery = participatesInQuery;
        return this;
    }

//    /**
//     * @return whether the value must be treated as a list to set the parameter.
//     */
//    public boolean isParameterAsList() {
//        return parameterAsList;
//    }

//	/**
//	 * If <code>true</code> the value must be treated as a list to set the
//	 * parameter, using <code>Query.setParameterList</code> for example.<br>
//	 * if parameterAsList is set to <code>true</code>, {@link #unpackListItems(boolean)} 
//	 * and {@link #repeater(boolean)} are set to <code>false</code> automatically.
//	 *
//	 * @param parameterAsList the parameterAsList to set
//	 * @return this PropertyMapper instance for method chaining.
//	 */
//    public PropertyMapper<Q, P> parameterAsList(boolean parameterAsList) {
//    	if (parameterAsList) {
//    		this.unpackListItems(false); // If treating as list, we don't want to unpack the items
//    		this.repeater(false); // If treating as list, we don't want to treat it as a repeater
//    	}
//        this.parameterAsList = parameterAsList;
//        return this;
//    }

	/**
	 * @return the AssignNamedParameter instance that sets the parameter in the
	 *         query.
	 */
	public AssignNamedParameter<Q, P> getOnParticipatesNamed() {
		return onParticipatesNamed;
	}

	/**
	 * Sets the AssignNamedParameter instance that sets the parameter in the query.
	 * 
	 * @param onParticipatesNamed the AssignNamedParameter instance
	 * @return this PropertyMapper instance for method chaining.
	 */
	public PropertyMapper<Q, P> onParticipatesNamed(AssignNamedParameter<Q, P> onParticipatesNamed) {
		this.onParticipatesNamed = onParticipatesNamed;
		return this;
	}

	/**
	 * @return the AssignPositionalParameter instance that sets the parameter in the
	 *         query.
	 */
	public AssignPositionalParameter<Q, P> getOnParticipatesPositional() {
		return onParticipatesPositional;
	}
	
	/**
	 * Sets the AssignPositionalParameter instance that sets the parameter in the
	 * query.
	 * 
	 * @param onParticipatesPositional the AssignPositionalParameter instance
	 * @return this PropertyMapper instance for method chaining.
	 */
	public PropertyMapper<Q, P> onParticipatesPositional(AssignPositionalParameter<Q, P> onParticipatesPositional) {
		this.onParticipatesPositional = onParticipatesPositional;
		return this;
	}
	
//	public boolean isUnpackListItems() { 
//		return unpackListItems;
//	}
//
//	/**
//	 * If <code>true</code> the enumerable must be unpacked into individual items 
//	 * separated by comma instead of being set as a list using {@code Query.setParameterList}
//	 * for example.<br>
//	 * The separator on processed query String will depend on {@link QueryTemplateConfig#targetItemListSeparatorMarker(String)} 
//	 * configuration.<br>
//	 * If unpacking, we don't want to treat it as a list parameter or repeater. 
//	 * So if it is set to <code>true</code>, {link {@link #repeater(boolean)} 
//	 * is set to <code>false</code> automatically.
//	 * 
//	 * @param unpackListItems the unpackListItems to set
//	 * @return this PropertyMapper instance for method chaining.
//	 */
//	public PropertyMapper<Q, P> unpackListItems(boolean unpackListItems) {
//		if (unpackListItems) {
//			//this.parameterAsList(false); // If unpacking, we don't want to treat it as a list parameter
//			this.repeater(false); // If unpacking, we don't want to treat it as a repeater
//		}
//		this.unpackListItems = unpackListItems;
//		return this;
//	}
//	
//	public boolean isRepeater() {
//		return isRepeater;
//	}
	
//	/**
//	 * If <code>true</code> the property is considered a repeater, meaning that 
//	 * the value items will be used to repeat a part of the query multiple times.<br>
//	 * If it is set to <code>true</code>, {@link #unpackListItems(boolean)} are set 
//	 * to <code>false</code> automatically.
//	 * @param isRepeater The value indicating whether the property is a repeater or not.
//	 * @return This PropertyMapper instance for method chaining.
//	 */
//	public PropertyMapper<Q, P> repeater(boolean isRepeater) {
//		if (isRepeater) {
//			//this.parameterAsList(false); // If unpacking, we don't want to treat it as a list parameter
//			this.unpackListItems(false); // If unpacking, we don't want to treat it as a repeater
//		}
//		this.isRepeater = isRepeater;
//		return this;
//	}
}
