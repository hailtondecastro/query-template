package io.github.querytemplate;


class PropertyMapperConfigDefault<Q, P> implements PropertyMapperConfig<Q, P> {
	QueryTemplateConfig<Q> queryTemplateConfig;
	
	PropertyMapperConfigDefault(QueryTemplateConfig<Q> queryTemplateConfig) {
		this.queryTemplateConfig = queryTemplateConfig;
	}
	
	@Override
	public QueryTemplateConfig<Q> done() {
		return this.queryTemplateConfig;
	}
	
    private String filterPrp;
    //Not used
    //private String entityPrp;
    private FillVerifier fillVerifier;
    //private Type type = null;
    private AssignNamedParameter<Q, P> onFilledNamed = null;
    private AssignPositionalParameter<Q, P> onFilledPositional = null;
    //private boolean parameterAsList = false;
    private boolean unpackListItems = false;
    private boolean repeater = false;
    
	@Override
	public PropertyMapperConfig<Q, P> filterPrp(String filterPrp) {
		this.filterPrp = filterPrp;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, P> fillVerifier(FillVerifier fillVerifier) {
		this.fillVerifier = fillVerifier;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, P> onFilled(AssignNamedParameter<Q, P> onFilled) {
		this.onFilledNamed = onFilled;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, P> onFilled(AssignPositionalParameter<Q, P> onFilled) {
		this.onFilledPositional = onFilled;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, P> unpackListItems(boolean unpackListItems) {
		this.unpackListItems = unpackListItems;
		return this;
	}
	
	@Override
	public PropertyMapperConfig<Q, P> repeater(boolean repeater) {
		this.repeater = repeater;
		return this;
	}
	
	@Override
	public String getFilterPrp() {
		return filterPrp;
	}
	
	@Override
	public FillVerifier getFillVerifier() {
		return fillVerifier;
	}
	
	@Override
	public AssignNamedParameter<Q, P> getOnFilledNamed() {
		return onFilledNamed;
	}
	
	@Override
	public AssignPositionalParameter<Q, P> getOnFilledPositional() {
		return onFilledPositional;
	}
	
	@Override
	public boolean isUnpackListItems() {
		return unpackListItems;
	}
	
	@Override
	public boolean isRepeater() {
		return repeater;
	}
}