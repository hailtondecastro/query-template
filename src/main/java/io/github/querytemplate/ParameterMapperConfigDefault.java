package io.github.querytemplate;

public class ParameterMapperConfigDefault<Q, P> implements ParameterMapperConfig<Q, P> {
	private PropertyMapperConfigInternal<Q, P> propertyMapperConfig;
	
	private String parameterName;
    private boolean unpackListItems = false;
    private boolean repeater = false;
    
    ParameterMapperConfigDefault(PropertyMapperConfig<Q, P> propertyMapperConfig) {
		this.propertyMapperConfig = (PropertyMapperConfigInternal<Q, P>) propertyMapperConfig;
    }
    
	@Override
	public ParameterMapperConfig<Q, P> parameterName(String parameterName) {
		if (parameterName == null) {
			throw new IllegalArgumentException("parameterName cannot be null");	
		}				
		this.parameterName = parameterName;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, P> unpackListItems(boolean unpackListItems) {
		this.unpackListItems = unpackListItems;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, P> repeater(boolean repeater) {
		this.repeater = repeater;
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
	public PropertyMapperConfig<Q, P> done() {
		this.propertyMapperConfig.updateParameter(this);
		return this.propertyMapperConfig;
	}
	
	@Override
	public String toString() {
		return "ParameterMapperConfig [parameterName=" + parameterName + ", unpackListItems=" + unpackListItems
				+ ", repeater=" + repeater + "]";
	}
}
