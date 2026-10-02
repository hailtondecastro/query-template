package io.github.querytemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

class PropertyMapperConfigDefault<Q, F, P, I> implements PropertyMapperConfigInternal<Q, F, P, I>, PropertyMapperConfig<Q, F, P, I> {
	QueryTemplateConfig<Q, F> queryTemplateConfig;
	
	PropertyMapperConfigDefault(QueryTemplateConfig<Q, F> queryTemplateConfig) {
		this.parameterMappers = new LinkedHashMap<>();
		this.queryTemplateConfig = queryTemplateConfig;
	}
	
	@Override
	public QueryTemplateConfig<Q, F> done() {
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
    
    private Map<String, ParameterMapperConfig<Q, F, P, I>> parameterMappers;
    
	@Override
	public PropertyMapperConfig<Q, F, P, I> filterPrp(String filterPrp) {
		this.filterPrp = filterPrp;
		return this;
	}
	
//	@Override
//	public PropertyMapperConfig<Q, F, P, I> parameterName(String parameterName) {
//		this.parameterName = parameterName;
//		return this;
//	}
	
	@Override
	public PropertyMapperConfig<Q, F, P, I> participatesInQuery(ParticipatesInQuery participatesInQuery) {
		this.participatesInQuery = participatesInQuery;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, F, P, I> onParticipatesNamed(AssignNamedParameter<Q, P> onParticipatesNamed) {
		this.onParticipatesNamed = onParticipatesNamed;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, F, P, I> onParticipatesPositional(AssignPositionalParameter<Q, P> onParticipatesPositional) {
		this.onParticipatesPositional = onParticipatesPositional;
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> addParameter(String parameterName) {
		boolean containsKey = this.parameterMappers.containsKey(parameterName);
		if (containsKey) {
			throw new IllegalArgumentException(
					"Parameter with name '" + parameterName + "' already exists and is another instance. Use modifyParameter() to modify an existing parameter.");
		}
		
		ParameterMapperConfig<Q, F, P, I> parameterMapperConfig = new ParameterMapperConfigDefault<Q, F, P, I>(this).parameterName(parameterName);
		this.parameterMappers.put(parameterName, parameterMapperConfig);
		return parameterMapperConfig;
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> addParameter() {
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
	public PropertyMapperConfig<Q, F, P, I> removeParameter(String parameterName) {
		if (parameterName == null) {
			throw new IllegalArgumentException("parameterName cannot be null");
		}
		this.parameterMappers.remove(parameterName);
		return this;
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> modifyParameter(String parameterName) {
        return this.parameterMappers.get(parameterName);
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> modifyParameter() {
		if (this.parameterMappers.size() > 1) {
			throw new IllegalStateException(
					"There are multiple parameters. Use modifyParameter(String parameterName) to modify a specific parameter.");
		}
		return this.parameterMappers.values().iterator().next();
	}
	
	@Override
	public ParameterMapperConfig<Q, F, P, I> updateParameter(ParameterMapperConfig<Q, F, P, I> parameterMapperConfig) {
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
		ParameterMapperConfig<Q, F, P, I> replaced = this.parameterMappers.put(parameterMapperConfig.getParameterName(), parameterMapperConfig);
		if (containsKey && replaced != null && replaced != parameterMapperConfig) {
			throw new IllegalArgumentException(
					"Parameter with name '" + parameterMapperConfig.getParameterName() + "' already exists and is another instance. Use modifyParameter() to modify an existing parameter.");
		}
		this.parameterMappers.put(parameterMapperConfig.getParameterName(), parameterMapperConfig);
		return parameterMapperConfig;
	}
	
	@Override
	public Map<String, ParameterMapperConfig<Q, F, P, I>> getParameterMappers() {
		return this.parameterMappers;
	}
	
//	@Override
//	public PropertyMapperConfig<Q, F, P, I> unpackListItems(boolean unpackListItems) {
//		this.unpackListItems = unpackListItems;
//		return this;
//	}
	
//	@Override
//	public PropertyMapperConfig<Q, F, P, I> repeater(boolean repeater) {
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

	@Override
	public PropertyMapperConfig<Q, F, I, P> switchType() {
		return (PropertyMapperConfig<Q, F, I, P>) this;
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