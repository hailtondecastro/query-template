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
     * by invoking the corresponding getter (e.g. "name" -&gt; "getName").
     *
     * @param obj          the object to read the property from.
     * @param propertyName the property name.
     * @return the property value.
     */
    public static Object getProperty(Object obj, String propertyName) {
        try {
            // Converts the field name to the getter convention (e.g. "name" -> "getName").
            String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            Method method = obj.getClass().getMethod(getterName);
            return method.invoke(obj);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Property or getter not found: " + propertyName, e);
        } catch (Exception e) {
            // Catches IllegalAccessException or InvocationTargetException.
            throw new RuntimeException("Error while accessing property " + propertyName, e);
        }
    }
}
