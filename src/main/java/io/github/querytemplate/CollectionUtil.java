package io.github.querytemplate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collection helper utilities.
 */
class CollectionUtil {

    private CollectionUtil() {
    }

    /**
     * Converts an arbitrary value (array, {@link Collection} or {@link Iterable})
     * into a {@link Collection}.
     *
     * @param value the value to convert.
     * @return a collection with the value elements.
     */
    static Collection<Object> toCollection(Object value) {
        List<Object> list = new ArrayList<>();
        if (value == null) {
            return list;
        }
        if (value instanceof Collection) {
            list.addAll((Collection<?>) value);
        } else if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                list.add(Array.get(value, i));
            }
        } else if (value instanceof Iterable) {
            for (Object o : (Iterable<?>) value) {
                list.add(o);
            }
        } else {
            throw new QueryTemplateException("Value is not an array, Collection or Iterable: " + value);
        }
        return list;
    }

    /**
     * Builds a readable string from an arbitrary value (array, {@link Collection}
     * or {@link Iterable}).
     *
     * @param value the value.
     * @return a readable representation.
     */
    static String toString(Object value) {
        return toCollection(value).toString();
    }
}
