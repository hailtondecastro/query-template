package io.github.querytemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.Runtime.Version;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bsh.Interpreter;
import io.github.querytemplate.MyFilter.FormalName;
import io.github.querytemplate.proxy.MethodFilter;
import io.github.querytemplate.proxy.MethodHandler;
import io.github.querytemplate.proxy.ProxyFactory;
import io.github.querytemplate.proxy.ProxyFactoryCreator;

/**
 * Unit tests demonstrating how to use {@link QueryTemplateDefault}. Java port of the
 * C# {@code Demonstracao} class.
 */
public class QueryTemplateTest {

    private static final Logger LOG = LoggerFactory.getLogger(QueryTemplateTest.class);

	private <P> AssignNamedParameter<Query<MyEntity>, P> simpleOnParticipatesNamed() {
		return (query,
			name,
			value,
			paramInfo) -> query.setParameter(name, value);
	}
	
	private <P> AssignPositionalParameter<Query<MyEntity>, P> simpleOnParticipatesPositional() {
		return (query,
			position,
			value,
			paramInfo) -> query.setParameter(position, value);
	}
	
	private <P> AssignNamedParameter<Query<MyEntity>, P> simpleOnParticipatesNamed(Type type) {
		return (query,
			name,
			value,
			paramInfo) -> query.setParameter(name, value, type);
	}
	
	private <P> AssignPositionalParameter<Query<MyEntity>, P> simpleOnParticipatesPositional(Type type) {
		return (query,
			position,
			value,
			paramInfo) -> query.setParameter(position, value, type);
	}
	
	private <PI> AssignNamedParameter<Query<MyEntity>, Collection<PI>> simpleOnParticipatesListNamed(Type itemType) {
		return (query,
            name,
            value,
			paramInfo) -> query.setParameterList(name, (Collection) value, itemType);
	}
	
	private <PI> AssignPositionalParameter<Query<MyEntity>, Collection<PI>> simpleOnParticipatesListPositional(Type itemType) {
		return (query,
			position,
			value,
			paramInfo) -> query.setParameterList(position, (Collection) value, itemType);
	}
	
