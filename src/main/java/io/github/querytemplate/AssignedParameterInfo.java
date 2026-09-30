package io.github.querytemplate;

public interface AssignedParameterInfo<P> {

	P getValue();

	/**
	 * The name of the parameter as it was used into the {@link QueryTemplate}.
	 * @return The name of the parameter as it was used into the {@link QueryTemplate}.
	 */
	String getName();

	/**
	 * The unpacked/repeated name is the name of the parameter when it is unpacked or repeated in the query.
	 * @return The unpacked/repeated name of the parameter or null if the parameter is not unpacked or repeated.
	 */
	String getUnpackedRepeatedName();

	/**
	 * The index of the unpacked/repeated parameter in the source list.
	 * It is null if the parameter is not unpacked or repeated.
	 * @return The index of the unpacked/repeated parameter in the source list or null if the parameter is not unpacked or repeated. This is always zero-based index.
	 */
	Integer getIndex();

	/**
	 * The position of the parameter in the query. It is null if the parameter is
	 * query use named parameters.
	 * 
	 * @return Parameter position in the query or null if the parameter is not positional.
	 */
	Integer getPosition();

}