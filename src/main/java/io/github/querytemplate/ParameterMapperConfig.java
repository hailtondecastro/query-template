package io.github.querytemplate;

public interface ParameterMapperConfig<Q, F, P, I> {

	/**
	 * Finishes the configuration of the parameter mapper 
	 * and returns to the property mapper configuration.
	 * 
	 * @return the query template configuration instance for method chaining.
	 */
	PropertyMapperConfig<Q, F, P, I> done();

	/**
	 * Sets the parameter name for the property mapper. If not set, it will default
	 * to the filter property name.
	 * 
	 * @param parameterName the parameter name.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> parameterName(String parameterName);

	/**
	 * Sets whether to unpack list items for the property mapper. If true, the list items will be unpacked and used as individual parameters.
	 * @param unpackListItems whether to unpack list items.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> unpackListItems(boolean unpackListItems);

	/**
	 * Sets whether the property mapper is a repeater. If true, the property mapper will be repeated for each item in the list.
	 * @param repeater whether the property mapper is a repeater.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> repeater(boolean repeater);
	
	/**
	 * Sets the named parameter callback for the property parameter mapper.<br>
	 * This will be called when the property is participating in the query, just for this parameter and if this is used in the query.
	 * @param onParticipatesNamed the named parameter callback.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> onParticipatesNamed(
		AssignNamedParameter<Q, P> onParticipatesNamed);

	/**
	 * Sets the positional parameter callback for the property parameter mapper.<br>
	 * This will be called when the property is participating in the query, just for this parameter and if this is used in the query.
	 *  
	 * @param onParticipatesPositional the positional parameter callback.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> onParticipatesPositional(
		AssignPositionalParameter<Q, P> onParticipatesPositional);
	
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
	
	/**
	 * Gets the named parameter callback for the parameter property mapper.
	 * @return the named parameter callback.
	 */
	AssignNamedParameter<Q, P> getOnParticipatesNamed();

	/**
	 * Gets the positional parameter callback for the parameter property mapper.
	 * @return the positional parameter callback.
	 */
	AssignPositionalParameter<Q, P> getOnParticipatesPositional();

}