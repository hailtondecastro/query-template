package io.github.querytemplate;

/**
 * Interface for running scripts with bindings. It allows to bind variables, clear bindings, and evaluate scripts.
 */
public interface EvalRunner {
	Object binding(QueryTemplateState<?, ?> preliminaryState, String name, Object value) throws Throwable;
	void clearBindings(QueryTemplateState<?, ?> preliminaryState) throws Throwable;
	Object eval(QueryTemplateState<?, ?> preliminaryState, String script) throws Throwable;
}
