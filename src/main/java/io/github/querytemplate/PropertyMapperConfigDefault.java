package io.github.querytemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

class PropertyMapperConfigDefault<Q, P> implements PropertyMapperConfigInternal<Q, P> {
	QueryTemplateConfig<Q> queryTemplateConfig;
	
	PropertyMapperConfigDefault(QueryTemplateConfig<Q> queryTemplateConfig) {
		this.parameterMappers = new LinkedHashMap<>();
		this.queryTemplateConfig = queryTemplateConfig;
	}
	
	@Override
	public QueryTemplateConfig<Q> done() {
		if (this.parameterMappers.isEmpty()) {
			this.addParameter().done();
		}
		return this.queryTemplateConfig;
	}
	
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
//    private boolean repeater = false;
    
    private Map<String, ParameterMapperConfig<Q, P>> parameterMappers;
    
	@Override
	public PropertyMapperConfig<Q, P> filterPrp(String filterPrp) {
		this.filterPrp = filterPrp;
		return this;
	}
	
//	@Override
//	public PropertyMapperConfig<Q, P> parameterName(String parameterName) {
//		this.parameterName = parameterName;
//		return this;
//	}
	
	@Override
	public PropertyMapperConfig<Q, P> participatesInQuery(ParticipatesInQuery participatesInQuery) {
		this.participatesInQuery = participatesInQuery;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, P> onParticipatesNamed(AssignNamedParameter<Q, P> onParticipatesNamed) {
		this.onParticipatesNamed = onParticipatesNamed;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, P> onParticipatesPositional(AssignPositionalParameter<Q, P> onParticipatesPositional) {
		this.onParticipatesPositional = onParticipatesPositional;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, P> addParameter(String parameterName) {
		boolean containsKey = this.parameterMappers.containsKey(parameterName);
		if (containsKey) {
			throw new IllegalArgumentException(
					"Parameter with name '" + parameterName + "' already exists and is another instance. Use modifyParameter() to modify an existing parameter.");
		}
		
		ParameterMapperConfig<Q, P> parameterMapperConfig = new ParameterMapperConfigDefault<Q, P>(this).parameterName(parameterName);
		this.parameterMappers.put(parameterName, parameterMapperConfig);
		return parameterMapperConfig;
	}
	
	@Override
	public ParameterMapperConfig<Q, P> addParameter() {
		if (this.parameterMappers.size() > 0) {
			if (this.parameterMappers.size() == 1) {
				throw new IllegalStateException(
						"There is already a parameter. Use addParameter(String parameterName) to add a specific parameter or modifyParameter() to modify the existing parameter.");
			}
			throw new IllegalStateException(
					"There are already parameters. Use addParameter(String parameterName) to add a specific parameter.");
		}
		return this.addParameter(this.filterPrp);
	}
	
	@Override
	public PropertyMapperConfig<Q, P> removeParameter(String parameterName) {
		if (parameterName == null) {
			throw new IllegalArgumentException("parameterName cannot be null");
		}
		this.parameterMappers.remove(parameterName);
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, P> modifyParameter(String parameterName) {
        return this.parameterMappers.get(parameterName);
	}
	
	@Override
	public ParameterMapperConfig<Q, P> modifyParameter() {
		if (this.parameterMappers.size() > 1) {
			throw new IllegalStateException(
					"There are multiple parameters. Use modifyParameter(String parameterName) to modify a specific parameter.");
		}
		return this.parameterMappers.values().iterator().next();
	}
	
	@Override
	public ParameterMapperConfig<Q, P> updateParameter(ParameterMapperConfig<Q, P> parameterMapperConfig) {
        Optional<String> oldParameterName = this.parameterMappers.entrySet().stream()
                .filter(entry -> entry.getValue() == parameterMapperConfig)
                .map(Map.Entry::getKey)
                .findFirst();
        
		if (!oldParameterName.isPresent()) {
			throw new IllegalArgumentException(
					"The parameterMapperConfig instance is not registered in this property mapper.");
		}
        this.parameterMappers.remove(oldParameterName.get());
		
		boolean containsKey = this.parameterMappers.containsKey(parameterMapperConfig.getParameterName());
		ParameterMapperConfig<Q, P> replaced = this.parameterMappers.put(parameterMapperConfig.getParameterName(), parameterMapperConfig);
		if (containsKey && replaced != null && replaced != parameterMapperConfig) {
			throw new IllegalArgumentException(
					"Parameter with name '" + parameterMapperConfig.getParameterName() + "' already exists and is another instance. Use modifyParameter() to modify an existing parameter.");
		}
		this.parameterMappers.put(parameterMapperConfig.getParameterName(), parameterMapperConfig);
		return parameterMapperConfig;
	}
	
	@Override
	public Map<String, ParameterMapperConfig<Q, P>> getParameterMappers() {
		return this.parameterMappers;
	}
	
//	@Override
//	public PropertyMapperConfig<Q, P> unpackListItems(boolean unpackListItems) {
//		this.unpackListItems = unpackListItems;
//		return this;
//	}
	
//	@Override
//	public PropertyMapperConfig<Q, P> repeater(boolean repeater) {
//		this.repeater = repeater;
//		return this;
//	}
	
	@Override
	public String getFilterPrp() {
		return filterPrp;
	}
		
//	@Override
//	public String getParameterName() {
//		if (this.parameterName == null) {
//			return this.filterPrp;
//		}
//		return parameterName;
//	}
	
	@Override
	public ParticipatesInQuery getParticipatesInQuery() {
		return participatesInQuery;
	}
	
	@Override
	public AssignNamedParameter<Q, P> getOnParticipatesNamed() {
		return onParticipatesNamed;
	}
	
	@Override
	public AssignPositionalParameter<Q, P> getOnParticipatesPositional() {
		return onParticipatesPositional;
	}

	@Override
	public String toString() {
		return "PropertyMapperConfig [filterPrp=" + getFilterPrp() + ", parameters=" + getParameterMappers() + "]";
	}
	
//	@Override
//	public boolean isUnpackListItems() {
//		return unpackListItems;
//	}
//	
//	@Override
//	public boolean isRepeater() {
//		return repeater;
//	}
}