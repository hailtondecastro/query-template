package io.github.querytemplate;

/**
 * 
 * @param <T>
 */
class OutputParam<T> {
    public T value;

    public OutputParam(T value) {
        this.value = value;
    }

	T getValue() {
		return value;
	}

	void setValue(T value) {
		this.value = value;
	}
}
