package io.github.querytemplate;

/**
 * Configuration class for a property mapper. It allows to set the filter property, fill verifier, parameter callbacks, and other options for the property mapper.
 * @param <Q> Plataform-specific query type (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
 * @param <P> The type of the property to be mapped. It is not used internally, it is only for strong typing and IDE code completion.
 */
public interface PropertyMapperConfig<Q, P> {

	/**
	 * Finishes the configuration of the property mapper and returns to the query template configuration.
	 * 
	 * @return the query template configuration instance for method chaining.
	 */
	QueryTemplateConfig<Q> done();

	/**
	 * Sets the filter property for the property mapper.
	 * @param filterPrp the property name in the filter.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> filterPrp(String filterPrp);

	/**
	 * Sets the parameter name for the property mapper. If not set, it will default
	 * to the filter property name.
	 * 
	 * @param parameterName the parameter name.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> parameterName(String parameterName);
	
	/**
	 * Sets the fill verifier for the property mapper.
	 * @param fillVerifier the fill verifier.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> fillVerifier(FillVerifier fillVerifier);

	/**
	 * Sets the named parameter callback for the property mapper.
	 * @param onFilled the named parameter callback.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> onFilled(
		AssignNamedParameter<Q, P> onFilled);

	/**
	 * Sets the positional parameter callback for the property mapper.
	 * @param onFilled the positional parameter callback.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> onFilled(
		AssignPositionalParameter<Q, P> onFilled);

	/**
	 * Sets whether to unpack list items for the property mapper. If true, the list items will be unpacked and used as individual parameters.
	 * @param unpackListItems whether to unpack list items.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> unpackListItems(boolean unpackListItems);

	/**
	 * Sets whether the property mapper is a repeater. If true, the property mapper will be repeated for each item in the list.
	 * @param repeater whether the property mapper is a repeater.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> repeater(boolean repeater);

	/**
	 * Gets the filter property for the property mapper.
	 * @return the filter property name.
	 */
	String getFilterPrp();
	
	/**
	 * Gets the parameter name for the property mapper. If not set, it will default to the filter property name.
	 * 
	 * @return the parameter name.
	 */
	String getParameterName();

	/**
	 * Gets the fill verifier for the property mapper.
	 * @return the fill verifier.
	 */
	FillVerifier getFillVerifier();

	/**
	 * Gets the named parameter callback for the property mapper.
	 * @return the named parameter callback.
	 */
	AssignNamedParameter<Q, P> getOnFilledNamed();

	/**
	 * Gets the positional parameter callback for the property mapper.
	 * @return the positional parameter callback.
	 */
	AssignPositionalParameter<Q, P> getOnFilledPositional();

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