package io.github.querytemplate;

/**
 * Internal configuration interface for a property mapper. It extends the public
 * PropertyMapperConfig interface and adds methods for managing parameters.
 * 
 * @param <Q> Plataform-specific query type (e.g., String for SQL, CriteriaQuery
 *            for JPA, etc.). It is not used internally, it is only for strong
 *            typing and IDE code completion.
 * @param <P> The type of the property to be mapped. It is not used internally,
 *            it is only for strong typing and IDE code completion.
 * @param <I> The type of the items in the collection/list/array property to be mapped. It is not used
 *           internally, it is only for strong typing and IDE code completion.                       
 * @param <F> Filter type. It is used internally to create proxy objects and resolve
 */
public interface PropertyMapperConfigInternal<Q, F, P, I> extends PropertyMapperConfig<Q, F, P, I> {	

	/**
	 * Updates a parameter of the property mapper.<br>
	 * It is necessary because of {@link ParameterMapperConfig#parameterName(String)}
	 * 
	 * @param parameterMapperConfig the configuration of the parameter to update.
	 * @return this instance for method chaining.
	 */
	ParameterMapperConfig<Q, F, P, I> updateParameter(ParameterMapperConfig<Q, F, P, I> parameterMapperConfig);
}