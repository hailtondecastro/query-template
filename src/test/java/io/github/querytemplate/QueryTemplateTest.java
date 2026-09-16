package io.github.querytemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests demonstrating how to use {@link QueryTemplateDefault}. Java port of the
 * C# {@code Demonstracao} class.
 */
public class QueryTemplateTest {

    private static final Logger LOG = LoggerFactory.getLogger(QueryTemplateTest.class);

	private <P> AssignNamedParameter<Query<MyEntity>, P> simpleOnFilledNamed() {
		return (query,
			name,
			value) -> query.setParameter(name, value);
	}
	
	private <P> AssignPositionalParameter<Query<MyEntity>, P> simpleOnFilledPositional() {
		return (query,
			position,
			value) -> query.setParameter(position, value);
	}
	
	private <P> AssignNamedParameter<Query<MyEntity>, P> simpleOnFilledNamed(Type type) {
		return (query,
			name,
			value) -> query.setParameter(name, value, type);
	}
	
	private <P> AssignPositionalParameter<Query<MyEntity>, P> simpleOnFilledPositional(Type type) {
		return (query,
			position,
			value) -> query.setParameter(position, value, type);
	}
	
//	private <PI> AssignNamedParameterDelegate<Query<MyEntity>, Collection<PI>> simpleNamedParameterListSetter() {
//		return (query,
//            name,
//            value) -> query.setParameterList(name, (Collection) value);
//	}
	
	private <PI> AssignNamedParameter<Query<MyEntity>, Collection<PI>> simpleOnFilledListNamed(Type itemType) {
		return (query,
            name,
            value) -> query.setParameterList(name, (Collection) value, itemType);
	}
	
	private <PI> AssignPositionalParameter<Query<MyEntity>, Collection<PI>> simpleOnFilledListPositional(Type itemType) {
		return (query,
			position,
			value) -> query.setParameterList(position, (Collection) value, itemType);
	}
	
    private void configPropertyMappers(QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig) {
    	queryTemplateConfig
    		.clearMappers()
    			.addMapper("filterPrp1", String.class).fillVerifier(FillVerifiers.STRING_NOTEMPTY).onFilled(this.simpleOnFilledNamed(StandardBasicTypes.STRING)).onFilled(this.simpleOnFilledPositional(StandardBasicTypes.STRING)).done()                                                                             
    			.addMapper("filterPrp2", String.class).fillVerifier(FillVerifiers.STRING_NOTEMPTY).onFilled(this.simpleOnFilledNamed(StandardBasicTypes.STRING)).onFilled(this.simpleOnFilledPositional(StandardBasicTypes.STRING)).done()                                                                             
    			.addMapper("filterPrp3", String.class).fillVerifier(FillVerifiers.STRING_NOTEMPTY).onFilled(this.simpleOnFilledNamed(StandardBasicTypes.STRING)).onFilled(this.simpleOnFilledPositional(StandardBasicTypes.STRING)).done()                                                                        
    			.addMapper("filterPrp4", new SimpleTypeToken<Collection<String>>(){}.getRawType()).fillVerifier(FillVerifiers.COLLECTION_NOTEMPTY).onFilled(this.simpleOnFilledNamed(StandardBasicTypes.STRING)).onFilled(this.simpleOnFilledPositional(StandardBasicTypes.STRING)).unpackListItems(true).done()   
    			.addMapper("filterPrp5", String.class).fillVerifier(FillVerifiers.STRING_NOTEMPTY).onFilled(this.simpleOnFilledNamed(StandardBasicTypes.STRING)).onFilled(this.simpleOnFilledPositional(StandardBasicTypes.STRING)).done()
    			.addMapper("filterPrp6", MyProperty.class).fillVerifier(FillVerifiers.NOTNULL).onFilled(this.simpleOnFilledNamed()).onFilled(this.simpleOnFilledPositional()).done()
    			.addMapper("filterPrp7", new SimpleTypeToken<Collection<String>>(){}.getRawType())
	        		.fillVerifier(FillVerifiers.COLLECTION_NOTEMPTY)
	        		.repeater(true)
	        		.onFilled(this.simpleOnFilledNamed(StandardBasicTypes.INTEGER))
	        		.onFilled(this.simpleOnFilledPositional(StandardBasicTypes.INTEGER))
	        		.done()
	        	.addMapper("filterPrp8", Integer[].class).fillVerifier(FillVerifiers.ARRAY_NOTEMPTY).onFilled(this.simpleOnFilledNamed(StandardBasicTypes.INTEGER)).onFilled(this.simpleOnFilledPositional(StandardBasicTypes.INTEGER)).repeater(true).done()
    		;
    }
    
