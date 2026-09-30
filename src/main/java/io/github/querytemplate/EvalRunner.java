package io.github.querytemplate;

/**
 * Interface for running scripts with bindings. It allows to bind variables, clear bindings, and evaluate scripts.
 */
public interface EvalRunner {
	<Q> Object binding(QueryTemplateState<Q> preliminarState, String name, Object value) throws Throwable;
	<Q> void clearBindings(QueryTemplateState<Q> preliminarState) throws Throwable;
	<Q> Object eval(QueryTemplateState<Q> preliminarState, String script) throws Throwable;
}
