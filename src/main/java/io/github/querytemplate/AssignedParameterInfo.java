package io.github.querytemplate;

public interface AssignedParameterInfo<P> {

	P getValue();

	/**
	 * The name of the parameter as it was used into the {@link QueryTemplate}.
	 * @return
	 */
	String getName();

	/**
	 * The unpacked/repeated name is the name of the parameter when it is unpacked or repeated in the query.
	 * @return
	 */
	String getUnpackedRepeatedName();

	/**
	 * The index of the unpacked/repeated parameter in the source list.
	 * It is null if the parameter is not unpacked or repeated.
	 * @return
	 */
	Integer getIndex();

	/**
	 * The position of the parameter in the query. It is null if the parameter is
	 * query use named parameters.
	 * 
	 * @return
	 */
	Integer getPosition();

}