    private void configPropertyMappersDifferentParamName(QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig) {
    	this.configPropertyMappers(queryTemplateConfig);
        for (String prpName : queryTemplateConfig.getMappersConfigMap().keySet()) {
        	String filterPrp = queryTemplateConfig.modifyMapper(prpName, Object.class).getFilterPrp();
        	queryTemplateConfig.modifyMapper(filterPrp, Object.class).parameterName(filterPrp.replace("filterPrp", "filterPrm")).done();
        }
    }
    
    private static String QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS = 
            "/* This comment shows how to place: a backslash using escape (\\\\); an opening bracket (\\[).*/ \n" +
            "select empl.att1 as {empl.id}, empl.att2 as {empl.name}, empl.att3 as {empl.department}, 'F' as {empl.category} \n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [filterPrp1][ empl.att1 = :filterPrp1]  \n" +
            "     [filterPrp2][ empl.att2 > :filterPrp2]  \n" +
            "     [filterPrp3][ (empl.att3 < :filterPrp3 \n or empl.att3 > 100)]  \n" +
            "     [extra][empl.att4 = 'foo']  \n" +
            "union  \n" +
            "select outs.att1 as {outs.id}, outs.att2 as {outs.name}, outs.att3 as {outs.department}, 'T' as {outs.category} \n" +
            "   from OUTSOURCEDTB outs \n" +
            "   where \n" +
            "     outs.att1 = 'foo'  \n" +
            "     and outs.att2 = 'baa'  \n" +
            "     and outs.att3 = 'foo'  \n" +
            "     [filters]  \n" +
            "     [filterPrp4][outs.att4 in (:filterPrp4)]  \n" +
            "union  \n" +
            "select free.att1 as {empl.id}, free.att2 as {empl.name}, free.att3 as {empl.department}, 'I' as {empl.category} \n" +
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
    
