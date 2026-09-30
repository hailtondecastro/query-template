package io.github.querytemplate;

import java.lang.reflect.Method;
import java.util.Map;

import javax.script.Bindings;

/**
 * EvalRunner implementation for JSR-233 (Java Scripting API) engines, it is provided by {@link JSR233EvalRunnerCreator}.<br>
 * @see JSR233EvalRunnerCreator
 */
public final class JSR233EvalRunner implements EvalRunner {
	private Bindings bindings;
	private JSR233EvalRunnerCreator creator;
	
	public JSR233EvalRunner(
		JSR233EvalRunnerCreator creator,
		Bindings bindings) {
		super();
		this.creator = creator;
		this.bindings = bindings;
	}
	
	@Override
	public <Q> Object binding(QueryTemplateState<Q> preliminarState,
		String name,
		Object value) throws Throwable {
		Object processedValue = value;
		
		if (this.bindings.getClass().getName().equals("com.oracle.truffle.js.scriptengine.GraalJSBindings")) {
			if (value instanceof Map<?, ?>) {
				Class<?> proxyObjectClass = Class.forName("org.graalvm.polyglot.proxy.ProxyObject");
				Method fromMapMethod = proxyObjectClass.getMethod("fromMap", Map.class);
				processedValue = fromMapMethod.invoke(null, value);
				//processedValue = org.graalvm.polyglot.proxy.ProxyObject.fromMap((Map<String, Object>) value);
			}
		}
		
		return this.bindings.put(name, processedValue);
	}
	@Override
	public <Q> void clearBindings(QueryTemplateState<Q> preliminarState) throws Throwable {
		this.bindings.clear();
	}
	@Override
	public <Q> Object eval(QueryTemplateState<Q> preliminarState,
		String script) throws Throwable {
		return this.creator.eval(preliminarState, script, this.bindings);
	}
}