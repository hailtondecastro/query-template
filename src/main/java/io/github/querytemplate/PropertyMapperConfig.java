package io.github.querytemplate;

import java.util.Map;

/**
 * Configuration class for a property mapper. It allows to set the filter property, participates-in-query verifier, 
 * parameter name, named and positional parameter callbacks, and other options for the property mapper.
 * @param <Q> Plataform-specific query type (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
 * @param <P> The type of the property to be mapped. It is not used internally, it is only for strong typing and IDE code completion.
 */
public interface PropertyMapperConfig<Q, P> {

	/**
	 * Finishes the configuration of the property mapper and returns to the query template configuration.<br>
	 * If there is no parameter mapper configured, it will be created with the default parameter name (same as filter property name).
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

//	/**
//	 * Sets the parameter name for the property mapper. If not set, it will default
//	 * to the filter property name.
//	 * 
//	 * @param parameterName the parameter name.
//	 * @return this instance for method chaining.
//	 */
//	PropertyMapperConfig<Q, P> parameterName(String parameterName);
	
	/**
	 * Adds a parameter to the property mapper.
	 * 
	 * @param parameterName the name of the parameter to add.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, P> addParameter(String parameterName);
	
	/**
	 * Adds a parameter to the property mapper with the same name as the filter property.
	 * 
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, P> addParameter();

	/**
	 * Removes a parameter from the property mapper.
	 * 
	 * @param parameterName the name of the parameter to remove.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> removeParameter(String parameterName);
	
	/**
	 * Modifies a parameter of the property mapper.<br
	 * Removes the parameter and returns a ParameterMapperConfig instance for modifying it.<br>
	 * The parameter will be re-added to the property mapper when the ParameterMapperConfig.done() method is called.
	 * 
	 * @param parameterName the name of the parameter to modify.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, P> modifyParameter(String parameterName);
	
	/**
	 * Modifies a parameter of the property mapper. If there is only one parameter, it will be modified. If there are multiple parameters, an exception will be thrown.<br>
	 * 
	 * 
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, P> modifyParameter();

	/**
	 * Gets the parameter mappers for the property mapper.
	 * 
	 * @return a map of parameter names to parameter mapper configurations.
	 */
	Map<String, ParameterMapperConfig<Q, P>> getParameterMappers();
	
	/**
	 * Sets the participates-in-query verifier for the property mapper.
	 * 
	 * @param participatesInQuery the participates-in-query verifier.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> participatesInQuery(ParticipatesInQuery participatesInQuery);

	/**
	 * Sets the named parameter callback for the property mapper.
	 * @param onParticipatesNamed the named parameter callback.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> onParticipatesNamed(
		AssignNamedParameter<Q, P> onParticipatesNamed);

	/**
	 * Sets the positional parameter callback for the property mapper.
	 * 
	 * @param onParticipatesPositional the positional parameter callback.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, P> onParticipatesPositional(
		AssignPositionalParameter<Q, P> onParticipatesPositional);

//	/**
//	 * Sets whether to unpack list items for the property mapper. If true, the list items will be unpacked and used as individual parameters.
//	 * @param unpackListItems whether to unpack list items.
//	 * @return this instance for method chaining.
//	 */
//	PropertyMapperConfig<Q, P> unpackListItems(boolean unpackListItems);

//	/**
//	 * Sets whether the property mapper is a repeater. If true, the property mapper will be repeated for each item in the list.
//	 * @param repeater whether the property mapper is a repeater.
//	 * @return this instance for method chaining.
//	 */
//	PropertyMapperConfig<Q, P> repeater(boolean repeater);

	/**
	 * Gets the filter property for the property mapper.
	 * @return the filter property name.
	 */
	String getFilterPrp();
	
//	/**
//	 * Gets the parameter name for the property mapper. If not set, it will default to the filter property name.
//	 * 
//	 * @return the parameter name.
//	 */
//	String getParameterName();

	/**
	 * Gets the participates-in-query verifier for the property mapper.
	 * 
	 * @return the participates-in-query verifier.
	 */
	ParticipatesInQuery getParticipatesInQuery();

	/**
	 * Gets the named parameter callback for the property mapper.
	 * @return the named parameter callback.
	 */
	AssignNamedParameter<Q, P> getOnParticipatesNamed();

	/**
	 * Gets the positional parameter callback for the property mapper.
	 * @return the positional parameter callback.
	 */
	AssignPositionalParameter<Q, P> getOnParticipatesPositional();

//	/**
//	 * Gets whether to unpack list items for the property mapper.
//	 * @return true if unpacking list items, false otherwise.
//	 */
//	boolean isUnpackListItems();

//	/**
//	 * Gets whether the property mapper is a repeater.
//	 * @return true if the property mapper is a repeater, false otherwise.
//	 */
//	boolean isRepeater();

}