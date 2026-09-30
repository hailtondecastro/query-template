package io.github.querytemplate;

/**
 * Information aboult a parameter assigned the {@link QueryTemplateState}.
 * 
 * @param <P> The type of the parameter value.
 */
public class AssignedParameterInfoDefault<P> implements AssignedParameterInfoInternal<P> {
	private String name;
	private String unpackedRepeatedName;
	private Integer index;
	private Integer position;
	private P value;
	
	protected void setValue(P value) {
		this.value = value;
	}
	
	@Override
	public P getValue() {
		return value;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public void setUnpackedRepeatedName(String unpackedRepeatedName) {
		this.unpackedRepeatedName = unpackedRepeatedName;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getUnpackedRepeatedName() {
		return unpackedRepeatedName;
	}

	@Override
	public Integer getIndex() {
		return index;
	}
	
	@Override
	public  void setIndex(Integer index) {
		this.index = index;
	}

	@Override
	public Integer getPosition() {
		return position;
	}
	
	@Override
	public void setPosition(Integer position) {
		this.position = position;
	}
}
