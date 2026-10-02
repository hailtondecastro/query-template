package io.github.querytemplate;

public class ParameterMapperConfigDefault<Q, F, P, I> implements ParameterMapperConfig<Q, F, P, I> {
	private PropertyMapperConfigInternal<Q, F, P, I> propertyMapperConfig;
	
	private String parameterName;
    private boolean unpackListItems = false;
    private boolean repeater = false;
    private AssignNamedParameter<Q, P> onParticipatesNamed = null;
    private AssignPositionalParameter<Q, P> onParticipatesPositional = null;
	
    ParameterMapperConfigDefault(PropertyMapperConfig<Q, F, P, I> propertyMapperConfig) {
		this.propertyMapperConfig = (PropertyMapperConfigInternal<Q, F, P, I>) propertyMapperConfig;
    }
    
	@Override
	public ParameterMapperConfig<Q, F, P, I> parameterName(String parameterName) {
		if (parameterName == null) {
			throw new IllegalArgumentException("parameterName cannot be null");	
		}				
		this.parameterName = parameterName;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> unpackListItems(boolean unpackListItems) {
		this.unpackListItems = unpackListItems;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> repeater(boolean repeater) {
		this.repeater = repeater;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> onParticipatesNamed(AssignNamedParameter<Q, P> onParticipatesNamed) {
		this.onParticipatesNamed = onParticipatesNamed;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> onParticipatesPositional(AssignPositionalParameter<Q, P> onParticipatesPositional) {
		this.onParticipatesPositional = onParticipatesPositional;
		return this;
	}
	
	@Override
	public String getParameterName() {
		if (this.parameterName == null) {
			return this.propertyMapperConfig.getFilterPrp();
		}
		return parameterName;
	}
	
	@Override
	public boolean isUnpackListItems() {
		return unpackListItems;
	}
	
	@Override
	public boolean isRepeater() {
		return repeater;
	}
	
	@Override
	public AssignNamedParameter<Q, P> getOnParticipatesNamed() {
		return this.onParticipatesNamed;
	}
	
	@Override
	public AssignPositionalParameter<Q, P> getOnParticipatesPositional() {
		return this.onParticipatesPositional;
	}
	
	@Override
	public PropertyMapperConfig<Q, F, P, I> done() {
		this.propertyMapperConfig.updateParameter(this);
		return this.propertyMapperConfig;
	}
	
	@Override
	public String toString() {
		return "ParameterMapperConfig [parameterName=" + parameterName + ", unpackListItems=" + unpackListItems
				+ ", repeater=" + repeater + "]";
	}
}
