package io.github.querytemplate.proxy;

import java.util.function.BiFunction;

import io.github.querytemplate.QueryTemplateConfig;

@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface ProxyFactoryCreator extends BiFunction<QueryTemplateConfig, Class, ProxyFactory> {
}
