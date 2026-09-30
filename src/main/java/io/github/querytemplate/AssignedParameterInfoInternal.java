package io.github.querytemplate;

public interface AssignedParameterInfoInternal<P> extends AssignedParameterInfo<P> {

	void setName(String name);

	void setUnpackedRepeatedName(String unpackedRepeatedName);

	void setIndex(Integer index);

	void setPosition(Integer position);

}