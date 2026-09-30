package io.github.querytemplate;

public interface ParameterMapperConfig<Q, P> {

	/**
	 * Finishes the configuration of the parameter mapper 
	 * and returns to the property mapper configuration.
	 * 
	 * @return the query template configuration instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> done();

	/**
	 * Sets the parameter name for the property mapper. If not set, it will default
	 * to the filter property name.
	 * 
	 * @param parameterName the parameter name.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, P> parameterName(String parameterName);

	/**
	 * Sets whether to unpack list items for the property mapper. If true, the list items will be unpacked and used as individual parameters.
	 * @param unpackListItems whether to unpack list items.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, P> unpackListItems(boolean unpackListItems);

	/**
	 * Sets whether the property mapper is a repeater. If true, the property mapper will be repeated for each item in the list.
	 * @param repeater whether the property mapper is a repeater.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, P> repeater(boolean repeater);
	
	/**
	 * Gets the parameter name. If not set, it will default to the filter property name.
	 * 
	 * @return the parameter name.
	 */
	String getParameterName();

	/**
	 * Gets whether to unpack list items for the property mapper.
	 * @return true if unpacking list items, false otherwise.
	 */
	boolean isUnpackListItems();

	/**
	 * Gets whether the property mapper is a repeater.
	 * @return true if the property mapper is a repeater, false otherwise.
	 */
	boolean isRepeater();

}