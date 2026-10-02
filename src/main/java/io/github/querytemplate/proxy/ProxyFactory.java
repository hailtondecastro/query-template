package io.github.querytemplate.proxy;

/**
 * javassist.util.proxy.ProxyFactory base interface to avoid direct dependency on javassist library. This interface defines methods for setting the superclass and interfaces for a proxy class, allowing for dynamic proxy creation without relying on a specific library implementation.<br>  
 * 
 * Factory class for creating proxy instances. This class can be used to generate dynamic proxies for interfaces, allowing method calls to be intercepted and handled by a specified invocation handler.<br>
 * 
 */
@SuppressWarnings("rawtypes")
public interface ProxyFactory {
	void setSuperclass(Class clazz);
	void setInterfaces(Class[] ifs);
	Object create(Class[] paramTypes, Object[] args, MethodHandler mh) throws Throwable;
	void setFilter(MethodFilter mf);
}