    @Test
    public void buildQueryWithoutParenthesisPrp4Unpacked() {
        String query = QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS;
        
        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null);

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        
        this.configPropertyMappers(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are filled, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("empl.att1 = :filterPrp1"));
        assertThat(state.getQueryString(), containsString("empl.att3 < :filterPrp3"));
        assertThat(state.getQueryString(), not(containsString(":filterPrp2")));
        // 'extra' criterion is included because at least one parameter is filled.
        assertThat(state.getQueryString(), containsString("empl.att4 = 'foo'"));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), containsString("outs.att4 in (:filterPrp4_0, :filterPrp4_1)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrp1")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrp3")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrp4")));
    }

    @Test
    public void buildQueryWithoutParenthesisPrp4UnpackedDifferentParamName() {
        String query = QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS;
        query = query.replaceAll(":filterPrp", ":filterPrm");
        
        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null);

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        
        this.configPropertyMappersDifferentParamName(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are filled, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("empl.att1 = :filterPrm1"));
        assertThat(state.getQueryString(), containsString("empl.att3 < :filterPrm3"));
        assertThat(state.getQueryString(), not(containsString(":filterPrm2")));
        // 'extra' criterion is included because at least one parameter is filled.
        assertThat(state.getQueryString(), containsString("empl.att4 = 'foo'"));
        // filterPrm4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), containsString("outs.att4 in (:filterPrm4_0, :filterPrm4_1)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrm1")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrm3")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrm4")));
    }
    
    @Test
    public void buildQueryWithoutParenthesisPrp4Packed() {
        String query = QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS;
        
        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null);

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        
        this.configPropertyMappers(queryTemplateConfig);
        
        queryTemplateConfig
        	.modifyMapper("filterPrp4", new SimpleTypeToken<Collection<String>>(){}.getRawType())
        		.onFilled(this.simpleOnFilledListNamed(StandardBasicTypes.STRING))
        		.onFilled(this.simpleOnFilledListPositional(StandardBasicTypes.STRING))
        		.unpackListItems(false)
    		.done();
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1, filterPrp3 are filterPrp4 filled, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("empl.att1 = :filterPrp1"));
        assertThat(state.getQueryString(), containsString("empl.att3 < :filterPrp3"));
        assertThat(state.getQueryString(), not(containsString(":filterPrp2")));
        // 'extra' criterion is included because at least one parameter is filled.
        assertThat(state.getQueryString(), containsString("empl.att4 = 'foo'"));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), containsString("outs.att4 in (:filterPrp4)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrp1")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrp3")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrp4")));
    }
    
    @Test
    public void buildQueryWithoutParenthesisPrp4PackedDifferentParamName() {
        String query = QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS;
        query = query.replaceAll(":filterPrp", ":filterPrm");
        
        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null);

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        
        this.configPropertyMappersDifferentParamName(queryTemplateConfig);
        
        queryTemplateConfig
        	.modifyMapper("filterPrp4", new SimpleTypeToken<Collection<String>>(){}.getRawType())
        		.onFilled(this.simpleOnFilledListNamed(StandardBasicTypes.STRING))
        		.onFilled(this.simpleOnFilledListPositional(StandardBasicTypes.STRING))
        		.unpackListItems(false)
    		.done();
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1, filterPrp3 are filterPrp4 filled, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("empl.att1 = :filterPrm1"));
        assertThat(state.getQueryString(), containsString("empl.att3 < :filterPrm3"));
        assertThat(state.getQueryString(), not(containsString(":filterPrm2")));
        // 'extra' criterion is included because at least one parameter is filled.
        assertThat(state.getQueryString(), containsString("empl.att4 = 'foo'"));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), containsString("outs.att4 in (:filterPrm4)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrm1")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrm3")));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrm4")));
    }

    @Test
    public void buildQueryWithoutParenthesisPositional() {
        String query = QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS;

        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null);

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType())
        			.convertNamedToPositionalParameters(true);
        
        this.configPropertyMappers(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are filled, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("empl.att1 = ?"));
        assertThat(state.getQueryString(), containsString("empl.att3 < ?"));
        assertThat(state.getQueryString(), not(containsString(":filterPrp2")));
        // 'extra' criterion is included because at least one parameter is filled.
        assertThat(state.getQueryString(), containsString("empl.att4 = 'foo'"));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), containsString("outs.att4 in (?, ?)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[1, foo1, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[2, foo3, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[3, foo4.1, org.hibernate.type.StringType" )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[4, baa4.2, org.hibernate.type.StringType" )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[5, foo1, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[6, foo1, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[7, foo1, org.hibernate.type.StringType"   )));
    }
    
    @Test
    public void buildQueryWithoutParenthesisPositionalDifferentParamName() {
        String query = QUERY_BUILD_QUERY_WITHOUT_PARENTHESIS;
        query = query.replaceAll(":filterPrp", ":filterPrm");

        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo4.1", "baa4.2"), "",
                null, null, null);

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType())
        			.convertNamedToPositionalParameters(true);
        
        this.configPropertyMappersDifferentParamName(queryTemplateConfig);
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // filterPrp1 and filterPrp3 are filled, filterPrp2 is not.
        assertThat(state.getQueryString(), containsString("empl.att1 = ?"));
        assertThat(state.getQueryString(), containsString("empl.att3 < ?"));
        assertThat(state.getQueryString(), not(containsString(":filterPrm2")));
        // 'extra' criterion is included because at least one parameter is filled.
        assertThat(state.getQueryString(), containsString("empl.att4 = 'foo'"));
        // filterPrp4 clause belongs to the second (fixed where) block.
        assertThat(state.getQueryString(), containsString("outs.att4 in (?, ?)"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[1, foo1, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[2, foo3, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[3, foo4.1, org.hibernate.type.StringType" )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[4, baa4.2, org.hibernate.type.StringType" )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[5, foo1, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[6, foo1, org.hibernate.type.StringType"   )));
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("setParameter[7, foo1, org.hibernate.type.StringType"   )));
    }
    
    public static final String BUILD_QUERY_WITH_PARENTHESIS = 
            "select empl.att1 as {empl.id} \n" +
            "   from EMPLOYEESTB empl \n" +
            "   [filters] \n" +
            "   [where] \n" +
            "     [filterPrp1][ empl.att1 = :filterPrp1] \n" +
            "     [filterPrp2][ empl.att2 > :filterPrp2] \n" +
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
    
    @Test
    public void buildQueryWithParenthesis() {
        String query = BUILD_QUERY_WITH_PARENTHESIS;

        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "bla",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        QueryTemplateConfig<Query<MyEntity>> queryTemplateCompactConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType())
        			.compactQueryText(true);
        
        this.configPropertyMappers(queryTemplateConfig);
        this.configPropertyMappers(queryTemplateCompactConfig);
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplate<Query<MyEntity>> qtCompact = QueryTemplate.of(queryTemplateCompactConfig);
        

        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        QueryTemplateState<Query<MyEntity>> stateCompact = qtCompact.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());
        LOG.debug("Compact result:\n" + stateCompact.getQueryString());

        // filterPrp1 filled -> its clause is present; the filterPrp2-only parenthesis is
        // omitted because filterPrp2 is empty.
        assertThat(state.getQueryString(), containsString("empl.att1 = :filterPrp1"));
        assertThat(state.getQueryString(), not(containsString(":filterPrp2")));
        // The repeat token unfolds filterPrp7/filterPrp8 into indexed parameters.
        assertThat(state.getQueryString(), containsString(":filterPrp7_0"));
        assertThat(state.getQueryString(), containsString(":filterPrp8_2"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrp7_0")));
    }

    @Test
    public void buildQueryWithParenthesisDifferentParamName() {
        String query = BUILD_QUERY_WITH_PARENTHESIS;
        query = query.replaceAll(":filterPrp", ":filterPrm");

        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "bla",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        QueryTemplateConfig<Query<MyEntity>> queryTemplateCompactConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType())
        			.compactQueryText(true);
        
        this.configPropertyMappersDifferentParamName(queryTemplateConfig);
        this.configPropertyMappersDifferentParamName(queryTemplateCompactConfig);
        
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        QueryTemplate<Query<MyEntity>> qtCompact = QueryTemplate.of(queryTemplateCompactConfig);
        

        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        QueryTemplateState<Query<MyEntity>> stateCompact = qtCompact.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());
        LOG.debug("Compact result:\n" + stateCompact.getQueryString());

        // filterPrp1 filled -> its clause is present; the filterPrp2-only parenthesis is
        // omitted because filterPrp2 is empty.
        assertThat(state.getQueryString(), containsString("empl.att1 = :filterPrm1"));
        assertThat(state.getQueryString(), not(containsString(":filterPrm2")));
        // The repeat token unfolds filterPrp7/filterPrp8 into indexed parameters.
        assertThat(state.getQueryString(), containsString(":filterPrm7_0"));
        assertThat(state.getQueryString(), containsString(":filterPrm8_2"));

        QueryMock<MyEntity> queryMock = new QueryMock<>();
        qt.setParamQuery(state, queryMock.getQuery());
        assertThat(queryMock.getParameterCalls(), hasItem(containsString("filterPrm7_0")));
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
    @Test
    public void buildQueryWithQueryHelper() {
        String helperQuery = BUILD_QUERY_WITH_QUERY_HELPER_HELPER_QUERY;

        String query = BUILD_QUERY_WITH_QUERY_HELPER;

        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "bla",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });
        
        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        this.configPropertyMappers(queryTemplateConfig);

		queryTemplateConfig.addQueryHelper("EmployeeQH", helperQuery);
        
        //QueryTemplate qtHelper = new QueryTemplate(helperQuery, mappers);
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        //qt.addQueryHelper("EmployeeQH", qtHelper);

        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // The helper query is inlined and its filled criterion is present.
        assertThat(state.getQueryString(), containsString("FUNC.att1 = :filterPrp1"));
        assertThat(state.getQueryString(), containsString("FUNC_H_SQ"));
    }

    @Test
    public void buildQueryWithQueryHelperDifferentParamName() {
        String helperQuery = BUILD_QUERY_WITH_QUERY_HELPER_HELPER_QUERY;
        helperQuery = helperQuery.replaceAll(":filterPrp", ":filterPrm");

        String query = BUILD_QUERY_WITH_QUERY_HELPER;
        query = query.replaceAll(":filterPrp", ":filterPrm");

        MyFilter mf = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "bla",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });
        
        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());
        this.configPropertyMappersDifferentParamName(queryTemplateConfig);

		queryTemplateConfig.addQueryHelper("EmployeeQH", helperQuery);
        
        //QueryTemplate qtHelper = new QueryTemplate(helperQuery, mappers);
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);
        //qt.addQueryHelper("EmployeeQH", qtHelper);

        QueryTemplateState<Query<MyEntity>> state = qt.buildQueryState(mf);
        LOG.debug("Result:\n" + state.getQueryString());

        // The helper query is inlined and its filled criterion is present.
        assertThat(state.getQueryString(), containsString("FUNC.att1 = :filterPrm1"));
        assertThat(state.getQueryString(), containsString("FUNC_H_SQ"));
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
            "     [!filterPrp5][no_operator][ SELECT 'NADA' FROM DUAL]" +
            "   ) FUNC_H_SQ\n";
    
    @Test
    public void conditionedQueryHelper() {
        String helperQuery = CONDITIONED_QUERY_HELPER_QUERY_HELPER;

        String query = CONDITIONED_QUERY_HELPER_QUERY;

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());

		queryTemplateConfig.addQueryHelper("EmployeeQH", helperQuery);
		this.configPropertyMappers(queryTemplateConfig);
		
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);

        // filterPrp5 filled -> helper is used.
        MyFilter filled = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "bla",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });
        QueryTemplateState<Query<MyEntity>> stateFilled = qt.buildQueryState(filled);
        LOG.debug("Filled:\n" + stateFilled.getQueryString());
        assertThat(stateFilled.getQueryString(), containsString("EMPLOYEESTB"));
        assertThat(stateFilled.getQueryString(), not(containsString("SELECT 'NADA' FROM DUAL")));

        // filterPrp5 empty -> fallback SELECT is used.
        MyFilter empty = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });
        QueryTemplateState<Query<MyEntity>> stateEmpty = qt.buildQueryState(empty);
        LOG.debug("Empty:\n" + stateEmpty.getQueryString());
        assertThat(stateEmpty.getQueryString(), containsString("SELECT 'NADA' FROM DUAL"));
    }
    
    @Test
    public void conditionedQueryHelperDifferentParamName() {
        String helperQuery = CONDITIONED_QUERY_HELPER_QUERY_HELPER;
        helperQuery = helperQuery.replaceAll(":filterPrp", ":filterPrm");

        String query = CONDITIONED_QUERY_HELPER_QUERY;
        query = query.replaceAll(":filterPrp", ":filterPrm");

        QueryTemplateConfig<Query<MyEntity>> queryTemplateConfig =
        		QueryTemplateConfig.of(query, new SimpleTypeToken<Query<MyEntity>>(){}.getRawType());

		queryTemplateConfig.addQueryHelper("EmployeeQH", helperQuery);
		this.configPropertyMappersDifferentParamName(queryTemplateConfig);
		
        QueryTemplate<Query<MyEntity>> qt = QueryTemplate.of(queryTemplateConfig);

        // filterPrp5 filled -> helper is used.
        MyFilter filled = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "bla",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });
        QueryTemplateState<Query<MyEntity>> stateFilled = qt.buildQueryState(filled);
        LOG.debug("Filled:\n" + stateFilled.getQueryString());
        assertThat(stateFilled.getQueryString(), containsString("EMPLOYEESTB"));
        assertThat(stateFilled.getQueryString(), not(containsString("SELECT 'NADA' FROM DUAL")));

        // filterPrp5 empty -> fallback SELECT is used.
        MyFilter empty = new MyFilter("foo1", "", "foo3", Arrays.asList("foo", "baa"), "",
                null, Arrays.asList(1, 2, 3), new Integer[] { 4, 5, 6 });
        QueryTemplateState<Query<MyEntity>> stateEmpty = qt.buildQueryState(empty);
        LOG.debug("Empty:\n" + stateEmpty.getQueryString());
        assertThat(stateEmpty.getQueryString(), containsString("SELECT 'NADA' FROM DUAL"));
    }
}
