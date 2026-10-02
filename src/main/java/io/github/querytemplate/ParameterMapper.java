package io.github.querytemplate;

/**
 * Mapper used to relate:<br>
 * FilterProperty X ParameterName X EntityProperty X ParticipatesInQuery X CriteriaHandler.
 */
class ParameterMapper<Q, F, P> {

	private PropertyMapper<Q, F, P> owner;
	
    private String parameterName;
    private boolean unpackListItems = false;
    private boolean isRepeater = false;
    private AssignNamedParameter<Q, P> onParticipatesNamed = null;
    private AssignPositionalParameter<Q, P> onParticipatesPositional = null;

//	public static <SQ, SP> PropertyMapper<SQ, SP> of(String filterPrp, Class<SP> propertyClass) {
//		return new PropertyMapper<SQ, SP>(filterPrp);
//	}
    
    /**
     * Constructor.
     *
     * @param filterPrp    the property name in the filter.
     */
    ParameterMapper(PropertyMapper<Q, F, P> owner) {
        this.owner = owner;
    }
    
	public String getParameterName() {
		return parameterName;
	}
	
	public ParameterMapper<Q, F, P> parameterName(String parameterName) {
		this.parameterName = parameterName;
		return this;
	}
		
	public boolean isUnpackListItems() { 
		return unpackListItems;
	}
	
	public PropertyMapper<Q, F, P> getOwner() {
		return owner;
	}

	public AssignNamedParameter<Q, P> getOnParticipatesNamed() {
		return onParticipatesNamed;
	}

	public AssignPositionalParameter<Q, P> getOnParticipatesPositional() {
		return onParticipatesPositional;
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
	public ParameterMapper<Q, F, P> unpackListItems(boolean unpackListItems) {
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
	public ParameterMapper<Q, F, P> repeater(boolean isRepeater) {
		if (isRepeater) {
			//this.parameterAsList(false); // If unpacking, we don't want to treat it as a list parameter
			this.unpackListItems(false); // If unpacking, we don't want to treat it as a repeater
		}
		this.isRepeater = isRepeater;
		return this;
	}
	
	public ParameterMapper<Q, F, P> onParticipatesNamed(AssignNamedParameter<Q, P> onParticipatesNamed) {
		this.onParticipatesNamed = onParticipatesNamed;
		return this;
	}
	
	public ParameterMapper<Q, F, P> onParticipatesPositional(AssignPositionalParameter<Q, P> onParticipatesPositional) {
		this.onParticipatesPositional = onParticipatesPositional;
		return this;
	}
}
