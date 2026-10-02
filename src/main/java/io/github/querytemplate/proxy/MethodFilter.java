package io.github.querytemplate.proxy;

import java.lang.reflect.Method;

/**
 * Copy <code>javassist.util.proxy.MethodFilter</code>.
 */
public interface MethodFilter {
    boolean isHandled(Method m);
}