    private void configPropertyMappers(QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig) {
    	queryTemplateConfig
    		.clearMappers()
    			.addMapper("filterPrp1", String.class).participatesInQuery(ParticipantCheckers.STRING_NOTEMPTY).onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.STRING)).onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.STRING)).done()                                                                             
    			.addMapper("filterPrp2", String.class).participatesInQuery(ParticipantCheckers.STRING_NOTEMPTY).onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.STRING)).onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.STRING)).done()                                                                             
    			.addMapper("filterPrp3", String.class).participatesInQuery(ParticipantCheckers.STRING_NOTEMPTY).onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.STRING)).onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.STRING)).done()                                                                        
    			.addMapper("filterPrp4", new SimpleTypeToken<Collection<String>>(){}.getRawType()).participatesInQuery(ParticipantCheckers.COLLECTION_NOTEMPTY).onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.STRING)).onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.STRING)).addParameter().unpackListItems(true).done().done()   
    			.addMapper("filterPrp5", String.class).participatesInQuery(ParticipantCheckers.STRING_NOTEMPTY).onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.STRING)).onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.STRING)).done()
    			.addMapper("filterPrp6", MyProperty.class).participatesInQuery(ParticipantCheckers.NOTNULL).onParticipatesNamed(this.simpleOnParticipatesNamed()).onParticipatesPositional(this.simpleOnParticipatesPositional()).done()
    			.addMapper("filterPrp7", new SimpleTypeToken<Collection<String>>(){}.getRawType())
	        		.participatesInQuery(ParticipantCheckers.COLLECTION_NOTEMPTY)
	        		.addParameter().repeater(true).done()
	        		.onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.INTEGER))
	        		.onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.INTEGER))
	        		.done()
	        	.addMapper("filterPrp8", Integer[].class).participatesInQuery(ParticipantCheckers.ARRAY_NOTEMPTY).onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.INTEGER)).onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.INTEGER)).addParameter().repeater(true).done().done()
	        	.addMapper("filterPrp1AndfilterPrp2", Boolean.class).participatesInQuery(ParticipantCheckers.BOOLEAN_TRUE).done()
	        	.addMapper("formalName", FormalName.class).participatesInQuery(ParticipantCheckers.NOTNULL)
	        		.addParameter("firstName").done()
	        		.addParameter("lastName").done()
	        		.onParticipatesNamed(
	        				(query, name, value, paramInfo) -> {
	        					if (name.equals("firstName")) {
	        						query.setParameter(name, value.getFirstName(), StandardBasicTypes.STRING);
	        					} else if (name.equals("lastName")) {
	        						query.setParameter(name, value.getLastName(), StandardBasicTypes.STRING);
	        					}
	        				}
	        		)
	        		.onParticipatesPositional(
	        				(query, currentPosition, value, parameterInfo) -> {
	        					if (parameterInfo.getName().equals("firstName")) {
	        						query.setParameter(currentPosition, value.getFirstName(), StandardBasicTypes.STRING);
	        					} else if (parameterInfo.getName().equals("lastName")) {
	        						query.setParameter(currentPosition, value.getLastName(), StandardBasicTypes.STRING);
	        					}
	        				}
	        		)
	        	.done()
	        	.addMapper("formalNamesArr", FormalName.class).participatesInQuery(ParticipantCheckers.NOTNULL)
	        		.addParameter("firstNameArr").repeater(true).done()
	        		.addParameter("lastNameArr").repeater(true).done()
	        		.onParticipatesNamed(
	        				(query, name, value, paramInfo) -> {
	        					if (paramInfo.getName().equals("firstNameArr")) {
	        						query.setParameter(name, value.getFirstName(), StandardBasicTypes.STRING);
	        					} else if (paramInfo.getName().equals("lastNameArr")) {
	        						query.setParameter(name, value.getLastName(), StandardBasicTypes.STRING);
	        					}
	        				}
	        		)
	        		.onParticipatesPositional(
	        				(query, currentPosition, value, parameterInfo) -> {
	        					if (parameterInfo.getName().equals("firstNameArr")) {
	        						query.setParameter(currentPosition, value.getFirstName(), StandardBasicTypes.STRING);
	        					} else if (parameterInfo.getName().equals("lastNameArr")) {
	        						query.setParameter(currentPosition, value.getLastName(), StandardBasicTypes.STRING);
	        					}
	        				}
	        		)
        	.done()
    		;
    }
    
    private void configPropertyMappersDifferentParamName(QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig) {
    	this.configPropertyMappers(queryTemplateConfig);
        for (String prpName : queryTemplateConfig.getMappersConfig().keySet()) {
        	String filterPrp = queryTemplateConfig.modifyMapper(prpName, Object.class).getFilterPrp();
        	if (filterPrp.startsWith("filterPrp")) {
        		queryTemplateConfig.modifyMapper(filterPrp, Object.class)
        		.modifyParameter().parameterName(filterPrp.replace("filterPrp", "filterPrm")).done();        		
        	}
        }
    }
    
    private MyFilter createMyFilterFull() {
    	return new MyFilter("foo1", "foo2", "foo3", Arrays.asList("foo4.1", "baa4.2"), "foo5",
    			new MyProperty(), Arrays.asList(7001, 7002, 7003), new Integer[] { 8001, 8002, 8003 },
    			new FormalName("FOO_FIRST_NAME", "FOO_LAST_NAME"),
    			new FormalName[] { 
    					new FormalName("FOO_FIRST_NAME", "FOO_LAST_NAME"),
    					new FormalName("FOO_FIRST_NAME", "FOO_LAST_NAME") });    	
    }
    
    private ProxyFactoryCreator proxyFactoryCreator = (config, clazz) -> {
    	return new ProxyFactory() {
    		javassist.util.proxy.ProxyFactory javassistProxyFactory = new javassist.util.proxy.ProxyFactory();
			@Override
			public void setSuperclass(Class clazz) {
				this.javassistProxyFactory.setSuperclass(clazz);
			}
			@Override
			public void setInterfaces(Class[] ifs) {
				this.javassistProxyFactory.setInterfaces(ifs);
			}
			@Override
			public Object create(Class[] paramTypes,
				Object[] args,
				MethodHandler mh) throws Throwable {
				javassist.util.proxy.MethodHandler javassistMethodHandler = 
						(self, thisMethod, proceed, methodArgs) -> {
							return mh.invoke(
									self, 
									thisMethod, 
									proceed,
									methodArgs);
						};
				return this.javassistProxyFactory.create(paramTypes, args, javassistMethodHandler);
			}
			@Override
			public void setFilter(MethodFilter mf) {
				this.javassistProxyFactory.setFilter((method) -> mf.isHandled(method));
			}
    	};
    };
    
    private static String QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS = 
            "/* This comment shows how to place: a backslash using escape (\\\\); an opening bracket (\\[).*/ \n" +
            "select empl.att1 as {empl.id}, empl.att2 as {empl.name}, empl.att3 as {empl.department}, 'E' as {empl.category} \n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [filterPrp1][ empl.att1 = :filterPrp1]  \n" +
            "     [filterPrp2][ empl.att2 > :filterPrp2]  \n" +
            "     [filterPrp3][ (empl.att3 < :filterPrp3 or empl.att3 > 100)]  \n" +
            "     [extra][empl.att4 = 'foo']  \n" +
            "union  \n" +
            "select outs.att1 as {outs.id}, outs.att2 as {outs.name}, outs.att3 as {outs.department}, 'O' as {outs.category} \n" +
            "   from OUTSOURCEDTB outs \n" +
            "   where \n" +
            "     outs.att1 = 'foo'  \n" +
            "     and outs.att2 = 'baa'  \n" +
            "     and outs.att3 = 'foo'  \n" +
            "     [filters]  \n" +
            "     [filterPrp4][outs.att4 in (:filterPrp4)]  \n" +
            "union  \n" +
            "select free.att1 as {empl.id}, free.att2 as {empl.name}, free.att3 as {empl.department}, 'F' as {empl.category} \n" +
            "   from FREELANCERTB {free}  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [filterPrp5][ free.att1 = :filterPrp5]  \n" +
            "     [filterPrp6][ free.att2 > :filterPrp6]  \n" +
            "     [filterPrp1,filterPrp2,filterPrp3][ /*filterPrp1,filterPrp2,filterPrp3*/  free.att1 = :filterPrp1]  \n" +
            "     [$any$ filterPrp1,filterPrp2,filterPrp3][ /*$any$ filterPrp1,filterPrp2,filterPrp3*/  free.att1 = :filterPrp1]  \n" +
            "     [filterPrp1,!filterPrp2,filterPrp3][ /*filterPrp1,!filterPrp2,filterPrp3*/  free.att1 = :filterPrp1]  \n" +
            "     [!filterPrp5,!filterPrp6][ /*!filterPrp5,!filterPrp6*/  free.att1 = :filterPrp1]  \n" +
            "     [!filterPrp1,!filterPrp3][ /*!filterPrp1,!filterPrp3*/  free.att1 = :filterPrp1]  \n" +
            "     [extra][free.att3 = 'foo'] ";
    
    public void buildQueryWithoutParenthesisBase(
    	Function<String, String> replacer,
    	Function<QueryTemplateConfig<Query<MyEntity>, MyFilter>, QueryTemplateConfig<Query<MyEntity>, MyFilter>> configChanger,
    	Consumer<QueryMock<MyEntity>> assertsQueryMockDefault,
    	Consumer<QueryMock<MyEntity>> assertsQueryMockAfterFullFilter,
    	Consumer<QueryMock<MyEntity>> assertsQueryMockAfterEmptyFilter) {
    	
        String query = QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS;
        
        query = replacer.apply(query);
        
        MyFilter mf = this.createMyFilterFull();
        mf.setFilterPrp2("");
        mf.setFilterPrp5("");
        mf.setFilterPrp6(null);
        mf.setFilterPrp7(null);
        mf.setFilterPrp8(null);
        mf.setFormalName(null);
        mf.setFormalNamesArr(null);

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        
        this.configPropertyMappers(queryTemplateConfig);
        
        queryTemplateConfig = configChanger.apply(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att2 > :filterPrp2.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+\\(empl\\.att3 < :filterPrp3 or empl\\.att3 > 100\\).*")));
        // 'extra' criterion is included because at least one parameter is participating in the query.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att4 = 'foo'.*")));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\).*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att1 = :filterPrp5.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att2 > :filterPrp6.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$any\\$ filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*filterPrp1,!filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp5,!filterPrp6\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp1,!filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att3 = 'foo'.*")));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
		if (assertsQueryMockDefault == null) {
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp1, foo1.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp3, foo3.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp4_0, foo4.1.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp4_1, baa4.2.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp4_1, baa4.2.*"))));
			
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp2, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp5, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp6, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp7, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp8, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[formalName, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[formalNamesArr, .*")))));
		} else {
			assertsQueryMockDefault.accept(queryMock);
		}
		
		mf = this.createMyFilterFull();
		state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att2 > :filterPrp2.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+\\(empl\\.att3 < :filterPrp3 or empl\\.att3 > 100\\).*")));
        // 'extra' criterion is included because at least one parameter is participating in the query.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att4 = 'foo'.*")));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\).*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att1 = :filterPrp5.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att2 > :filterPrp6.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$any\\$ filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*filterPrp1,!filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp5,!filterPrp6\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp1,!filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att3 = 'foo'.*")));

        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
		if (assertsQueryMockAfterFullFilter == null) {
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp1, foo1.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp3, foo3.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp4_0, foo4.1.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp4_1, baa4.2.*"))));
			assertThat(queryMock.getParameterCalls(), hasItem(matchesPattern(replacer.apply("^setParameter\\[filterPrp4_1, baa4.2.*"))));
			
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp2, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp5, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp6, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp7, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[filterPrp8, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[formalName, .*")))));
			assertThat(queryMock.getParameterCalls(), hasItem(not(matchesPattern(replacer.apply("^setParameter\\[formalNamesArr, .*")))));
		} else {
			assertsQueryMockAfterFullFilter.accept(queryMock);
		}
		
		mf = new MyFilter();
		state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att2 > :filterPrp2.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+\\(empl\\.att3 < :filterPrp3 or empl\\.att3 > 100\\).*"))));
        // 'extra' criterion is included because at least one parameter is participating in the query.
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+empl\\.att4 = 'foo'.*"))));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\).*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att1 = :filterPrp5.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att2 > :filterPrp6.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$any\\$ filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*filterPrp1,!filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp5,!filterPrp6\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp1,!filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att3 = 'foo'.*")));

        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
		if (assertsQueryMockAfterEmptyFilter == null) {
			assertThat(queryMock.getParameterCalls(), empty());
		} else {
			assertsQueryMockAfterEmptyFilter.accept(queryMock);
		}
    }
    
    @Test
    public void buildQueryWithoutParenthesisPrp4Unpacked() {
		this.buildQueryWithoutParenthesisBase(
				Function.identity(),
				Function.identity(),
				null,
				null,
				null
		);
    }
    
    @Test
    public void buildQueryWithoutParenthesisPrp4UnpackedLambda() {
		assertThrows(QueryTemplateException.class, () -> {
			this.buildQueryWithoutParenthesisBase(
					Function.identity(),
					config -> {
						return config.removeMapper(f -> f.getFilterPrp4()); // `MyFilter::getFilterPrp1` can be used as well. Is it less clear than `f -> f.getFilterPrp4()`?! 
					},
					null,
					null,
					null
			);
		});
		
		this.buildQueryWithoutParenthesisBase(
				Function.identity(),
				config -> {
					return config
							.proxyFactoryCreator(this.proxyFactoryCreator)
							.removeMapper(f -> f.getFilterPrp1())
							.addMapper(f -> f.getFilterPrp1())
								.participatesInQuery(ParticipantCheckers.STRING_NOTEMPTY)
								.onParticipatesNamed(this.simpleOnParticipatesNamed(StandardBasicTypes.STRING))
								.onParticipatesPositional(this.simpleOnParticipatesPositional(StandardBasicTypes.STRING))
							.done();
				},
				null,
				null,
				null
		);
    }

    @Test
    public void buildQueryWithoutParenthesisPrp4UnpackedDifferentParamName() {
    	this.buildQueryWithoutParenthesisBase(
    			s -> (s
					.replaceAll(":filterPrp", ":filterPrm")
					.replaceAll("(setParameter[^\\[]*\\[)filterPrp", "$1filterPrm")
				),
				config -> {
					this.configPropertyMappersDifferentParamName(config);
					return config;
				},
				null,
				null,
				null
    	);
    }
    
    @Test
    public void buildQueryWithoutParenthesisPrp4Packed() {
    	this.buildQueryWithoutParenthesisBase(
    			s -> (s
    				.replace("outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\)", "outs\\.att4 in \\(:filterPrp4\\)")
    				.replaceAll("^(.*setParameter)(.*filterPrp4)_\\d+.*$", "$1List$2, \\\\[foo4\\\\.1, baa4\\\\.2\\\\].*\\$")
				),
				config -> {
			        return config
			        	.modifyMapper("filterPrp4", new SimpleTypeToken<Collection<String>>(){}.getRawType())
			        		.onParticipatesNamed(this.simpleOnParticipatesListNamed(StandardBasicTypes.STRING))
			        		.onParticipatesPositional(this.simpleOnParticipatesListPositional(StandardBasicTypes.STRING))
			        		.modifyParameter().unpackListItems(false).done()
			    		.done();
				},
				null,
				null,
				null
    	);
    }
    
    @Test
    public void buildQueryWithoutParenthesisPrp4PackedDifferentParamName() {
    	this.buildQueryWithoutParenthesisBase(
    			s -> (s
    				.replace("outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\)", "outs\\.att4 in \\(:filterPrp4\\)")
    				.replaceAll(":filterPrp", ":filterPrm")
    				.replaceAll("^(.*setParameter)(.*filterPrp4)_\\d+.*$", "$1List$2, \\\\[foo4\\\\.1, baa4\\\\.2\\\\].*\\$")
					.replaceAll("(setParameter[^\\[]*\\[)filterPrp", "$1filterPrm")
					//.replaceAll("setParameterList\\[filterPrp", "setParameterList\\[filterPrm")
				),
				config -> {
					this.configPropertyMappersDifferentParamName(config);
			        return config
		            	.modifyMapper("filterPrp4", new SimpleTypeToken<Collection<String>>(){}.getRawType())
			        		.onParticipatesNamed(this.simpleOnParticipatesListNamed(StandardBasicTypes.STRING))
			        		.onParticipatesPositional(this.simpleOnParticipatesListPositional(StandardBasicTypes.STRING))
			        		.modifyParameter().unpackListItems(false).done()
	        			.done();
				},
				null,
				null,
				null
    	);
    }

    @Test
    public void buildQueryWithoutParenthesisPositional() {
    	this.buildQueryWithoutParenthesisBase(
    			s -> {
    				if (s.startsWith("(?s).*")) {
    					s = s.replaceAll(":filterPrp\\w*\\b", "\\\\?");
    				}
    				return s;
    			},
				config -> {
					this.configPropertyMappers(config);
			        return config
		            	.convertNamedToPositionalParameters(true);
				},
				qm -> {
					assertThat(qm.getParameterCalls(), hasSize(7));
					assertThat(
							qm.getParameterCalls(), 
							contains(
									startsWith("setParameter[1, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[2, foo3, org.hibernate.type.StringType"  ),
									startsWith("setParameter[3, foo4.1, org.hibernate.type.StringType"),
									startsWith("setParameter[4, baa4.2, org.hibernate.type.StringType"),
									startsWith("setParameter[5, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[6, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[7, foo1, org.hibernate.type.StringType"  )
							)
					);
				},
				qm -> {
					assertThat(qm.getParameterCalls(), hasSize(9));
					assertThat(
							qm.getParameterCalls(),
							contains(
									startsWith("setParameter[1, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[2, foo2, org.hibernate.type.StringType"  ),
									startsWith("setParameter[3, foo3, org.hibernate.type.StringType"  ),
									startsWith("setParameter[4, foo4.1, org.hibernate.type.StringType"),
									startsWith("setParameter[5, baa4.2, org.hibernate.type.StringType"),
									startsWith("setParameter[6, foo5, org.hibernate.type.StringType"  ),
									startsWith("setParameter[7, io.github.querytemplate.MyProperty"   ),
									startsWith("setParameter[8, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[9, foo1, org.hibernate.type.StringType"  )
							)
					);
				},
				null
    	);
    }
    
    @Test
    public void buildQueryWithoutParenthesisPositionalDifferentParamName() {

    	this.buildQueryWithoutParenthesisBase(
    			s -> {
    				if (s.startsWith("(?s).*")) {
    					s = s.replaceAll(":filterPrp\\w*\\b", "\\\\?");
    				}
    				return s.replaceAll(":filterPrp", ":filterPrm");
    			},
				config -> {
					this.configPropertyMappersDifferentParamName(config);
			        return config
		            	.convertNamedToPositionalParameters(true);
				},
				qm -> {
					assertThat(qm.getParameterCalls(), hasSize(7));
					assertThat(
							qm.getParameterCalls(), 
							contains(
									startsWith("setParameter[1, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[2, foo3, org.hibernate.type.StringType"  ),
									startsWith("setParameter[3, foo4.1, org.hibernate.type.StringType"),
									startsWith("setParameter[4, baa4.2, org.hibernate.type.StringType"),
									startsWith("setParameter[5, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[6, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[7, foo1, org.hibernate.type.StringType"  )
							)
					);
				},
				qm -> {
					assertThat(qm.getParameterCalls(), hasSize(9));
					assertThat(
							qm.getParameterCalls(),
							contains(
									startsWith("setParameter[1, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[2, foo2, org.hibernate.type.StringType"  ),
									startsWith("setParameter[3, foo3, org.hibernate.type.StringType"  ),
									startsWith("setParameter[4, foo4.1, org.hibernate.type.StringType"),
									startsWith("setParameter[5, baa4.2, org.hibernate.type.StringType"),
									startsWith("setParameter[6, foo5, org.hibernate.type.StringType"  ),
									startsWith("setParameter[7, io.github.querytemplate.MyProperty"   ),
									startsWith("setParameter[8, foo1, org.hibernate.type.StringType"  ),
									startsWith("setParameter[9, foo1, org.hibernate.type.StringType"  )
							)
					);
				},
				null
    	);
    }
    
    public static final String BUILD_QUERY_WITH_PARENTHESIS = 
            "select empl.att1 as {empl.id} \n" +
            "   from EMPLOYEESTB empl \n" +
            "   [filters] \n" +
            "   [where] \n" +
            "     [filterPrp1][ empl.att1 = :filterPrp1] \n" +
            "     [filterPrp2][ /*filterPrp2*/ empl.att2 > :filterPrp2] \n" +
            "     [(] \n" +
            "       [filterPrp2][ empl.att2 > :filterPrp2] \n" +
            "       [filterPrp2][and][ empl.att2 > :filterPrp2] \n" +
            "     [)] \n" +
            "     [filterPrp1][or][ empl.att1 = :filterPrp1] \n" +
            "     [(] \n" +
            "       [and][(] \n" +
            "         [filterPrp7,filterPrp8][repeat][or][ (empl.att7 = :filterPrp7 and empl.att8 = :filterPrp8)] \n" +
            "       [)] \n" +
            "     [)] \n" +
            "     [extra][empl.att4 = 'foo'] \n";
    
    public void buildQueryWithParenthesis(
    	Function<String, String> replacer,
    	Function<QueryTemplateConfig<Query<MyEntity>, MyFilter>, QueryTemplateConfig<Query<MyEntity>, MyFilter>> configChanger) {
    	
        String query = BUILD_QUERY_WITH_PARENTHESIS;

        query = replacer.apply(query);
        
        MyFilter mf = this.createMyFilterFull();
        mf.setFilterPrp2("");
        mf.setFilterPrp6(null);
        mf.setFormalName(null);
        mf.setFormalNamesArr(null);

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateCompactConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class)
        			.compactQueryText(true);
        
        this.configPropertyMappers(queryTemplateConfig);
        this.configPropertyMappers(queryTemplateCompactConfig);
        queryTemplateCompactConfig.compactQueryText(true);
        
        queryTemplateConfig = configChanger.apply(queryTemplateConfig);
        queryTemplateCompactConfig = configChanger.apply(queryTemplateCompactConfig);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplate<Query<MyEntity>, MyFilter> qtCompact = QueryTemplate.of(queryTemplateCompactConfig);
        
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        QueryTemplateState<Query<MyEntity>, MyFilter> stateCompact = qtCompact.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());
        LOG.debug("Compact result:\n" + stateCompact.getQueryString());
        
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*where\\s*empl.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*\\(\\s*empl.att2 > :filterPrp2\\s*and\\s*empl.att2 > :filterPrp2\\s*\\).*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*or\\s*empl.att1 = :filterPrp1.*")));
        // The repeat token unfolds filterPrp7/filterPrp8 into indexed parameters.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*\\(\\s*\\(\\s*\\(empl\\.att7 = :filterPrp7_0 and empl\\.att8 = :filterPrp8_0\\) or  \\(empl\\.att7 = :filterPrp7_1 and empl\\.att8 = :filterPrp8_1\\) or  \\(empl\\.att7 = :filterPrp7_2 and empl\\.att8 = :filterPrp8_2\\)\\s*\\)\\s*\\).*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*empl.att4 = 'foo'.*")));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp1, foo1, org.hibernate.type.StringType"))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp2, foo2, org.hibernate.type.StringType")))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp7_0, 7001, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp7_1, 7002, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp7_2, 7003, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp8_0, 8001, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp8_1, 8002, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp8_2, 8003, org.hibernate.type.IntegerType"))));
        
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp2")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp3")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp4")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp5")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp6")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalName")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalNamesArr")))));
        
        mf = this.createMyFilterFull();
        
        state = qt.buildQueryState(mf);
        
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*where\\s*empl.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*or\\s*empl.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*/\\*filterPrp2\\*/ empl\\.att2 > :filterPrp2.*")));
        // The repeat token unfolds filterPrp7/filterPrp8 into indexed parameters.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*\\(\\s*\\(\\s*\\(empl\\.att7 = :filterPrp7_0 and empl\\.att8 = :filterPrp8_0\\) or  \\(empl\\.att7 = :filterPrp7_1 and empl\\.att8 = :filterPrp8_1\\) or  \\(empl\\.att7 = :filterPrp7_2 and empl\\.att8 = :filterPrp8_2\\)\\s*\\)\\s*\\).*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*empl.att4 = 'foo'.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*\\(\\s*empl.att2 > :filterPrp2\\s*and\\s*empl.att2 > :filterPrp2\\s*\\).*")));

        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp1, foo1, org.hibernate.type.StringType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp2, foo2, org.hibernate.type.StringType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp7_0, 7001, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp7_1, 7002, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp7_2, 7003, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp8_0, 8001, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp8_1, 8002, org.hibernate.type.IntegerType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp8_2, 8003, org.hibernate.type.IntegerType"))));
        
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp3")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp4")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp5")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp6")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalName")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalNamesArr")))));
        
        mf = new MyFilter();
        
        state = qt.buildQueryState(mf);
        
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*where\\s*empl.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*or\\s*empl.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*/\\*filterPrp2\\*/ empl\\.att2 > :filterPrp2.*"))));
        // The repeat token unfolds filterPrp7/filterPrp8 into indexed parameters.
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*\\(\\s*\\(\\s*\\(empl\\.att7 = :filterPrp7_0 and empl\\.att8 = :filterPrp8_0\\) or  \\(empl\\.att7 = :filterPrp7_1 and empl\\.att8 = :filterPrp8_1\\) or  \\(empl\\.att7 = :filterPrp7_2 and empl\\.att8 = :filterPrp8_2\\)\\s*\\)\\s*\\).*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*empl.att4 = 'foo'.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*\\(\\s*empl.att2 > :filterPrp2\\s*and\\s*empl.att2 > :filterPrp2\\s*\\).*"))));

        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), empty());
    }
	
    @Test
    public void buildQueryWithParenthesis() {
		this.buildQueryWithParenthesis(
				Function.identity(), 
				Function.identity()
		);
    }

    @Test
    public void buildQueryWithParenthesisDifferentParamName() {
    	this.buildQueryWithParenthesis(
    			s -> (
    					s.replaceAll(":filterPrp", ":filterPrm")
    					.replaceAll("setParameter\\[filterPrp", "setParameter\\[filterPrm")
				),
				config -> {
        						this.configPropertyMappersDifferentParamName(config);
        						return config;
				}
    	);
    }
    
    private static final String BUILD_QUERY_WITH_QUERY_HELPER_HELPER_QUERY =
            "select FUNC.att1, FUNC.att2, FUNC.att3, 'F' as category \n" +
            "   from EMPLOYEESTB FUNC \n" +
            "   [filters] \n" +
            "   [where] \n" +
            "     [filterPrp1][ FUNC.att1 = :filterPrp1] \n" +
            "     [extra][FUNC.att4 = 'foo'] \n";    		
    private static final String BUILD_QUERY_WITH_QUERY_HELPER =
            "select FUNC_H_SQ.att1 as {empl.id} \n" +
            "   from ( \n" +
            "     [Q:EmployeeQH]" +
            "   ) FUNC_H_SQ\n";
    
    public void buildQueryWithQueryHelperBase(
		Function<String, String> replacer,
		Function<QueryTemplateConfig<Query<MyEntity>, MyFilter>, QueryTemplateConfig<Query<MyEntity>, MyFilter>> configChanger) {
        String helperQuery = BUILD_QUERY_WITH_QUERY_HELPER_HELPER_QUERY;

        String query = BUILD_QUERY_WITH_QUERY_HELPER;

        query = replacer.apply(query);
        helperQuery = replacer.apply(helperQuery);
        
        MyFilter mf = this.createMyFilterFull();
        mf.setFilterPrp2("");
        mf.setFilterPrp6(null);
        mf.setFormalName(null);
        mf.setFormalNamesArr(null);
        
        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        this.configPropertyMappers(queryTemplateConfig);

		queryTemplateConfig.addQueryHelper("EmployeeQH", helperQuery);
        
		queryTemplateConfig = configChanger.apply(queryTemplateConfig);
		
        //QueryTemplate qtHelper = new QueryTemplate(helperQuery, mappers);
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        //qt.addQueryHelper("EmployeeQH", qtHelper);

        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // The helper query is inlined and its participating in the query criterion is present.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*where\\s*FUNC.att1 = :filterPrp1\\s*and\\s*FUNC.att4 = 'foo'.*")));
        assertThat(state.getQueryString(), containsString(replacer.apply("FUNC_H_SQ")));
        
        mf = this.createMyFilterFull();
        
        state = qt.buildQueryState(mf);
        
        // The helper query is in-lined and its participating in the query criterion is present.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*where\\s*FUNC.att1 = :filterPrp1\\s*and\\s*FUNC.att4 = 'foo'.*")));
        assertThat(state.getQueryString(), containsString(replacer.apply("FUNC_H_SQ")));
        
        //Empty
        mf = new MyFilter();
        
        state = qt.buildQueryState(mf);
        
        // The helper query is inlined and its participating in the query criterion is present.
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*where\\s*FUNC.att1 = :filterPrp1\\s*and\\s*FUNC.att4 = 'foo'.*"))));
        assertThat(state.getQueryString(), containsString(replacer.apply("FUNC_H_SQ")));
    }
    
    @Test
    public void buildQueryWithQueryHelper() {
		this.buildQueryWithQueryHelperBase(
				Function.identity(),
				Function.identity()
		);
    }

    @Test
    public void buildQueryWithQueryHelperDifferentParamName() {
		this.buildQueryWithQueryHelperBase(
				s -> (
						s.replaceAll(":filterPrp", ":filterPrm")
						.replaceAll("setParameter\\[filterPrp", "setParameter\\[filterPrm")
				),
				config -> {
					this.configPropertyMappersDifferentParamName(config);
					return config;
				}
		);
    }
    
    private static final String CONDITIONED_QUERY_HELPER_QUERY_HELPER =
            "select FUNC.att1 from EMPLOYEESTB FUNC \n" +
            "   [filters] \n" +
            "   [where] \n" +
            "     [filterPrp1][ FUNC.att1 = :filterPrp1] \n";
    private static final String CONDITIONED_QUERY_HELPER_QUERY =
            "select FUNC_H_SQ.att1 as {empl.id} \n" +
            "   from ( \n" +
            "   [filters] \n" +
            "     [filterPrp5] [no_operator][Q:EmployeeQH]" +
            "     [!filterPrp5][no_operator][ SELECT 'NOTHING' FROM DUAL]" +
            "   ) FUNC_H_SQ\n";
  
    public void conditionedQueryHelperBase(
    		Function<String, String> replacer,
    		Function<QueryTemplateConfig<Query<MyEntity>, MyFilter>, QueryTemplateConfig<Query<MyEntity>, MyFilter>> configChanger
    	) {
        String helperQuery = CONDITIONED_QUERY_HELPER_QUERY_HELPER;

        String query = CONDITIONED_QUERY_HELPER_QUERY;
        query = replacer.apply(query);
        helperQuery = replacer.apply(helperQuery);

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);

		queryTemplateConfig.addQueryHelper("EmployeeQH", helperQuery);
		this.configPropertyMappers(queryTemplateConfig);
		queryTemplateConfig = configChanger.apply(queryTemplateConfig);
		
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);

        // filterPrp5 participating in the query -> helper is used.
        MyFilter participatingFilter = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "bla",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });
        QueryTemplateState<Query<MyEntity>, MyFilter> participatingState = qt.buildQueryState(participatingFilter);
        LOG.debug("Participating in the query:\n" + participatingState.getQueryString());
        assertThat(participatingState.getQueryString(), containsString(replacer.apply("FUNC.att1 = :filterPrp1")));
        assertThat(participatingState.getQueryString(), containsString(replacer.apply("EMPLOYEESTB")));
        assertThat(participatingState.getQueryString(), not(containsString(replacer.apply("SELECT 'NOTHING' FROM DUAL"))));

        // filterPrp5 empty -> fallback SELECT is used.
        MyFilter empty = this.createMyFilterFull();
        empty.setFilterPrp2("");
        empty.setFilterPrp5("");
        empty.setFilterPrp6(null);
        empty.setFormalName(null);
        empty.setFormalNamesArr(null);
        
        QueryTemplateState<Query<MyEntity>, MyFilter> stateEmpty = qt.buildQueryState(empty);
        
        LOG.debug("Empty:\n" + stateEmpty.getQueryString());
        assertThat(stateEmpty.getQueryString(), not(containsString(replacer.apply("FUNC.att1 = :filterPrp1"))));
        assertThat(stateEmpty.getQueryString(), containsString("SELECT 'NOTHING' FROM DUAL"));
    }
    
    @Test
    public void conditionedQueryHelper() {
		this.conditionedQueryHelperBase(
				Function.identity(),
				Function.identity()
		);
    }
    
    @Test
    public void conditionedQueryHelperDifferentParamName() {
		this.conditionedQueryHelperBase(
				s -> (
						s.replaceAll(":filterPrp", ":filterPrm")
						.replaceAll("setParameter\\[filterPrp", "setParameter\\[filterPrm")
				),
				config -> {
					this.configPropertyMappersDifferentParamName(config);
					return config;
				}
		);
    }
    
    private static final String USED_PARAMETER_QUERY =
            "select FUNC.att1 from EMPLOYEESTB FUNC \n" +
            "   [filters] \n" +
            "   [where] \n" +
            "     [filterPrp1AndfilterPrp2][ (FUNC.att1 = :filterPrp1 and FUNC.att2 = :filterPrp2) ] \n";
    
    public void usedParameterTestBase(
    		Function<String, String> replacer, 
    		Function<
    			QueryTemplateConfig<Query<MyEntity>, MyFilter>, 
    			QueryTemplateConfig<Query<MyEntity>, MyFilter>
    		> configChanger) {
        String query = USED_PARAMETER_QUERY;
        query = replacer.apply(query);

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        this.configPropertyMappers(queryTemplateConfig);
        
        queryTemplateConfig = configChanger.apply(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        this.configPropertyMappers(queryTemplateConfig);

        // filterPrp5 participating in the query -> helper is used.
        MyFilter mf = this.createMyFilterFull();
        mf.setFilterPrp6(null);
        mf.setFormalName(null);
        mf.setFormalNamesArr(null);
        
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf );
        LOG.debug("participating in the query:\n" + state.getQueryString());
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*where\\s*\\(FUNC\\.att1 = :filterPrp1 and FUNC\\.att2 = :filterPrp2\\).*")));
        
        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp1, foo1, org.hibernate.type.StringType"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp2, foo2, org.hibernate.type.StringType"))));
        
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp3")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp4")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp5")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp6")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp7")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp8")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalName")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalNamesArr")))));        
    }
    
    @Test
    public void usedParameterTest() {
    	this.usedParameterTestBase(
    			Function.identity(),
    			Function.identity()
    	);
    }
    
    @Test
    public void usedParameterTestDifferentParamName() {
    	this.usedParameterTestBase(
    			s -> (
    					s
    					.replaceAll(":filterPrp", ":filterPrm")
    					.replaceAll("setParameter\\[filterPrp", "setParameter\\[filterPrm")
    			), 
    			config -> {
    				this.configPropertyMappersDifferentParamName(config);
    				return config;
    			});
    }
    
    /**
     * Always same instance of ScriptEngineManager, to simulates production environment 
     * where {@link ScriptEngineManager} is reused and {@link ScriptEngine} is created 
     * for each {@link QueryTemplateState} instance.
     */
    ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
	
    /**
     * Singleton instance.
     */
    private JSR233EvalRunnerCreator groovyEvalRunnerCreatorCompiled;
    private JSR233EvalRunnerCreator groovyEvalRunnerCreatorInterpreted;
    private JSR233EvalRunnerCreator graalvmEvalRunnerCreatorCompiled;
    private JSR233EvalRunnerCreator graalvmEvalRunnerCreatorInterpreted;
    private JSR233EvalRunnerCreator nashornEvalRunnerCreatorCompiled;
    private JSR233EvalRunnerCreator nashornEvalRunnerCreatorInterpreted;
    
    
    /**
     * Adapted BeanShell interpreter to support javascript like field access to map values, e.g.:
     * <pre>
     * $eval$ pp.filterPrp1 && pp.filterPrp2 && pp.filterPrp3
     * </pre>
     * References:
     *   <a href="https://beanshell.org/manual/bshmanual.html#set(),%20get(),%20and%20unset():~:text=set()%2C%20get()%2C%20and%20unset()">set(), get(), and unset()</a>
     */
    public static final class AdaptedBeanShellEvalRunner implements EvalRunner {
    	Interpreter	interpreter;
    	public AdaptedBeanShellEvalRunner() {
    		interpreter = new Interpreter();
    	}
		@Override
		public Object binding(QueryTemplateState<?, ?> preliminaryState,
			String name,
			Object value) throws Throwable {
			if (value instanceof Map<?, ?>) {
				Map<String, ?> valueMap = (Map<String, ?>) value;
				interpreter.unset(name);
				interpreter.eval(name + "=object();");
				for (String keyItem : valueMap.keySet()) {
					interpreter.set(name+"." + keyItem, valueMap.get(keyItem));
				}
			}
			return interpreter.get(name);
		}

		@Override
		public void clearBindings(QueryTemplateState<?, ?> preliminaryState) throws Throwable {
			interpreter.getNameSpace().clear();
		}

		@Override
		public Object eval(QueryTemplateState<?, ?> preliminaryState,
			String script) throws Throwable {
			return interpreter.eval(script);
		}
    }
    
	/**
	 * Pure BeanShell interpreter, it does not support javascript like field access to map values, e.g.:
	 * <pre>
	 * $eval$ pp.filterPrp1 && pp.filterPrp2 && pp.filterPrp3
	 * </pre>
	 * need to be modified to:
	 * <pre>
	 * $eval$ pp{"filterPrp1"} && pp{"filterPrp2"} && pp{"filterPrp3"}
	 * </pre>
	 * References:
	 *   <a href="https://beanshell.org/manual/bshmanual.html#set(),%20get(),%20and%20unset():~:text=Equivalent%20to%3A%20h.put(}">Maps, BeanShell</a>
	 */
    public static final class PureBeanShellEvalRunner implements EvalRunner {
    	Interpreter	interpreter;
    	public PureBeanShellEvalRunner() {
    		interpreter = new Interpreter();
    	}
		@Override
		public Object binding(QueryTemplateState<?, ?> preliminaryState,
			String name,
			Object value) throws Throwable {
			interpreter.set(name, value);
			return interpreter.get(name);
		}

		@Override
		public void clearBindings(QueryTemplateState<?, ?> preliminaryState) throws Throwable {
			interpreter.getNameSpace().clear();
		}

		@Override
		public Object eval(QueryTemplateState<?, ?> preliminaryState,
			String script) throws Throwable {
			return interpreter.eval(script);
		}
    }
    
	@BeforeEach
    public void setupEvalRunners() {
        this.groovyEvalRunnerCreatorCompiled =
        		new JSR233EvalRunnerCreator(
        				this.scriptEngineManager.getEngineByName("groovy"),
        				JSR233EvalRunnerCreator.JSR233ScriptMode.COMPILED);
        
        this.groovyEvalRunnerCreatorInterpreted =
        		new JSR233EvalRunnerCreator(
        				this.scriptEngineManager.getEngineByName("groovy"), 
        				JSR233EvalRunnerCreator.JSR233ScriptMode.INTERPRETED);

        this.graalvmEvalRunnerCreatorCompiled =
        		new JSR233EvalRunnerCreator(
        				this.scriptEngineManager.getEngineByName("graal.js"), 
        				JSR233EvalRunnerCreator.JSR233ScriptMode.COMPILED);
        
        this.graalvmEvalRunnerCreatorInterpreted =
        		new JSR233EvalRunnerCreator(
        				this.scriptEngineManager.getEngineByName("graal.js"),
        				JSR233EvalRunnerCreator.JSR233ScriptMode.INTERPRETED);

        this.nashornEvalRunnerCreatorCompiled =
        		new JSR233EvalRunnerCreator(
        				this.scriptEngineManager.getEngineByName("nashorn"), 
        				JSR233EvalRunnerCreator.JSR233ScriptMode.COMPILED);
        
        this.nashornEvalRunnerCreatorInterpreted =
        		new JSR233EvalRunnerCreator(
        				this.scriptEngineManager.getEngineByName("nashorn"),
        				JSR233EvalRunnerCreator.JSR233ScriptMode.INTERPRETED);
    }
    
    private static String BUILD_QUERY_WITH_EVAL_QUERY = 
            "/* This comment shows how to place: a backslash using escape (\\\\); an opening bracket (\\[).*/ \n" +
            "select empl.att1 as {empl.id}, empl.att2 as {empl.name}, empl.att3 as {empl.department}, 'F' as {empl.category} \n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [$eval$ pp.filterPrp1][ /*$eval$ pp.filterPrp1*/ empl.att1 = :filterPrp1]  \n" +
            "     [filterPrp2][ empl.att2 > :filterPrp2]  \n" +
            "     [filterPrp3][ (empl.att3 < :filterPrp3 or empl.att3 > 100)]  \n" +
            "     [extra][ empl.att4 = 'foo']  \n" +
            "union  \n" +
            "select outs.att1 as {outs.id}, outs.att2 as {outs.name}, outs.att3 as {outs.department}, 'O' as {outs.category} \n" +
            "   from OUTSOURCEDTB outs \n" +
            "   where \n" +
            "     outs.att1 = 'foo'  \n" +
            "     and outs.att2 = 'baa'  \n" +
            "     and outs.att3 = 'foo'  \n" +
            "     [filters]  \n" +
            "     [filterPrp4][outs.att4 in (:filterPrp4)]  \n" +
            "union  \n" +
            "select free.att1 as {empl.id}, free.att2 as {empl.name}, free.att3 as {empl.department}, 'O' as {empl.category} \n" +
            "   from FREELANCERTB {free}  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [filterPrp5][ free.att1 = :filterPrp5]  \n" +
            "     [filterPrp6][ free.att2 > :filterPrp6]  \n" +
            "     [$eval$ pp.filterPrp1 && pp.filterPrp2 && pp.filterPrp3][ /*$eval$ pp.filterPrp1 && pp.filterPrp2 && pp.filterPrp3*/  free.att1 = :filterPrp1]  \n" +
            "     [$any$ filterPrp1,filterPrp2,filterPrp3][ /*$any$ filterPrp1,filterPrp2,filterPrp3*/  free.att1 = :filterPrp1]  \n" +
            "     [$eval$ pp.filterPrp1 && (pp.filterPrp2 || pp.filterPrp3)][ /*$eval$ pp.filterPrp1 && (pp.filterPrp2 || pp.filterPrp3)*/  free.att1 = :filterPrp1]  \n" +
            "     [!filterPrp5,!filterPrp6][ /*!filterPrp5,!filterPrp6*/  free.att1 = :filterPrp1]  \n" +
            "     [$eval$ !pp.filterPrp1 && !pp.filterPrp3][ /*$eval$ !pp.filterPrp1 && !pp.filterPrp3*/  free.att1 = :filterPrp1]  \n" +
            "     [extra][free.att3 = 'foo'] ";
    
    public void buildQueryWithEval(
    	Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator,
    	Function<String, String> replacer) {
//    	    	
    	String query = BUILD_QUERY_WITH_EVAL_QUERY;
    	
    	query = replacer.apply(query);
        
        MyFilter mf = this.createMyFilterFull();
        mf.setFilterPrp2("");
        mf.setFilterPrp5("");
        mf.setFilterPrp6(null);
        mf.setFilterPrp7(null);
        mf.setFilterPrp8(null);    
        mf.setFormalName(null);
        mf.setFormalNamesArr(null);    

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        
        this.configPropertyMappers(queryTemplateConfig);
        
        queryTemplateConfig.evalRunnerCreator(evalRunnerCreator);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        
        
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*where\\s*/\\*\\$eval\\$ pp\\.filterPrp1\\*/ empl\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*empl\\.att2 > :filterPrp2.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*\\(empl\\.att3 < :filterPrp3 or empl\\.att3 > 100\\).*")));
        // 'extra' criterion is included because at least one parameter is participating in the query.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*empl\\.att4 = 'foo'.*")));
        // filterPrp4 clause belongs to the second (static where) block.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\).*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att1 = :filterPrp5.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att2 > :filterPrp6.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$eval\\$ pp\\.filterPrp1 && pp\\.filterPrp2 && pp\\.filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$any\\$ filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$eval\\$ pp\\.filterPrp1 && \\(pp\\.filterPrp2 \\|\\| pp\\.filterPrp3\\)\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp5,!filterPrp6\\*/  free.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att3 = 'foo'.*")));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp1, foo1, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp3, foo3, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp4_0, foo4.1, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp4_1, baa4.2, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp2")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp5")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp6")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp8")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalName")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalNamesArr")))));
        
        mf = this.createMyFilterFull();
        state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());
        
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*where\\s*/\\*\\$eval\\$ pp\\.filterPrp1\\*/ empl\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*empl\\.att2 > :filterPrp2.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*\\(empl\\.att3 < :filterPrp3 or empl\\.att3 > 100\\).*")));
        // 'extra' criterion is included because at least one parameter is participating in the query.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*empl\\.att4 = 'foo'.*")));
        // filterPrp4 clause belongs to the second (static where) block.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\).*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att1 = :filterPrp5.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att2 > :filterPrp6.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$eval\\$ pp\\.filterPrp1 && pp\\.filterPrp2 && pp\\.filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$any\\$ filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$eval\\$ pp\\.filterPrp1 && \\(pp\\.filterPrp2 \\|\\| pp\\.filterPrp3\\)\\*/  free\\.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp5,!filterPrp6\\*/  free.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*free\\.att3 = 'foo'.*")));
        
        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp1, foo1, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp3, foo3, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp4_0, foo4.1, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp4_1, baa4.2, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp2"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp5"))));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString(replacer.apply("setParameter[filterPrp6"))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[filterPrp8")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalName")))));
        assertThat(queryMock.getParameterCalls(), not(hasItem(containsString(replacer.apply("setParameter[formalNamesArr")))));
        
        mf = new MyFilter();
        state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());
        
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*where\\s*/\\*\\$eval\\$ pp\\.filterPrp1\\*/ empl\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*empl\\.att2 > :filterPrp2.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*\\(empl\\.att3 < :filterPrp3 or empl\\.att3 > 100\\).*"))));
        // 'extra' criterion is included because at least one parameter is participating in the query.
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*empl\\.att4 = 'foo'.*"))));
        // filterPrp4 clause belongs to the second (static where) block.
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*and\\s*outs\\.att4 in \\(:filterPrp4_0, :filterPrp4_1\\).*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att1 = :filterPrp5.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+free\\.att2 > :filterPrp6.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$eval\\$ pp\\.filterPrp1 && pp\\.filterPrp2 && pp\\.filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$any\\$ filterPrp1,filterPrp2,filterPrp3\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), not(matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*\\$eval\\$ pp\\.filterPrp1 && \\(pp\\.filterPrp2 \\|\\| pp\\.filterPrp3\\)\\*/  free\\.att1 = :filterPrp1.*"))));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+/\\*!filterPrp5,!filterPrp6\\*/  free.att1 = :filterPrp1.*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*and\\s*free\\.att3 = 'foo'.*")));
        
        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), empty());
    }
    
    @Test
    public void buildQueryWithEvalNashornCompiled() {
    	this.buildQueryWithEval(
    			this.nashornEvalRunnerCreatorCompiled,
    			Function.identity()
    			);
    }
    
    @Test
    public void buildQueryWithEvalNashornInterpreted() {
    	this.buildQueryWithEval(
    			this.nashornEvalRunnerCreatorInterpreted,
    			Function.identity()
    			);
    }
    
    public void buildQueryWithEvalGraalVM(JSR233EvalRunnerCreator evalRunnerCreator) {
    	Version javaVersion = Version.parse(System.getProperty("java.version"));
    	assumeTrue(javaVersion.feature() >= 21, () ->
    			"Java version must be 21 or higher for this test to run"
		);
    	assumeTrue(() -> {
	        	try {
	    			Class.forName("org.graalvm.polyglot.Context");
	    		} catch (ClassNotFoundException e) {
	    			return false;
	    		}
	    		return true;
	    	},
    		() -> "GraalVM polyglot library must be present for this test to run"
    	);
    }
    
    @Test
    public void buildQueryWithEvalGraalVMCompiled() {
    	this.buildQueryWithEvalGraalVM(
				this.graalvmEvalRunnerCreatorCompiled);
    }
    
    @Test
    public void buildQueryWithEvalGraalVMInterpreted() {
		this.buildQueryWithEvalGraalVM(
				this.graalvmEvalRunnerCreatorInterpreted);
    }
        
    @Test
    public void buildQueryWithEvalBeanShellPure() {
    	this.buildQueryWithEval(
    			state -> new PureBeanShellEvalRunner(),
				(Function<String, String>) (s) -> {
					String result = s;
					// replace pp.filterPrpX with pp{"filterPrpX"} to avoid issues with BeanShell parsing.
					result = result
							.replaceAll("pp\\.(\\b\\w*\\b)", "pp{\"$1\"}")
							.replaceAll("pp\\\\\\.(\\b\\w*\\b)", "pp\\\\{\"$1\"\\\\}");
					return result;
				});
    }

    @Test
    public void buildQueryWithEvalBeanShellAdapted() {
    	this.buildQueryWithEval(
    			state -> new AdaptedBeanShellEvalRunner(),
				(s) -> s);
	}

    public void buildQueryWithEvalGroovy(Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator) {
    	Version javaVersion = Version.parse(System.getProperty("java.version"));
    	assumeTrue(javaVersion.feature() >= 21, 
    			() -> "Java version must be 21 or higher for this test to run");
		assumeTrue(() -> {
			try {
				Class.forName("org.codehaus.groovy.jsr223.GroovyScriptEngineImpl");
			} catch (ClassNotFoundException e) {
				return false;
			}
			return true;
		}, () -> "Groovy JSR223 library must be present for this test to run");
    	
		this.buildQueryWithEval(
				evalRunnerCreator,
				Function.identity());
    }
    
    @Test
    public void buildQueryWithEvalGroovyCompiled() {
    	this.buildQueryWithEvalGroovy(this.groovyEvalRunnerCreatorCompiled);
    }
    
    @Test
    public void buildQueryWithEvalGroovyInterpreted() {
    	this.buildQueryWithEvalGroovy(this.groovyEvalRunnerCreatorInterpreted);
    }

    private static String BUILD_QUERY_WITH_EVAL_VAR_FUNCTION_QUERY = 
            "/* This comment shows how to place: a backslash using escape (\\\\); an opening bracket (\\[).*/ \n" +
            "select empl.att1 as {empl.id}, empl.att2 as {empl.name}, empl.att3 as {empl.department}, 'F' as {empl.category} \n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [$eval$ var prp1Prp2Prp3Var = pp.filterPrp1 && (pp.filterPrp2 || pp.filterPrp3);][no_operator][/*$eval$ var prp1Prp2Prp3Var = pp.filterPrp1 && (pp.filterPrp2 || pp.filterPrp3);*/]  \n" +
            "     [$eval$ function prp1Prp2Prp3Func() { return !prp1Prp2Prp3Var; }                ][no_operator][/*$eval$ function prp1Prp2Prp3Func() { return !prp1Prp2Prp3Var; }*/]  \n" +
            "     [$eval$ prp1Prp2Prp3Var   ][ ( empl.att1 = :filterPrp1 and ( empl.att2 > empl.att3 ) ) ]  \n" +
            "     [$eval$ prp1Prp2Prp3Func()][ ( empl.att1 is null and ( empl.att2 <= empl.att3 ) ) ]  \n";
    
    public void buildQueryWithEvalVarFunction(
    	Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator,
    	Function<String, String> replacer) {
//    	    	
    	String query = BUILD_QUERY_WITH_EVAL_VAR_FUNCTION_QUERY;
    	
    	query = replacer.apply(query);
        
        MyFilter mf = this.createMyFilterFull();
        mf.setFilterPrp2("");
        mf.setFilterPrp3("");

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        
        this.configPropertyMappers(queryTemplateConfig);
        
        queryTemplateConfig.evalRunnerCreator(evalRunnerCreator);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        
        assertThat(state.getQueryString(), not(containsString("/*$eval$ var prp1Prp2Prp3Var = pp.filterPrp1 && (pp.filterPrp2 || pp.filterPrp3);*/")));
        assertThat(state.getQueryString(), not(containsString("/*$eval$ function prp1Prp2Prp3Func() { return !prp1Prp2Prp3Var; }*/")));
        assertThat(state.getQueryString(), not(containsString("( empl.att1 = :filterPrp1 and ( empl.att2 > empl.att3 ) )")));
        assertThat(state.getQueryString(), containsString("( empl.att1 is null and ( empl.att2 <= empl.att3 ) )"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), empty());
        
        mf = this.createMyFilterFull();
        state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());
        
        assertThat(state.getQueryString(), not(containsString("/*$eval$ var prp1Prp2Prp3Var = pp.filterPrp1 && (pp.filterPrp2 || pp.filterPrp3);*/")));
        assertThat(state.getQueryString(), not(containsString("/*$eval$ function prp1Prp2Prp3Func() { return !prp1Prp2Prp3Var; }*/")));
        assertThat(state.getQueryString(), containsString("( empl.att1 = :filterPrp1 and ( empl.att2 > empl.att3 ) )"));
        assertThat(state.getQueryString(), not(containsString("( empl.att1 is null and ( empl.att2 <= empl.att3 ) )")));
        
        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasSize(1));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp1, foo1, org.hibernate.type.StringType")));
        
        mf = new MyFilter();
        state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());
        
        assertThat(state.getQueryString(), not(containsString("/*$eval$ var prp1Prp2Prp3Var = pp.filterPrp1 && (pp.filterPrp2 || pp.filterPrp3);*/")));
        assertThat(state.getQueryString(), not(containsString("/*$eval$ function prp1Prp2Prp3Func() { return !prp1Prp2Prp3Var; }*/")));
        assertThat(state.getQueryString(), not(containsString("( empl.att1 = :filterPrp1 and ( empl.att2 > empl.att3 ) )")));
        assertThat(state.getQueryString(), containsString("( empl.att1 is null and ( empl.att2 <= empl.att3 ) )"));
        
        queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), empty());
    }
    
    @Test
    public void buildQueryWithEvalVarFunctionNashornCompiled() {
    	this.buildQueryWithEvalVarFunction(
    			this.nashornEvalRunnerCreatorCompiled,
    			Function.identity()
    	);
    }
    
    @Test
    public void buildQueryWithEvalVarFunctionNashornInterpreted() {
		this.buildQueryWithEvalVarFunction(
				this.nashornEvalRunnerCreatorInterpreted, 
				Function.identity()
		);
    }
    
	public void buildQueryWithEvalVarFunctionGraalVM(Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator) {
    	Version javaVersion = Version.parse(System.getProperty("java.version"));
		assumeTrue(
				javaVersion.feature() >= 21,
				() -> "Java version must be 21 or higher for this test to run");
		assumeTrue(() -> {
			try {
				Class.forName("org.graalvm.polyglot.Context");
			} catch (ClassNotFoundException e) {
				return false;
				//throw new RuntimeException("GraalVM polyglot library must be present for this test to run", e);
			}
			return true;
		}, () -> "GraalVM polyglot library must be present for this test to run");
    	
    	this.buildQueryWithEvalVarFunction(
    			evalRunnerCreator,
    			Function.identity()
    	);
	}
    
    @Test
    public void buildQueryWithEvalVarFunctionGraalVMCompiled() {
    	this.buildQueryWithEvalVarFunctionGraalVM(
    			this.graalvmEvalRunnerCreatorCompiled
    	);
    }
    
    @Test
    public void buildQueryWithEvalVarFunctionGraalVMInterpreted() {
        this.buildQueryWithEvalVarFunctionGraalVM(
    	        this.graalvmEvalRunnerCreatorInterpreted
        );
    }
    
	public void buildQueryWithEvalVarFunctionGroovy(Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator) {
    	Version javaVersion = Version.parse(System.getProperty("java.version"));
    	assumeTrue(
    			javaVersion.feature() >= 21,
    			() -> "Java version must be 21 or higher for this test to run");
		assumeTrue(() -> {
				try {
					Class.forName("org.codehaus.groovy.jsr223.GroovyScriptEngineImpl");
				} catch (ClassNotFoundException e) {
					return false;
				}
				return true;
			}, 
			() -> "Groovy JSR223 library must be present for this test to run"
		);
	}
	
	@Test
	public void buildQueryWithEvalVarFunctionGroovyCompiled() {
		this.buildQueryWithEvalVarFunctionGroovy(this.groovyEvalRunnerCreatorCompiled);
	}
	
	@Test
	public void buildQueryWithEvalVarFunctionGroovyInterpreted() {
		this.buildQueryWithEvalVarFunctionGroovy(this.groovyEvalRunnerCreatorInterpreted);
	}
	
	public void buildQueryWithEvalVarFunctionBeanShell(
			Function<QueryTemplateState<?, ?>, EvalRunner> evalRunnerCreator,
			Function<String, String> replacer) {
		this.buildQueryWithEvalVarFunction(
				evalRunnerCreator, 
    			s -> (
    					replacer.apply(
    							s.replace("$eval$ function prp1Prp2Prp3Func()", "$eval$ boolean prp1Prp2Prp3Func()")
						)    								
				)
		);
	}
	
	@Test
	public void buildQueryWithEvalVarFunctionBeanShellPure() {
		this.buildQueryWithEvalVarFunctionBeanShell(
				state -> new PureBeanShellEvalRunner(),
				(Function<String, String>) (s) -> {
					String result = s;
					// replace pp.filterPrpX with pp{"filterPrpX"} to avoid issues with BeanShell parsing.
					result = result
							.replaceAll("pp\\.(\\b\\w*\\b)", "pp{\"$1\"}")
							.replaceAll("pp\\\\\\.(\\b\\w*\\b)", "pp\\\\{\"$1\"\\\\}")
							;
					return result;
				}
		);
	}
	
	@Test
	public void buildQueryWithEvalVarFunctionBeanShellAdapted() {
		this.buildQueryWithEvalVarFunctionBeanShell(
				state -> new AdaptedBeanShellEvalRunner(),
				Function.identity()
		);
	}
    
    private static String COMPLEX_PROPERTY_QUERY = 
            "select empl.att1 as {empl.id}, empl.att2 as {empl.name}, empl.att3 as {empl.department}, 'E' as {empl.category} \n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [formalName][ (empl.first_name like :firstName or empl.last_name like :lastName)]";
    @Test
    public void complexProperty() { 	
    	String query = COMPLEX_PROPERTY_QUERY;
        
        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null, new FormalName("FOO_FIRST_NAME", "FOO_LAST_NAME"), null);

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        
        this.configPropertyMappers(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("(empl.first_name like :firstName or empl.last_name like :lastName)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[firstName, FOO_FIRST_NAME, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[lastName, FOO_LAST_NAME, org.hibernate.type.StringType")));
    }
    
    private static String COMPLEX_PROPERTY_REPEAT_QUERY = 
            "select empl.id, empl.first_name, empl.department_id \n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [filterPrp1][ free.att1 = :filterPrp1]  \n" +
            "     [and] [(]  \n" +
            "       [ formalNamesArr][repeat][or][(empl.first_name like :firstNameArr and empl.last_name like :lastNameArr) ]" +
            "     [)]  \n";
    public void complexPropertyRepeatBase(
    	Function<String, String> replacer, 
		Function<
			QueryTemplateConfig<Query<MyEntity>, MyFilter>,
			QueryTemplateConfig<Query<MyEntity>, MyFilter>
    	> configChanger) {
    	String query = COMPLEX_PROPERTY_REPEAT_QUERY;
        query = replacer.apply(query);
    	
    	
        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null, 
                new FormalName("FOO_FIRST_NAME", "FOO_LAST_NAME"), 
                new FormalName[] {
                		new FormalName("FOO_FIRST_NAME_1", "FOO_LAST_NAME_1"),
                		new FormalName("FOO_FIRST_NAME_2", "FOO_LAST_NAME_2")
                });

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        
        this.configPropertyMappers(queryTemplateConfig);
        
        queryTemplateConfig = configChanger.apply(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("(empl.first_name like :firstNameArr_0 and empl.last_name like :lastNameArr_0)  or (empl.first_name like :firstNameArr_1 and empl.last_name like :lastNameArr_1)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[firstNameArr_0, FOO_FIRST_NAME_1, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[firstNameArr_1, FOO_FIRST_NAME_2, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[lastNameArr_0, FOO_LAST_NAME_1, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[lastNameArr_1, FOO_LAST_NAME_2, org.hibernate.type.StringType"))); 
    }
    
    
    @Test
    public void complexPropertyRepeat() {
		this.complexPropertyRepeatBase(
				Function.identity(),
				Function.identity()
		);
    }
    
    @Test
    public void complexPropertyRepeatLambda() {
		this.complexPropertyRepeatBase(
				Function.identity(),
				(config) -> config
					.proxyFactoryCreator(this.proxyFactoryCreator)
					.removeMapper(f -> f.getFormalName())
					.removeMapper(f -> f.getFormalNamesArr())
		        	.addMapper(f -> f.getFormalName())
		        		.participatesInQuery(ParticipantCheckers.NOTNULL)
	        			.addParameter("firstName").done()
	        			.addParameter("lastName").done()
	        			.onParticipatesNamed(
	        				(query, name, value, paramInfo) -> {
	        					if (name.equals("firstName")) {
	        						query.setParameter(name, value.getFirstName(), StandardBasicTypes.STRING);
	        					} else if (name.equals("lastName")) {
	        						query.setParameter(name, value.getLastName(), StandardBasicTypes.STRING);
	        					}
	        				}
						)
	        		.done()
		        	.addMapperA(f -> f.getFormalNamesArr())
		        		.participatesInQuery(ParticipantCheckers.NOTNULL)
		        		.addParameter("firstNameArr").repeater(true).done()
		        		.addParameter("lastNameArr").repeater(true).done()
		        		.switchType()
		        		.onParticipatesNamed(
		        				(query, name, value, paramInfo) -> {
		        					if (paramInfo.getName().equals("firstNameArr")) {
		        						query.setParameter(name, value.getFirstName(), StandardBasicTypes.STRING);
		        					} else if (paramInfo.getName().equals("lastNameArr")) {
		        						query.setParameter(name, value.getLastName(), StandardBasicTypes.STRING);
		        					}
		        				}
		        		)
		        		.onParticipatesPositional(
		        				(query, currentPosition, value, parameterInfo) -> {
		        					if (parameterInfo.getName().equals("firstNameArr")) {
		        						query.setParameter(currentPosition, value.getFirstName(), StandardBasicTypes.STRING);
		        					} else if (parameterInfo.getName().equals("lastNameArr")) {
		        						query.setParameter(currentPosition, value.getLastName(), StandardBasicTypes.STRING);
		        					}
		        				}
		        		)
	        		.done()
		);
    }
    
    private static String ON_PARTICIPATE_PER_PARAMETER_QUERY = 
            "select outs.att1 as {outs.id}, outs.att2 as {outs.name}, outs.att3 as {outs.department}, 'O' as {outs.category} \n" +
            "   from OUTSOURCEDTB outs \n" +
            "   [filters]  \n" +
            "   [where] \n" +
            "     [filterPrp4][outs.att4 in (:filterPrp4_a)]  \n" +
            "union  \n" +
            "select free.att1 as {empl.id}, free.att2 as {empl.name}, free.att3 as {empl.department}, 'F' as {empl.category} \n" +
            "   from FREELANCERTB {free}  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [(] \n" +
            "       [filterPrp4][repeat][or][ outs.att4 like :filterPrp4_b]  \n" +
            "     [)]";
    public void onParticipatePerParameterBase(
    	Function<String, String> replacer, 
		Function<
			QueryTemplateConfig<Query<MyEntity>, MyFilter>,
			QueryTemplateConfig<Query<MyEntity>, MyFilter>
    	> configChanger) {
    	String query = ON_PARTICIPATE_PER_PARAMETER_QUERY;
        query = replacer.apply(query);
    	
        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null, 
                new FormalName("FOO_FIRST_NAME", "FOO_LAST_NAME"), 
                new FormalName[] {
                		new FormalName("FOO_FIRST_NAME_1", "FOO_LAST_NAME_1"),
                		new FormalName("FOO_FIRST_NAME_2", "FOO_LAST_NAME_2")
                });

        QueryTemplateConfig<Query<MyEntity>, MyFilter> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType(), MyFilter.class);
        
        this.configPropertyMappers(queryTemplateConfig);
        
        queryTemplateConfig
        	.proxyFactoryCreator(this.proxyFactoryCreator)
        	.removeMapper(f -> f.getFilterPrp4())
        	.addMapperC(f -> f.getFilterPrp4())
        		.participatesInQuery(ParticipantCheckers.COLLECTION_NOTEMPTY)
	    		.switchType() 
	    		.addParameter("filterPrp4_b")
	    			.repeater(true)
	        		.onParticipatesNamed((q, name, value, paramInfo) -> {
	    				q.setParameter(name, value, StandardBasicTypes.STRING);
	        		})
	        	.done()
	        	.switchType() //this is necessary because we are changing the 
	            			  //  type of the mapper from `String` to `Collection<String>`. 
	            			  //  Without this, the next `addParameter().onParticipatesNamed()` 
	        	              //  raise compilation error: 
	        	              //  `The method setParameterList(String, Collection) in the type Query<MyEntity> is not applicable for the arguments (String, String)`
	    		.addParameter("filterPrp4_a")
	        		.unpackListItems(false)
	        		.onParticipatesNamed((q, name, value, paramInfo) -> {
	    				q.setParameterList(name, value);
	        		})
				.done()
		.done();
        	
        queryTemplateConfig = configChanger.apply(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>, MyFilter> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>, MyFilter> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are participating in the query, filterPrp2 is not.
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*(where|and)\\s+outs\\.att4 in \\(:filterPrp4_a\\).*")));
        assertThat(state.getQueryString(), matchesPattern(replacer.apply("(?s).*\\(\\s*outs.att4 like :filterPrp4_b_0\\s+or\\s+outs.att4 like :filterPrp4_b_1\\s*\\).*")));
        
        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls().size(), equalTo(3));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp4_b_0, foo4.1, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[filterPrp4_b_1, baa4.2, org.hibernate.type.StringType")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameterList[filterPrp4_a, [foo4.1, baa4.2]]"))); 
    }
    
    @Test
    public void onParticipatePerParameter() {
    	this.onParticipatePerParameterBase(
    			Function.identity(),
    			Function.identity()
		);
    }
    
    @Test
    public void scriptEngineManagerTest() throws ScriptException {
    	//Ref: [Is there an eval() function in Java? - Stack Overflow](https://stackoverflow.com/a/2605051/1350308)
    	ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    	ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("js");
    	Bindings bindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
    	if (bindings==null) {
    	    bindings = scriptEngine.createBindings();
    	    scriptEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
    	}
    	bindings.put("filterPrm1", true);
    	bindings.put("filterPrm2", false);
    	Object evalResult = scriptEngine.eval("filterPrm1 || filterPrm2", bindings);
    }
}
