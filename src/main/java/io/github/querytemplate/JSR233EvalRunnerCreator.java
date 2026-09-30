package io.github.querytemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Singleton {@link Function}&lt;{@link QueryTemplateState}&lt;?&gt;, {@link EvalRunner}&gt; that creates {@link JSR233EvalRunner} instances.<br>
 * This is necessary if you are using {@link Compilable} {@link ScriptEngine}.<br>
 * {@link JSR233EvalRunner} holds an instance of {@link Bindings} and is keeped alive together with {@link QueryTemplateState}.
 * This is why we need a <code>Function<QueryTemplateState<?>, EvalRunner></code> like
 * {@link JSR233EvalRunnerCreator} as a singleton instance to hold the single {@link ScriptEngine} and various {@link CompiledScript}'s
 * that indirectly run scripts on {@link JSR233EvalRunnerCreator#eval(QueryTemplateState, String, Bindings)}.<br>
 * 
 */
public final class JSR233EvalRunnerCreator implements Function<QueryTemplateState<?>, EvalRunner> {
    public static enum JSR233ScriptMode {
		INTERPRETED,
		COMPILED
	}

	private ScriptEngine scriptEngine;
    private Compilable compilableEngine;
    private JSR233EvalRunnerCreator.JSR233ScriptMode scriptMode = JSR233EvalRunnerCreator.JSR233ScriptMode.INTERPRETED;
    private Map<QueryTemplate<?>, Map<String, CompiledScript>> compiledScriptsByTemplate = new HashMap<>();
    
    public JSR233EvalRunnerCreator(ScriptEngine scriptEngine, JSR233EvalRunnerCreator.JSR233ScriptMode scriptMode) {
		this.scriptEngine = scriptEngine;
		this.scriptMode = scriptMode;
		if (this.scriptMode == JSR233EvalRunnerCreator.JSR233ScriptMode.COMPILED) {
			this.compilableEngine = (Compilable) this.scriptEngine;
		}
    }
    
	@Override
	public EvalRunner apply(QueryTemplateState<?> state) {
		Bindings bindings = this.scriptEngine.createBindings();
		if (this.scriptMode == JSR233EvalRunnerCreator.JSR233ScriptMode.COMPILED) {
			this.compiledScriptsByTemplate.put(state.getTemplateOwner(), new HashMap<>());				
		}
		
		return new JSR233EvalRunner(this, bindings);
	}
	
	public Object eval(QueryTemplateState<?> state, String scriptText, Bindings bindings) throws ScriptException {
		if (this.scriptMode == JSR233EvalRunnerCreator.JSR233ScriptMode.COMPILED) {
			Map<String, CompiledScript> compiledSciptsOfQTemplate = this.compiledScriptsByTemplate.get(state.getTemplateOwner());
			synchronized (state.getTemplateOwner()) {
				if (!compiledSciptsOfQTemplate.containsKey(scriptText)) {
					compiledSciptsOfQTemplate.put(scriptText, this.compilableEngine.compile(scriptText));
				}
			}			
			CompiledScript compiledScript = compiledSciptsOfQTemplate.get(scriptText);
			return compiledScript.eval(bindings);
		} else {
			return this.scriptEngine.eval(scriptText, bindings);
		}
	}
}