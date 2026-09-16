package io.github.querytemplate;

/**
 * Mapper used to relate:<br>
 * FilterProperty X ParameterName X EntityProperty X FillVerifier X CriteriaHandler.
 */
public class PropertyMapper<Q, P> {

    private String filterPrp;
    private String parameterName;
    //Not used
    //private String entityPrp;
    private FillVerifier fillVerifier;
    //private Type type = null;
    private AssignNamedParameter<Q, P> onFilledNamed = null;
    private AssignPositionalParameter<Q, P> onFilledPositional = null;
    //private boolean parameterAsList = false;
    private boolean unpackListItems = false;
    private boolean isRepeater = false;

//	public static <SQ, SP> PropertyMapper<SQ, SP> of(String filterPrp, Class<SP> propertyClass) {
//		return new PropertyMapper<SQ, SP>(filterPrp);
//	}
    
    /**
     * Constructor.
     *
     * @param filterPrp    the property name in the filter.
     */
    PropertyMapper(String filterPrp) {
        this.filterPrp = filterPrp;
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
    
	public String getParameterName() {
		return parameterName;
	}
	
	public PropertyMapper<Q, P> parameterName(String parameterName) {
		this.parameterName = parameterName;
		return this;
	}

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
     * @return the fill verifier.
     */
    public FillVerifier getFillVerifier() {
        return fillVerifier;
    }

    public PropertyMapper<Q, P> fillVerifier(FillVerifier fillVerifier) {
        this.fillVerifier = fillVerifier;
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

	public AssignNamedParameter<Q, P> getOnFilledNamed() {
		return onFilledNamed;
	}

	public PropertyMapper<Q, P> onFilled(AssignNamedParameter<Q, P> onFilledNamed) {
		this.onFilledNamed = onFilledNamed;
		return this;
	}

	public AssignPositionalParameter<Q, P> getOnFilledPositional() {
		return onFilledPositional;
	}
	
	public PropertyMapper<Q, P> onFilled(AssignPositionalParameter<Q, P> onFilledPositional) {
		this.onFilledPositional = onFilledPositional;
		return this;
	}
	
	public boolean isUnpackListItems() { 
		return unpackListItems;
	}

	/**
	 * If <code>true</code> the enumerable must be unpacked into individual items 
	 * separated by comma instead of being set as a list using {@code Query.setParameterList}
	 * for example.<br>
	 * The separator on processed query String will depend on {@link QueryTemplateConfig#targetItemListSeparatorMarker(String)} 
	 * configuration.<br>
	 * If unpacking, we don't want to treat it as a list parameter or repeater. 
	 * So if it is set to <code>true</code>, {link {@link #repeater(boolean)} 
	 * is set to <code>false</code> automatically.
	 * 
	 * @param unpackListItems the unpackListItems to set
	 * @return this PropertyMapper instance for method chaining.
	 */
	public PropertyMapper<Q, P> unpackListItems(boolean unpackListItems) {
		if (unpackListItems) {
			//this.parameterAsList(false); // If unpacking, we don't want to treat it as a list parameter
			this.repeater(false); // If unpacking, we don't want to treat it as a repeater
		}
		this.unpackListItems = unpackListItems;
		return this;
	}
	
	public boolean isRepeater() {
		return isRepeater;
	}
	
	/**
	 * If <code>true</code> the property is considered a repeater, meaning that 
	 * the value items will be used to repeat a part of the query multiple times.<br>
	 * If it is set to <code>true</code>, {@link #unpackListItems(boolean)} are set 
	 * to <code>false</code> automatically.
	 * @param isRepeater The value indicating whether the property is a repeater or not.
	 * @return This PropertyMapper instance for method chaining.
	 */
	public PropertyMapper<Q, P> repeater(boolean isRepeater) {
		if (isRepeater) {
			//this.parameterAsList(false); // If unpacking, we don't want to treat it as a list parameter
			this.unpackListItems(false); // If unpacking, we don't want to treat it as a repeater
		}
		this.isRepeater = isRepeater;
		return this;
	}
}
