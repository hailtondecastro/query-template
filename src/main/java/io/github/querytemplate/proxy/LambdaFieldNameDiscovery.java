package io.github.querytemplate.proxy;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal class to discover field and method names using lambda expressions and dynamic proxies.
 */
@SuppressWarnings("rawtypes")
public class LambdaFieldNameDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(LambdaFieldNameDiscovery.class);

	private static transient WeakReference<ConcurrentHashMap<Class, Object>> proxyInstancesMapWR;
    
    @SuppressWarnings("unchecked")
	private static <T> T getProxyInstance(ProxyFactory proxyFactory, Class<T> targetClass) {
        ConcurrentHashMap<Class, Object> proxyInstancesMap = null;
        if (proxyInstancesMapWR == null || proxyInstancesMapWR.get() == null) {
            proxyInstancesMap = new ConcurrentHashMap<Class, Object>();
            proxyInstancesMapWR = new WeakReference<ConcurrentHashMap<Class, Object>>(proxyInstancesMap);
        } else {
            proxyInstancesMap = proxyInstancesMapWR.get();
        }
        if (!proxyInstancesMap.containsKey(targetClass)) {
            if (!targetClass.isInterface())
            	proxyFactory.setSuperclass(targetClass);
            else
            	proxyFactory.setInterfaces(new Class[]{ targetClass });
            proxyFactory.setFilter(methodFilter);
            Object proxy;
            try {
                proxy = proxyFactory.create(new Class<?>[0], new Object[0], new MethodHandlerDefault(proxyFactory));
            } catch (Throwable e) {
                throw new RuntimeException("Erro ao criar proxy", e);
            }
            proxyInstancesMap.put(targetClass, proxy);
        }
        return (T) proxyInstancesMap.get(targetClass);
    }
    
    
//    private enum ElementType {
//        FIELD_BY_GET,
//        METHOD,
//        METHOD_FULL_NAME,
//    }
    
    public static <T> String fieldByGetMethod(ProxyFactory proxyFactory, Function<T, ? extends Object> callback, Class<? extends T> targetClass) {
        T beanProxy = getProxyInstance(proxyFactory, targetClass);
        currentCallChainTD.set(null);
        
        callback.apply(beanProxy);
        
        if (logger.isTraceEnabled())
        	logger.trace("returning 'fildByGetMethod': " + currentCallChainTD.get().callPath());
        String result = currentCallChainTD.get().callPath();
        currentCallChainTD.set(null);
        return result;
    }
    
    public static <T> CallChainInfo fieldByGetMethodChainInfo(ProxyFactory proxyFactory, Function<T, ? extends Object> callback, Class<? extends T> targetClass) {
        T beanProxy = getProxyInstance(proxyFactory, targetClass);
        currentCallChainTD.set(null);
        
        callback.apply(beanProxy);
        
        if (logger.isTraceEnabled())
        	logger.trace("returning 'fildByGetMethod': " + currentCallChainTD.get().callPath());
        String result = currentCallChainTD.get().callPath();
        CallChainInfo callChainInfo = currentCallChainTD.get();
        currentCallChainTD.set(null);
        return callChainInfo;
    }
    
    public static class CallInfo {
		private String fieldName;
		private Method method;
		public CallInfo(
			String fieldName,
			Method method) {
			super();
			this.fieldName = fieldName;
			this.method = method;
		}
		
		@Override
		public String toString() {
			return "CallRingInfo [fieldName=" + fieldName + ", method=" + method + "]";
		}
		public String getFieldName() {
			return fieldName;
		}
		public Method getMethod() {
			return method;
		}
    }
    
    public static class CallChainInfo {
    	private List<CallInfo> callInfos = new java.util.ArrayList<>();
		@Override
		public String toString() {
			return "CallChainInfo [callInfos=" + callInfos + "]";
		}
		
		public String callPath() {
			// Source - https://stackoverflow.com/a/63610480
			// Posted by Joachim Sauer, modified by community. See post 'Timeline' for change history
			// Retrieved 2026-10-01, License - CC BY-SA 4.0
			String callPath = this.callInfos.stream().map(ringInfo -> ringInfo.fieldName)
					.collect(Collectors.joining("."));
			return callPath;
		}
		public Class<?> lastMethodReturnType() {
            if (this.callInfos.isEmpty())
                return null;
            return this.callInfos.get(this.callInfos.size() - 1).method.getReturnType();
        }
		public void add(CallInfo callInfo) {
			this.callInfos.add(callInfo);
		}
		public List<CallInfo> getCallInfos() {
			return Collections.unmodifiableList(this.callInfos);
		}		
    }
    
    static Pattern fieldFromMethodPattern = Pattern.compile("^(is|get)(.)(.*)$");
    static ThreadLocal<CallChainInfo> currentCallChainTD = new ThreadLocal<CallChainInfo>();
    static MethodFilter methodFilter = new MethodFilter() {
        @Override
        public boolean isHandled(Method m) {
            // TODO Auto-generated method stub
            return true;
        }
    };
    private static class MethodHandlerDefault implements MethodHandler{
    	private ProxyFactory proxyFactory;

		public MethodHandlerDefault(
			ProxyFactory proxyFactory) {
			super();
			this.proxyFactory = proxyFactory;
		}

		@SuppressWarnings("unused")
		public ProxyFactory getProxyFactory() {
			return proxyFactory;
		}
		
        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed,
                Object[] args) throws Throwable {
                Matcher matcher = fieldFromMethodPattern
                        .matcher(thisMethod.getName());
            if (matcher.matches()) {
            	if (currentCallChainTD.get() != null) {
            		//lastFieldCallChainTD.set(new ArrayList<>());
            	} else {
            		currentCallChainTD.set(new CallChainInfo());
            	}
            	String fieldName = matcher.group(2).toLowerCase() + matcher.group(3);
            	currentCallChainTD.get().add(new CallInfo(fieldName, thisMethod));
            } else {
                throw new RuntimeException(
                		"Only getter methods can be called. Method called: "
                		//"Somente os metodos get podem ser chamados. Metodo chamado: "
                                + thisMethod.getName());
            }
            if (!Modifier
                    .isFinal(thisMethod.getReturnType().getModifiers())
                    && (thisMethod.getReturnType().isInterface()
                            || hasParameterlessPublicConstructor(
                                    thisMethod.getReturnType()))) {
                return getProxyInstance(this.proxyFactory, thisMethod.getReturnType());
            }
            
            return null;
        }
    }
    
    private static boolean hasParameterlessPublicConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            // In Java 7-, use getParameterTypes and check the length of the array returned
            if (constructor.getParameterCount() == 0) { 
                return true;
            }
        }
        return false;
    }
}
