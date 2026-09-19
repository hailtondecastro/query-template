package io.github.querytemplate;

import java.lang.reflect.Method;

/**
 * Reads a bean property value through its getter using reflection.
 * Replaces the C# {@code PreenchedorPrps.GetProperty}.
 */
public class PropertyUtils {

    private PropertyUtils() {
    }

    /**
     * Returns the value of the property named {@code propertyName} of {@code obj}
     * by invoking the corresponding getter (e.g., "name" -&gt; "getName" or "active" -&gt; "isActive").
     *
     * @param obj          the object to read the property from.
     * @param propertyName the property name.
     * @return the property value.
     * @throws IllegalArgumentException if the property or its getter method cannot be found.
     * @throws RuntimeException         if an error occurs during method invocation.
     */
    public static Object getProperty(Object obj, String propertyName) {
        String capitalized = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        
        String getterName = "get" + capitalized;
        String isName = "is" + capitalized;

        Method method = null;
        
        try {
            // First, try the standard "get" prefix
            method = obj.getClass().getMethod(getterName);
        } catch (NoSuchMethodException e) {
            try {
                // Fallback to the "is" prefix for boolean properties
                method = obj.getClass().getMethod(isName);
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException("Property, getter, or is-getter not found: " + propertyName, ex);
            }
        }

        try {
            return method.invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException("Error while accessing property: " + propertyName, e);
        }
    }
}