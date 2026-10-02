package io.github.querytemplate;

import java.util.Map;

/**
 * Configuration class for a property mapper. It allows to set the filter property, participates-in-query verifier, 
 * parameter name, named and positional parameter callbacks, and other options for the property mapper.
 * @param <Q> Plataform-specific query type (e.g., String for SQL, CriteriaQuery for JPA, etc.). It is not used internally, it is only for strong typing and IDE code completion.
 * @param <P> The type of the property to be mapped. It is not used internally, it is only for strong typing and IDE code completion.
 * @param <F> Filter type. It is used internally to create proxy objects and resolve properties by lambda expressions, used too for strong typing and IDE code completion.
 * @param <I> The type of the items in the property to be mapped. It is not used internally, it is only for strong typing and IDE code completion.
 */
public interface PropertyMapperConfig<Q, F, P, I> {

	/**
	 * Finishes the configuration of the property mapper and returns to the query template configuration.<br>
	 * If there is no parameter mapper configured, it will be created with the default parameter name (same as filter property name).
	 * 
	 * @return the query template configuration instance for method chaining.
	 */
	QueryTemplateConfig<Q, F> done();

	/**
	 * Sets the filter property for the property mapper.
	 * @param filterPrp the property name in the filter.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, F, P, I> filterPrp(String filterPrp);

//	/**
//	 * Sets the parameter name for the property mapper. If not set, it will default
//	 * to the filter property name.
//	 * 
//	 * @param parameterName the parameter name.
//	 * @return this instance for method chaining.
//	 */
//	PropertyMapperConfig<Q, F, P, I> parameterName(String parameterName);
	
	/**
	 * Adds a parameter to the property mapper.
	 * 
	 * @param parameterName the name of the parameter to add.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> addParameter(String parameterName);
	
//	ParameterMapperConfig<Q, F, P, I> addParameterC(String parameterName);
//	ParameterMapperConfig<Q, F, P, I> addParameterL(String parameterName);
//	ParameterMapperConfig<Q, F, P, I> addParameterA(String parameterName);
	
	/**
	 * Adds a parameter to the property mapper with the same name as the filter property.
	 * 
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> addParameter();

	/**
	 * Removes a parameter from the property mapper.
	 * 
	 * @param parameterName the name of the parameter to remove.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, F, P, I> removeParameter(String parameterName);
	
	/**
	 * Modifies a parameter of the property mapper.<br>
	 * Removes the parameter and returns a ParameterMapperConfig instance for modifying it.<br>
	 * The parameter will be re-added to the property mapper when the ParameterMapperConfig.done() method is called.
	 * 
	 * @param parameterName the name of the parameter to modify.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> modifyParameter(String parameterName);
	
//	<I> ParameterMapperConfig<Q, F, I> configAsListItem(Function<F, Collection<I>> collItems);
//	<I> ParameterMapperConfig<Q, F, I> configAsCollItem(Function<F, List<I>> listItems);
//	<C> ParameterMapperConfig<Q, F, C> configAsCollection(Function<F, C> coll);
//	<L> ParameterMapperConfig<Q, F, C> configAsList(Function<F, L> coll);
	
//	ParameterMapperConfig<Q, F, P, I> modifyParameterC(String parameterName, Function<F, Collection<P>> filterPrp);
//	ParameterMapperConfig<Q, F, P, I> modifyParameterL(String parameterName, Function<F, List<P>> filterPrp);
//	ParameterMapperConfig<Q, F, P, I> modifyParameterA(String parameterName, Function<F, P[]> filterPrp);
	
	/**
	 * Modifies a parameter of the property mapper. If there is only one parameter, it will be modified. If there are multiple parameters, an exception will be thrown.<br>
	 * 
	 * 
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> modifyParameter();

	/**
	 * Gets the parameter mappers for the property mapper.
	 * 
	 * @return a map of parameter names to parameter mapper configurations.
	 */
	Map<String, ParameterMapperConfig<Q, F, P, I>> getParameterMappers();
	
	/**
	 * Sets the participates-in-query verifier for the property mapper.
	 * 
	 * @param participatesInQuery the participates-in-query verifier.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, F, P, I> participatesInQuery(ParticipatesInQuery participatesInQuery);

	/**
	 * Sets the named parameter callback for the property mapper.
	 * This will be called when the property is participating in the query for all parameters that is used in the query.
	 * @param onParticipatesNamed the named parameter callback.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, F, P, I> onParticipatesNamed(
		AssignNamedParameter<Q, P> onParticipatesNamed);

	/**
	 * Sets the positional parameter callback for the property mapper.<br>
	 * This will be called when the property is participating in the query for all parameters that is used in the query.
	 * 
	 * @param onParticipatesPositional the positional parameter callback.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, F, P, I> onParticipatesPositional(
		AssignPositionalParameter<Q, P> onParticipatesPositional);

//	/**
//	 * Sets whether to unpack list items for the property mapper. If true, the list items will be unpacked and used as individual parameters.
//	 * @param unpackListItems whether to unpack list items.
//	 * @return this instance for method chaining.
//	 */
//	PropertyMapperConfig<Q, F, P, I> unpackListItems(boolean unpackListItems);

//	/**
//	 * Sets whether the property mapper is a repeater. If true, the property mapper will be repeated for each item in the list.
//	 * @param repeater whether the property mapper is a repeater.
//	 * @return this instance for method chaining.
//	 */
//	PropertyMapperConfig<Q, F, P, I> repeater(boolean repeater);

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

	/**
	 * Switches the collection property mapper type the its item type, so it can be used to configure the item type instead of the collection type.<br>
	 * It is not used internally, it is only for strong typing and IDE code completion.
	 * @return this instance for method chaining.
	 */
	PropertyMapperConfig<Q, F, I, P> switchType();
	
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