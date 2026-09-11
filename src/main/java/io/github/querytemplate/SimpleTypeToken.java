package io.github.querytemplate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A simplified version of the Super Type Token pattern.
 */
public abstract class SimpleTypeToken<T> {
    private final Type type;

    protected SimpleTypeToken() {
        // 1. Get the generic superclass of the current subclass instance
        Type superclass = getClass().getGenericSuperclass();
        
        // 2. Ensure it is actually parameterized (e.g., SimpleTypeToken<List<String>>)
        if (superclass instanceof ParameterizedType) {
            // 3. Extract the actual type arguments passed to the superclass
            this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException("Missing type parameter. Did you forget the '{}'?");
        }
    }

    public Type getType() {
        return this.type;
    }
    
	public Class<T> getRawType() {
		if (type instanceof Class<?>) {
			return (Class<T>) type;
		} else if (type instanceof ParameterizedType) {
			return (Class<T>) ((ParameterizedType) type).getRawType();
		} else {
			throw new IllegalStateException("Type is neither a Class nor a ParameterizedType");
		}
	}
}