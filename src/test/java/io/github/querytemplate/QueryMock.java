package io.github.querytemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock of {@link Query} for unit testing. Records every {@code setParameter} /
 * {@code setParameterList} invocation so the test can assert on them. It is built
 * with a dynamic {@link Proxy} to avoid implementing the whole {@link Query}
 * contract.
 */
public class QueryMock<E> implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(QueryMock.class);

    private final List<String> parameterCalls = new ArrayList<>();
    private final Query<E> proxy;

    @SuppressWarnings("unchecked")
	public QueryMock() {
        this.proxy = (Query<E>) Proxy.newProxyInstance(
                QueryMock.class.getClassLoader(),
                new Class<?>[] { Query.class },
                this);
    }

    /**
     * @return the proxy implementing {@link Query}.
     */
    public Query<E> getQuery() {
        return this.proxy;
    }

    /**
     * @return the recorded {@code setParameter}/{@code setParameterList} calls.
     */
    public List<String> getParameterCalls() {
        return this.parameterCalls;
    }

    @Override
    public Object invoke(Object proxyObj, Method method, Object[] args) {
        String name = method.getName();
        if (name.startsWith("setParameter")) {
            String call = name + Arrays.toString(args);
            this.parameterCalls.add(call);
            LOG.debug(call);
        }
        if (Query.class.isAssignableFrom(method.getReturnType())) {
            return this.proxy;
        }
        if ("toString".equals(name)) {
            return "QueryMock";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(this.proxy);
        }
        if ("equals".equals(name)) {
            return this.proxy == (args != null ? args[0] : null);
        }
        return null;
    }
}
