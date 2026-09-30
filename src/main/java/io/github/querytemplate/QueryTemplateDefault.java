package io.github.querytemplate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryTemplateDefault<Q> implements QueryTemplateInternal<Q> {

	/** Used to temporarily replace the character. */
    private static String TEMPORARY_ESCAPE_PREFIX = "_PREF_TEMP_ESC_";
	
    private static final Logger LOG = LoggerFactory.getLogger(QueryTemplateDefault.class);

    /** Possible parameters in the query. */
    private Set<PropertyMapper<Q, ?>> usableMappers = null;

    //private boolean compactQueryText = false;

    /** Regular expression that splits the query string into the parts to be assembled. */
    private String splitterIntoPartsPattern = "";

    /** Fixed parts of the query. */
    private String[] queryTextParts;
    /** Variable parts of the query. */
    private List<QueryTemplateTokenPojo> queryTextTokens;

    /** QueryTemplate used as Helpers to assemble inner parts of the query. */
    private Map<String, QueryTemplateInternal<Q>> queryHelpers = new LinkedHashMap<>();

    /** Query after compaction and substitution. */
    private String queryTextEscapedSubs = "";
    
    //private List<PropertyMapper<Q, ?>> mappers = new ArrayList<>();
    //private Map<String, PropertyMapper<Q, ?>> mappersMap;
    private Map<String, PropertyMapper<Q, ?>> mappersByParamNameMap;
    private Map<String, PropertyMapper<Q, ?>> mappersByPropertyNameMap;
    
    //private String sqlHqlQueryOriginal;
    
    private QueryTemplateConfig<Q> config;
    
    private QueryTemplateInternal<Q> parent;
    
    /**
     * TODO: Make a QueryTemplateBuilder Using the Builder Pattern to make it easier to build the object with all the parameters sharing then with QueryTemplateConfig.
     * Builds the object passing all the possible parameters.
     */
    QueryTemplateDefault(QueryTemplateConfig<Q> config) {
		this.config = config;
        this.setUp();
    }
    
    QueryTemplateDefault(QueryTemplateConfig<Q> config, QueryTemplateInternal<Q> parent) {
		this.config = config;
		this.parent = parent;
        this.setUp();
    }
    
    @Override
	public void setUp() {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Setting up QueryTemplateDefault with config: " + this.config);
		}
    	
    	String queryText = this.config.getQueryTextOriginal();
    	
		this.mappersByParamNameMap = new LinkedHashMap<>();
		this.mappersByPropertyNameMap = new LinkedHashMap<>();
    	for (String propertyNameItem : this.config.getMappersConfig().keySet()) {
    		PropertyMapperConfig<Q, Object> mapperConfig = (PropertyMapperConfig<Q, Object>) config.getMappersConfig().get(propertyNameItem);
    		PropertyMapper<Q, Object> mapper = 
    				new PropertyMapper<>(propertyNameItem);
    		mapper
    			.onParticipatesNamed(mapperConfig.getOnParticipatesNamed())
    			.onParticipatesPositional(mapperConfig.getOnParticipatesPositional())
    			.participatesInQuery(mapperConfig.getParticipatesInQuery());
    		if (mapperConfig.getParameterMappers().isEmpty()) {
    			mapper.addParameter(mapper.getFilterPrp())
    				.repeater(false)
    				.unpackListItems(false);
    		} else {
    			for (String parameterName : mapperConfig.getParameterMappers().keySet()) {
    				ParameterMapperConfig<Q, Object> parameterMapperConfig = (ParameterMapperConfig<Q, Object>) mapperConfig.getParameterMappers().get(parameterName);
    				mapper.addParameter(parameterName)
    					.repeater(parameterMapperConfig.isRepeater())
    					.unpackListItems(parameterMapperConfig.isUnpackListItems());
    			}
    		}
			for (String parameterName : mapper.getParameterMappers().keySet()) {
				ParameterMapper<Q, Object> parameterMapper = mapper.getParameterMappers().get(parameterName);
	    		this.mappersByParamNameMap.put(parameterMapper.getParameterName(), mapper);
			}
    		this.mappersByPropertyNameMap.put(mapper.getFilterPrp(), mapper);
    	}
    	
        if (this.config.isCompactQueryText()) {
            queryText = this.compactInputQueryText(this.config.getQueryTextOriginal());
        }

        this.usableMappers = new LinkedHashSet<>();

        this.splitterIntoPartsPattern = "(?s)" + this.config.getFiltersToken() + "|" + this.config.getWhereToken() + "|" + this.config.getAndToken() + "|" + this.config.getOrToken()
                + "|" + this.config.getNoOperatorToken() + "|" + this.config.getRepeatToken() + "|" + this.config.getOpenParenthesisToken() + "|"
                + this.config.getCloseParenthesisToken() + "|" + this.config.getExtraToken() + "|" + this.config.getPropertiesToken() + "|" + this.config.getCriterionToken();
        Pattern pt = Pattern.compile(this.splitterIntoPartsPattern);

        Map<String, String> escapeMap = new LinkedHashMap<>();
        this.queryTextEscapedSubs = this.replaceEscapes(queryText, escapeMap);

        this.queryTextParts = pt.split(this.queryTextEscapedSubs, -1);
        
		if (LOG.isDebugEnabled()) {
			LOG.debug("Splitting query text into parts with pattern: " + this.splitterIntoPartsPattern);
			LOG.debug("Resulting parts: " + this.queryTextParts.length + " parts.");
		}
        
        this.queryTextTokens = this.buildTokens(this.queryTextEscapedSubs, this.queryTextParts.length, pt,
                this.usableMappers);

        this.restoreEscapes(this.queryTextParts, escapeMap);
        this.restoreEscapesTokens(this.queryTextTokens, escapeMap);
        
        this.setUpQueryHelpers();

        if (LOG.isTraceEnabled()) {
            StringBuilder logStr = new StringBuilder("\nStatic parts:\n");
            for (int i = 0; i < this.queryTextParts.length; i++) {
                logStr.append(i).append(": ").append(this.queryTextParts[i]).append("\n");
            }
            logStr.append("Parts to assemble:\n");
            for (int i = 0; i < this.queryTextTokens.size(); i++) {
                logStr.append(i).append(": ").append(this.queryTextTokens.get(i).getValue()).append("\n");
            }
            LOG.trace(logStr.toString());
        }
    }
    
    /**
     * Sets up the query helpers, which are other QueryTemplate instances that will be used to assemble inner parts of the query.
     * Implicitly recursive, since the QueryTemplate constructor calls setUp(), which calls setUpQueryHelpers() again for the inner query helpers.
     */
    private void setUpQueryHelpers() {
    	for (String queryHelperKey : this.config.getQueryHelpers().keySet()) {
    		QueryTemplateConfig<Q> queryHelperConfig = this.config.getQueryHelpers().get(queryHelperKey);
    		// Implicitly recursive, since the QueryTemplate constructor calls setUp(), which calls setUpQueryHelpers() again for the inner query helpers.
    		QueryTemplateInternal<Q> queryTemplateHelper = (QueryTemplateInternal<Q>) QueryTemplateInternal.of(queryHelperConfig, this);
    		
    		this.queryHelpers.put(
    				queryHelperKey,
    				queryTemplateHelper
			);
    	}
    }

    /**
     * Compacts the input query, removing line breaks and triple spaces.
     *
     * @param queryText the query.
     * @return the compacted query.
     */
    private String compactInputQueryText(String queryText) {
    	String doubleBlanckPatternStr = "(\"[^\"]*\"|'[^']*')|\\s+";
    	String blockCommentPatternStr = "(?s)/\\\\*[\\\\s\\\\S]*?\\\\*/";
    	String commentPatternStr = "(?m)^\\s*--.*$";
    	
    	String compacted = queryText;

        int length = compacted.length();
        do {
            compacted = compacted
            		.replaceAll(commentPatternStr, "")
            		.replaceAll(blockCommentPatternStr, "")
            		.replaceAll(doubleBlanckPatternStr, "$1 ");
            if (compacted.length() == length) {
                break;
            } else {
                length = compacted.length();
            }
        } while (true);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Compacted query text: " + compacted.length() 
			+ " characters. Original query text: " + queryText.length() 
			+ " characters.");
		}
        return compacted;
    }

//    /**
//     * Builds using the default parameters.
//     *
//     * @param sqlHqlQuery query containing the tokens for substitution.
//     */
//    public QueryTemplate(String sqlHqlQuery, List<PropertyMapper<Q, ?>> mappers) {
//        this(sqlHqlQuery, mappers, FILTERS_TOKEN, WHERE_TOKEN, AND_TOKEN,
//                OR_TOKEN, NO_OPERATOR_TOKEN, OPEN_PARENTHESIS_TOKEN, CLOSE_PARENTHESIS_TOKEN,
//                EXTRA_TOKEN, PARAM_TOKEN, REPEAT_TOKEN, CRITERION_TOKEN, PARAM_DELIMITER_TOKEN,
//                CRITERION_DELIMITER_TOKEN, QUERY_HELPER_TOKEN, ESCAPE_CHARACTER, false,
//                RESERVED_ANY_PARAM);
//        this.convertNamedToPositionalParameters(false);
//        this.targetReservedWordWhere(TARGET_RESERVED_WORD_WHERE);
//        this.targetReservedWordAnd(TARGET_RESERVED_WORD_AND);
//        this.targetReservedWordOr(TARGET_RESERVED_WORD_OR);
//        this.targetReservedWordOpenParenthesis(TARGET_RESERVED_WORD_OPEN_PARENTHESIS);
//        this.targetReservedWordCloseParenthesis(TARGET_RESERVED_WORD_CLOSE_PARENTHESIS);
//        this.targetReservedWordPositionalParameterMarker(TARGET_RESERVED_WORD_POSITIONAL_PARAMETER_MARKER);
//        
//        this.setUp();
//    }

//    /**
//     * @param sqlHqlQuery        query containing the tokens for substitution.
//     * @param compactSqlHqlQuery whether the query must be compacted.
//     */
//    public QueryTemplate(String sqlHqlQuery, List<PropertyMapper<Q, ?>> mappers, boolean compactSqlHqlQuery) {
//        this(sqlHqlQuery, mappers, FILTERS_TOKEN, WHERE_TOKEN, AND_TOKEN,
//                OR_TOKEN, NO_OPERATOR_TOKEN, OPEN_PARENTHESIS_TOKEN, CLOSE_PARENTHESIS_TOKEN,
//                EXTRA_TOKEN, PARAM_TOKEN, REPEAT_TOKEN, CRITERION_TOKEN, PARAM_DELIMITER_TOKEN,
//                CRITERION_DELIMITER_TOKEN, QUERY_HELPER_TOKEN, ESCAPE_CHARACTER, compactSqlHqlQuery,
//                RESERVED_ANY_PARAM);
//        this.convertNamedToPositionalParameters(false);
//        this.targetReservedWordWhere(TARGET_RESERVED_WORD_WHERE);
//        this.targetReservedWordAnd(TARGET_RESERVED_WORD_AND);
//        this.targetReservedWordOr(TARGET_RESERVED_WORD_OR);
//        this.targetReservedWordOpenParenthesis(TARGET_RESERVED_WORD_OPEN_PARENTHESIS);
//        this.targetReservedWordCloseParenthesis(TARGET_RESERVED_WORD_CLOSE_PARENTHESIS);
//        this.targetReservedWordPositionalParameterMarker(TARGET_RESERVED_WORD_POSITIONAL_PARAMETER_MARKER);
//        
//        this.setUp();
//    }

    /**
     * Assigns values to the parameters based on the participation of the properties in the query.
     * This method does not process inner queries, only the current query.
     *
     * @param filterObject     the filter object.
     * @param query            the query to set the parameters on.
     */
    void setParamQueryNonRecursive(QueryTemplateStateInternal<Q> state, Q query, Map<Integer, Action> positionalParameterActions) {
        for (String parameterName : this.mappersByParamNameMap.keySet()) {
        	PropertyMapper<Q, ?> mapperItem = this.mappersByParamNameMap.get(parameterName);
        	PropertyMapper<Q, Object> mapperItemCasted = (PropertyMapper<Q, Object>) mapperItem;
        	ParameterMapper<Q, Object> parameterMapper = mapperItemCasted.getParameterMappers().get(parameterName);
            if (LOG.isDebugEnabled()) {
            	LOG.debug("Tests whether the parameter is used in the query and whether it is participating in the query, based on the filter object.");
            	if (!this.usableMappers.contains(mapperItemCasted)) {
            		LOG.debug("Property '" + mapperItemCasted.getFilterPrp() + "/Parameters: " + mapperItemCasted.getParameterMappers().keySet()+ " ' are not usable in the query. The parameter "+this.config.getParameterUsagePrefix()+ parameterName + " is not present in query text. ");
            	} else {
            		LOG.debug("Property '" + mapperItemCasted.getFilterPrp() + "/Parameters: " + mapperItemCasted.getParameterMappers().keySet()+ " ' are usable in the query. The parameter "+ parameterName + " is present in query text. ");
            	}
				if (mapperItemCasted.getParticipatesInQuery().isParticipating(state.getFilter(),
						mapperItemCasted.getFilterPrp())) {
					LOG.debug("Property '" + mapperItemCasted.getFilterPrp() + " is participating in the query. `ParticipatesInQuery.isParticipating()` returned true.");
				} else {
					LOG.debug("Property '" + mapperItemCasted.getFilterPrp() + " is NOT participating in the query. `ParticipatesInQuery.isParticipating()` returned false.");
	            }
            }
        	
            // Tests whether the parameter is used in the query and whether it is participating in the query, based on the filter object.
            if ( this.usableMappers.contains(mapperItemCasted)
                    && mapperItemCasted.getParticipatesInQuery().isParticipating(state.getFilter(), mapperItemCasted.getFilterPrp()) ) {
                try {
                    Object value = PropertyUtils.getProperty(state.getFilter(), mapperItemCasted.getFilterPrp());
                    
                    //if (mapperItemCasted.isUnfoldEnumerable()) {
                    if (parameterMapper.isUnpackListItems() || parameterMapper.isRepeater()) {
                    	Collection<Object> valueColl;
                    	if (value.getClass().isArray()) {
                    		valueColl = new ArrayList<>();
                    		int len = Array.getLength(value);
                    		for (int i = 0; i < len; i++) {
                    			valueColl.add(Array.get(value, i));
                    		}
                    	} else {
                    		valueColl = CollectionUtil.toCollection(value);
                    	}
                    	int repeatIndex = 0;
                    	for (Object valueItem : valueColl) {
                    		if (!this.config.isConvertNamedToPositionalParameters()) {
                    			if (mapperItemCasted.getOnParticipatesNamed() != null
                    					&& state.getQueryString().contains(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName())) {
                        			AssignedParameterInfoInternal<?> assignedParameterInfo = new AssignedParameterInfoDefault<>();
                        			assignedParameterInfo.setPosition(null);
                        			assignedParameterInfo.setIndex(repeatIndex);
                        			assignedParameterInfo.setName(parameterMapper.getParameterName());
                        			assignedParameterInfo.setUnpackedRepeatedName(parameterMapper.getParameterName() + "_" + repeatIndex);
                    				mapperItemCasted.getOnParticipatesNamed().accept(query, assignedParameterInfo.getUnpackedRepeatedName(), valueItem, (AssignedParameterInfo<Object>) assignedParameterInfo);
                    			}
                    		} else {
                    			if (mapperItemCasted.getOnParticipatesPositional() != null) {
                    				List<AssignedParameterInfo<?>> assignedParameterInfos = state
                    						.getPropertyMapperItemIndexToAssignedParameterInfo().get(mapperItemCasted)
                    						.get(repeatIndex);
                    				for (AssignedParameterInfo<?> assignedParameterInfo : assignedParameterInfos) {
                    					// delaying the execution of the parameter setting to avoid issues with the order of execution and potential side effects
                    					positionalParameterActions.put(assignedParameterInfo.getPosition(), () -> mapperItemCasted.getOnParticipatesPositional().accept(query, assignedParameterInfo.getPosition(), valueItem, (AssignedParameterInfo<Object>) assignedParameterInfo));
                    				}
                    			}
                    		}
                    		repeatIndex++;
                    	}
                    } else if (value instanceof FragmentInclusion) {
                    	// nothing
                    } else {
                    	if (!this.config.isConvertNamedToPositionalParameters()) {
                    		if (mapperItemCasted.getOnParticipatesNamed() != null
                    				&& state.getQueryString().contains(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName())) {
                    			AssignedParameterInfoInternal<?> assignedParameterInfo = new AssignedParameterInfoDefault<>();
                    			assignedParameterInfo.setPosition(null);
                    			assignedParameterInfo.setIndex(null);
                    			assignedParameterInfo.setName(parameterMapper.getParameterName());
                    			assignedParameterInfo.setUnpackedRepeatedName(null);
                    			mapperItemCasted.getOnParticipatesNamed().accept(query, parameterMapper.getParameterName(), value, (AssignedParameterInfo<Object>) assignedParameterInfo);
                    		}                    		
                    	} else {
                    		if (mapperItemCasted.getOnParticipatesPositional() != null) {
                    			List<AssignedParameterInfo<?>> parameterPositions = state.getPropertyMapperToAssignedParameterInfo().get(mapperItemCasted);
                    			for (AssignedParameterInfo<?> assignedParameterInfo : parameterPositions) {
                    				// delaying the execution of the parameter setting to avoid issues with the order of execution and potential side effects
                    				positionalParameterActions.put(assignedParameterInfo.getPosition(), () -> mapperItemCasted.getOnParticipatesPositional().accept(query, assignedParameterInfo.getPosition(), value, (AssignedParameterInfo<Object>) assignedParameterInfo));
                    			}
                    		}
                    	}
                    }
                } catch (Exception e) {
                    throw new QueryTemplateException("Error while trying to set parameter: " + mapperItemCasted.getFilterPrp(), e);
                }
            }
        }
    }
    
    @Override
    public void setParamQueryRecursive(QueryTemplateState<Q> state, Q query, Map<Integer, Action> positionalParameterActions) {
        // Setting the parameters of the inner queries.
        for (String queryHelperKey : this.queryHelpers.keySet()) {
            if (this.queryHelpers.containsKey(queryHelperKey)) {
            	QueryTemplateInternal<Q> queryHelper = this.queryHelpers.get(queryHelperKey);
                queryHelper.setParamQueryRecursive(state, query, positionalParameterActions);
            } else {
                throw new QueryTemplateException(
                        "Query helper not loaded, use 'addQueryHelper'. Key: '" + queryHelperKey + "'");
            }
        }

        this.setParamQueryNonRecursive((QueryTemplateStateInternal<Q>) state, query, positionalParameterActions);
    }

    /**
     * Assigns values to the parameters based on the participation of the properties in the query, including inner queries.
     *
     * @param positionalParameterActions a map of parameter positions to actions that set the parameter values on the query. This is used to delay the execution of setting the parameters until all actions are collected, ensuring that they are executed in order of their target parameter positions.
     * @param state        the state of the query template.
     * @param query        the query to set the parameters on.
     */
    @Override
	public void setParamQuery(QueryTemplateState<Q> state, Q query,  Map<Integer, Action> positionalParameterActions) {
    	this.setParamQueryRecursive(state, query, positionalParameterActions);
    	
		if (this.config.isConvertNamedToPositionalParameters()) {
			for (int i = this.config.getParameterBasePosition(); i < positionalParameterActions.size() + this.config.getParameterBasePosition(); i++) {
				if (!positionalParameterActions.containsKey(i)) {
					throw new QueryTemplateException("No action found for parameter position " + i);
				}
				Action action = positionalParameterActions.get(i);
				// Executing the action to set the parameter value on the query. This is done in order of the target parameter positions to avoid issues with the order of execution and potential side effects.
				action.execute();
			}
		}
    }
    
    /**
     * Assigns values to the parameters based on the participation of the properties in the query, including inner queries.
     *
     * @param state 	the state of the query template.
     * @param query 	the query to set the parameters on.
     */
    @Override
	public void setParamQuery(QueryTemplateState<Q> state, Q query) {
    	Map<Integer, Action> positionalParameterActions = new LinkedHashMap<>();
    	this.setParamQuery(state, query, positionalParameterActions);
    }

	private void processPositionalParameters(QueryTemplateStateInternal<Q> state) {
		if (this.config.isConvertNamedToPositionalParameters()) {
			String parameterWithIndexSufixPatternStr = this.config.getParameterUsagePrefix() + this.config.getParameterNamePattern();
			String indexSufixPatternStr = "^(.+)_(\\d+)$";
			Pattern parameterWithIndexSufixPattern = Pattern.compile(parameterWithIndexSufixPatternStr);
			Pattern indexSufixPattern = Pattern.compile(indexSufixPatternStr);
			
			Matcher matcher = parameterWithIndexSufixPattern.matcher(state.getQueryString());
			int parameterPosition = this.config.getParameterBasePosition();
			int matcherIndex = 0;
			
			StringBuilder positionalPametersQueryString = new StringBuilder();
			while (true) {
				if (!matcher.find(matcherIndex)) {
					break;
				}
				positionalPametersQueryString.append(state.getQueryString().substring(matcherIndex, matcher.start()));
				positionalPametersQueryString.append(this.config.getTargetReservedWordPositionalParameterMarker());
				matcherIndex = matcher.end();
				String parameterWithIndexSufix = matcher.group(1);
				String parameterName = "";
				String parameterIndexSufix = null;
				Matcher indexSufixMatcher = indexSufixPattern.matcher(parameterWithIndexSufix);
				indexSufixMatcher.find();
				if (!indexSufixMatcher.matches()) {
					parameterName = parameterWithIndexSufix;
				} else {
					parameterName = indexSufixMatcher.group(1);
					parameterIndexSufix = indexSufixMatcher.group(2);
				}
				if (!this.mappersByParamNameMap.containsKey(parameterName)) {
					throw new QueryTemplateException(
							"Parameter name '" + parameterName + "' is not present in the mappers.");
				}
				if (!parameterWithIndexSufix.equals(parameterName) && this.mappersByParamNameMap.containsKey(parameterWithIndexSufix) && this.mappersByParamNameMap.containsKey(parameterName)) {
					throw new QueryTemplateException("Parameter with index sufix '" + parameterWithIndexSufix + "' and parameter name '" + parameterName + "' are both present in the mappers map. This is not allowed.");
                }

				PropertyMapper<Q, ?> mapperItem = this.mappersByParamNameMap.get(parameterName);
				ParameterMapper<Q, ?> parameterMapper = mapperItem.getParameterMappers().get(parameterName);
				//if (mapperItem.isUnfoldEnumerable()) {
				//if (state.getIsRepeatablePropertyMapper().get(mapperItem)) {
				if (parameterMapper.isRepeater() || parameterMapper.isUnpackListItems()) {
					if (!state.getPropertyMapperItemIndexToAssignedParameterInfo().containsKey(mapperItem)) {
	                    state.getPropertyMapperItemIndexToAssignedParameterInfo().put(mapperItem, new LinkedHashMap<>());
	                }
					Map<Integer, List<AssignedParameterInfo<?>>> itemIndexToAssignedParameterInfo = state.getPropertyMapperItemIndexToAssignedParameterInfo().get(mapperItem);
					int itemIndex = Integer.parseInt(parameterIndexSufix);
					if (!itemIndexToAssignedParameterInfo.containsKey(itemIndex)) {
						itemIndexToAssignedParameterInfo.put(itemIndex, new ArrayList<>());
					}
					AssignedParameterInfoInternal<?> assignedParameterInfo = new AssignedParameterInfoDefault<>();
					assignedParameterInfo.setPosition(parameterPosition);
					assignedParameterInfo.setName(parameterName);
					assignedParameterInfo.setUnpackedRepeatedName(parameterWithIndexSufix);
					assignedParameterInfo.setIndex(itemIndex);
					itemIndexToAssignedParameterInfo.get(itemIndex).add(assignedParameterInfo);
					parameterPosition++;
				} else {
					if (!state.getPropertyMapperToAssignedParameterInfo().containsKey(mapperItem)) {
						state.getPropertyMapperToAssignedParameterInfo().put(mapperItem, new ArrayList<>());
					}
					List<AssignedParameterInfo<?>> parameterPositions = state.getPropertyMapperToAssignedParameterInfo().get(mapperItem);
					AssignedParameterInfoInternal<?> assignedParameterInfo = new AssignedParameterInfoDefault<>();
					assignedParameterInfo.setPosition(parameterPosition);
					assignedParameterInfo.setName(parameterName);
					parameterPositions.add(assignedParameterInfo);
					parameterPosition++;
				}
			}
			positionalPametersQueryString.append(state.getQueryString().substring(matcherIndex));
			state.setQueryString(positionalPametersQueryString.toString());
		}
	}
    
//    /**
//     * Assembles the query using the default configuration.
//     * @param <SQ>
//     *
//     * @param mappers              the transfer objects.
//     * @param sqlHqlQuery      the query.
//     * @param filter           the filter object.
//     * @param usableParameters set of the usable parameters in the query. Must be
//     *                         non-null and resettable.
//     * @return the assembled query.
//     */
//    private static <SQ> String buildQueryString(List<PropertyMapper<SQ, ?>> mappers, String sqlHqlQuery,
//            Object filter, Set<String> usableParameters) {
//        QueryTemplate qt = new QueryTemplate(sqlHqlQuery);
//        usableParameters.clear();
//        usableParameters.addAll(qt.getUsableParameters());
//        return qt.buildQueryString(mappers, filter);
//    }

    /**
     * Assembles the query according to the parameters that are participating in the query.
     * @param filter the filter object.
     * @return the assembled query.
     */
    @Override
	public QueryTemplateState<Q> buildQueryState(Object filter) {
    	// TODO: Continuar daqui
    	QueryTemplateStateInternal<Q> state = new QueryTemplateStateDefault<>(this, null, filter, new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    	
        // Auxiliary variable to access the mapper by property name.
        Map<String, PropertyMapper> mappersByPrp = new LinkedHashMap<>();
        for (String filterPrp : this.mappersByPropertyNameMap.keySet()) {
			PropertyMapper<Q, ?> mapperItem = this.mappersByPropertyNameMap.get(filterPrp);
            if (mappersByPrp.containsKey(mapperItem.getFilterPrp())) {
                throw new QueryTemplateException(
                        String.format("Error while adding property '%s'", mapperItem.getFilterPrp()));
            }
            mappersByPrp.put(mapperItem.getFilterPrp(), mapperItem);
		}

        StringBuilder queryTextMod = new StringBuilder();

        // Whether any parameter was already participating in the query.
        boolean anyParam = false;
        // Connector.
        String connector = " "+this.config.getTargetReservedWordWhere()+" ";

        for (int i = 0; i < this.queryTextParts.length; i++) {
            queryTextMod.append(this.queryTextParts[i]);

            if (i < this.queryTextTokens.size() && this.queryTextTokens.get(i) != null) {
                QueryTemplateTokenPojo.TokenType type = this.queryTextTokens.get(i).getType();
                if (type == QueryTemplateTokenPojo.TokenType.FILTERS) {
                    anyParam = false;
                    connector = " "+this.config.getTargetReservedWordAnd()+" ";
                } else if (type == QueryTemplateTokenPojo.TokenType.WHERE) {
                    connector = " "+this.config.getTargetReservedWordWhere()+" ";
                } else if (type == QueryTemplateTokenPojo.TokenType.AND) {
                    if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                        connector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.OR) {
                    if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                        connector = " "+this.config.getTargetReservedWordOr()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.NO_OPERATOR) {
                    if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                        connector = "  ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.OPEN_PARENTHESIS) {
                    //int[] index = { i };
                	//boolean[] anyParamParenthesis = { false };
                	OutputParam<Integer> index =  new OutputParam<Integer>(i);
                	OutputParam<Boolean> anyParamParenthesis = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParenthesisCriterion(index, connector, anyParamParenthesis,
                            filter, state));
                    i = index.getValue();
                    anyParam = anyParam || anyParamParenthesis.getValue();
                    if (anyParam) {
                        connector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.EXTRA) {
                    i++;
                    if (i >= this.queryTextTokens.size()) {
                        throw new QueryTemplateException(
                                QueryTemplateTokenPojo.TokenType.CRITERION + " Expected\n"
                                        + this.tokenExceptionMessage(
                                                Math.min(i, this.queryTextTokens.size() - 1),
                                                this.queryTextTokens, "UNEXPECTED TOKEN (AFTER)"));
                    }
                    // Adding connector if it exists.
                    if (this.queryTextTokens.get(i).getType() == QueryTemplateTokenPojo.TokenType.AND) {
                        i++;
                        if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                            connector = " "+this.config.getTargetReservedWordAnd()+" ";
                        }
                    } else if (this.queryTextTokens.get(i).getType() == QueryTemplateTokenPojo.TokenType.OR) {
                        i++;
                        if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                            connector = " "+this.config.getTargetReservedWordOr()+" ";
                        }
                    }

                    if (i >= this.queryTextTokens.size()
                            || this.queryTextTokens.get(i).getType() != QueryTemplateTokenPojo.TokenType.CRITERION) {
                        String msgExParent = "";
                        if (i >= this.queryTextTokens.size()) {
                            msgExParent = "UNEXPECTED TOKEN (AFTER)";
                        }
                        throw new QueryTemplateException(
                                QueryTemplateTokenPojo.TokenType.CRITERION + " Expected\n"
                                        + this.tokenExceptionMessage(
                                                Math.min(i, this.queryTextTokens.size() - 1),
                                                this.queryTextTokens, msgExParent));
                    }
                    if (anyParam) {
                        queryTextMod.append(connector).append(this.queryTextTokens.get(i).getValue());
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.QUERY_HELPER) {
                    queryTextMod.append(" ")
                            .append(this.buildQueryHelper(filter, this.queryTextTokens.get(i))).append(" ");
                } else if (type == QueryTemplateTokenPojo.TokenType.PARAMETER) {
                    //int[] index = { i };
                	//boolean[] paramParticipates = { false };
                	OutputParam<Integer> index =  new OutputParam<Integer>(i);
                	OutputParam<Boolean> paramParticipates = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParameterCriterion(index, connector, paramParticipates, anyParam,
                            filter, state));
                    i = index.getValue();
                    anyParam = anyParam || paramParticipates.getValue();
                    if (anyParam) {
                        connector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else {
                    throw new QueryTemplateException(
                            this.tokenExceptionMessage(i, this.queryTextTokens, "UNEXPECTED TOKEN"));
                }
            }
        }
        
        state.setQueryString(queryTextMod.toString());
        
        this.processPositionalParameters(state);
        
        return state;
    }

    /**
     * Returns an unexpected token message.
     *
     * @param tokenIndex   the token index.
     * @param tokens       the tokens.
     * @param tokenMessage the message for the token.
     * @return the message.
     */
    private String tokenExceptionMessage(int tokenIndex, List<QueryTemplateTokenPojo> tokens, String tokenMessage) {
        return String.format("%3$s. Type='%1$s'; Value :'%2$s'. \nError: %4$s\n",
                tokens.get(tokenIndex).getType().toString(),
                tokens.get(tokenIndex).getValue(),
                tokenMessage,
                "\n" + this.showErrorPosition(this.queryTextEscapedSubs, tokens.get(tokenIndex).getIndexInString()));
    }

    /**
     * Shows the position of the error.
     *
     * @param textWithError the text containing the error.
     * @param errorIndex    the error index in {@code textWithError}.
     * @return the position representation.
     */
    private String showErrorPosition(String textWithError, int errorIndex) {
        StringBuilder sb = new StringBuilder();
        char[] textWithErrorCA = textWithError.toCharArray();
        int indexInLine = 0;
        for (int i = 0; i < textWithErrorCA.length; i++) {
            // Ends at the line where the error occurs.
            if (i > errorIndex && textWithErrorCA[i] == '\n') {
                break;
            }

            sb.append(textWithErrorCA[i]);
            if (textWithErrorCA[i] == '\n') {
                indexInLine = 0;
            } else if (i < errorIndex) {
                indexInLine++;
            }
        }

        sb.append("\n");
        for (int i = 0; i < indexInLine; i++) {
            sb.append(' ');
        }
        sb.append('^');
        return sb.toString();
    }

    private String unpackArrayParamCriterionIfNecessary(Object filter, List<PropertyMapper<Q, ?>> criterionMappers, String criterion, QueryTemplateState<Q> state) {
        StringBuilder repeatedCriterion = new StringBuilder();

        StringBuilder filterPrpsStr = new StringBuilder();
        String comma = "";
        // Checking the existence of some enumerable.
        Map<PropertyMapper, Object> unpackableValues = new LinkedHashMap<>();
        for (PropertyMapper<Q, ?> mapper : criterionMappers) {
            Object value = PropertyUtils.getProperty(filter, mapper.getFilterPrp());

            // In the case `any` is used one or more properties can be not participating in the query.
            if (mapper.getParticipatesInQuery().isParticipating(filter, mapper.getFilterPrp())) {
            	for (String parameterName : mapper.getParameterMappers().keySet()) {
					ParameterMapper<Q, ?> parameterMapper = mapper.getParameterMappers().get(parameterName);
					if (parameterMapper.isUnpackListItems()) {
						unpackableValues.put(mapper, value);
					}
				}
            }
        }

        
        Collection<Object> valueColl = null;

        String currentTargetListItemConnector = "";
        String criterionSub = criterion;
        
		for (PropertyMapper<Q, ?> mapper : unpackableValues.keySet()) {
			Object value = unpackableValues.get(mapper);
            valueColl = CollectionUtil.toCollection(value);
            if (valueColl == null) {
                throw new QueryTemplateException(
                        "Some parameter of enumerable type expected. parameter: '" + filterPrpsStr + "'");
            }
			for (String parameterName : mapper.getParameterMappers().keySet()) {
				ParameterMapper<Q, ?> parameterMapper = mapper.getParameterMappers().get(parameterName);
				if (parameterMapper.isUnpackListItems()) {
					int unpackIndex = 0;
					StringBuilder unpackedParameterReferences = new StringBuilder();
					for (Object valueItem : valueColl) {
						unpackedParameterReferences.append(currentTargetListItemConnector);
						unpackedParameterReferences.append(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName() + "_" + unpackIndex);
						currentTargetListItemConnector = this.config.getTargetItemListSeparatorMarker();
						unpackIndex++;
					}
		        	unpackIndex = 0;
					criterionSub = criterionSub.replace(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName(), unpackedParameterReferences.toString());
				}				
			}
		}

        return criterionSub;
    }
    /**
     * Processes the criterion for an array parameter, repeating it several times.
     * Used when the {repeat} token occurs.
     *
     * @return the repeated criterion.
     */
    private String repeatArrayParamCriterion(String filterPrpsStr, Object filter, List<PropertyMapper<Q, ?>> criterionMappers,
            String repeatConnector, String criterion, QueryTemplateState<Q> state) {
        StringBuilder repeatedCriterion = new StringBuilder();

        String comma = "";
        Collection<Object> firstRepeaterValue = null;
        PropertyMapper firstRepeaterMapper = null;
        Set<PropertyMapper> repeatableMappers = new LinkedHashSet<>();
        // Checking the existence of some enumerable.
        for (PropertyMapper<Q, ?> mapper : criterionMappers) {
            Object value = PropertyUtils.getProperty(filter, mapper.getFilterPrp());

            for (String parameterName : mapper.getParameterMappers().keySet()) {
            	ParameterMapper<Q, ?> parameterMapper = mapper.getParameterMappers().get(parameterName);
            	// In the case `any` is used one or more properties can be not participating in the query.
            	if (value != null) {
            		if (parameterMapper.isRepeater()) {
            			repeatableMappers.add(mapper);
            			if (firstRepeaterValue == null) {
            				firstRepeaterValue = CollectionUtil.toCollection(value);
            				firstRepeaterMapper = mapper;
            			} else {
            				Collection<Object> currentRepeaterValue = CollectionUtil.toCollection(value);
            				if (firstRepeaterValue.size() != currentRepeaterValue.size()) {
            					throw new QueryTemplateException(
            							"All repeatable parameters must have the same number of values. Property '"
            									+ firstRepeaterMapper.getFilterPrp() + "' has " + firstRepeaterValue.size()
            									+ " values, while parameter '" + mapper.getFilterPrp() + "' has "
            									+ currentRepeaterValue.size() + " values.");
            				}
            			}
            		} else {
            			// nothing
            		}
            	} else {
            		// nothing
            	}
			}
        }

        if (firstRepeaterValue == null) {
            throw new QueryTemplateException(
                    "Some parameter of enumerable type expected. parameter: '" + filterPrpsStr + "'");
        }

        String tempConnector = "  ";
        int repeatIndex = 0;
        for (Object valueItem : firstRepeaterValue) {
            repeatedCriterion.append(tempConnector);
            String criterionSub = criterion;
            for (PropertyMapper<Q, ?> repeatableMapper : repeatableMappers) {
            	for (String parameterName : repeatableMapper.getParameterMappers().keySet()) {
					ParameterMapper<Q, ?> parameterMapper = repeatableMapper.getParameterMappers().get(parameterName);
					criterionSub = criterionSub.replace(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName(),
							this.config.getParameterUsagePrefix() + parameterMapper.getParameterName() + "_" + repeatIndex);
				}
            }

            repeatedCriterion.append(criterionSub);

            tempConnector = repeatConnector;
            repeatIndex++;
        }

        return repeatedCriterion.toString();
    }

    private void createEvalRunnerIfNecessary(QueryTemplateStateInternal<Q> state) throws Throwable {
    	if (state.getEvalRunner() == null) {
    		EvalRunner evalRunner = this.config.getEvalRunnerCreator().apply(state);
    		Map<String, Boolean> prpParticipations = new LinkedHashMap<>();
    		Map<String, Object> prpValues = new LinkedHashMap<>();
    		for (String filterPrp : this.mappersByPropertyNameMap.keySet()) {
    			PropertyMapper<Q, ?> mapperItem = this.mappersByPropertyNameMap.get(filterPrp);
    			Object prpValue = PropertyUtils.getProperty(state.getFilter(), filterPrp);
    			Boolean prpParticipation = mapperItem.getParticipatesInQuery().isParticipating(state.getFilter(), filterPrp);
    			prpParticipations.put(filterPrp, prpParticipation);
    			prpValues.put(filterPrp, prpValue);
    		}
			evalRunner.clearBindings(state);
			evalRunner.binding(state, "prpParticipations", prpParticipations);
			evalRunner.binding(state, "pp", prpParticipations);
			evalRunner.binding(state, "prpValues", prpValues);
			evalRunner.binding(state, "pv", prpValues);
			
    		state.setEvalRunner(evalRunner);
    	}
    }
    
    /**
     * Assembles a part of the query that depends on the parameter(s) currently
     * being processed. Returns the part along with the connector.
     */
    private String buildParameterCriterion(OutputParam<Integer> index, String currentConnector,
    		OutputParam<Boolean> paramParticipates, 
    		boolean anyParamBefore, Object filter, 
            QueryTemplateStateInternal<Q> state) {    	
        StringBuilder queryTextMod = new StringBuilder();
        boolean repeatTokenActive = false;
        String repeatConnector = "  ";
        paramParticipates.setValue(false);
        String[] filterPrps = new String[] {};
        String evalPrps = null;
        String filterPrpsStr = "";

        boolean firstIteration = true;
        while (true) {
            if (!firstIteration) {
                queryTextMod.append(this.queryTextParts[index.getValue()]);
            }

            if (index.getValue() < this.queryTextTokens.size() && this.queryTextTokens.get(index.getValue()) != null) {
                QueryTemplateTokenPojo.TokenType type = this.queryTextTokens.get(index.getValue()).getType();
                if (type == QueryTemplateTokenPojo.TokenType.PARAMETER) {
                	String tokenValue = this.queryTextTokens.get(index.getValue()).getValue();
                    Pattern rxReservedEvalReplacer = Pattern.compile(this.config.getReservedEvalProperty());
                    Pattern rxReservedEvalMatcher = Pattern.compile("(?s)\\s*" + this.config.getReservedEvalProperty() + ".*");
                    if (rxReservedEvalMatcher.matcher(tokenValue).matches()) {
                    	evalPrps = rxReservedEvalReplacer.matcher(tokenValue).replaceAll("").trim();
                    } else {
                    	evalPrps = null;
                    	repeatTokenActive = false;
                    	filterPrpsStr = this.queryTextTokens.get(index.getValue()).getValue();
                    	filterPrps = filterPrpsStr.split(",", -1);
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.AND) {
                    repeatConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    if (anyParamBefore) {
                        currentConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.OR) {
                    repeatConnector = " "+this.config.getTargetReservedWordOr()+" ";
                    if (anyParamBefore) {
                        currentConnector = " "+this.config.getTargetReservedWordOr()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.NO_OPERATOR) {
                    repeatConnector = "  ";
                    if (!currentConnector.contains(this.config.getTargetReservedWordWhere())) {
                        currentConnector = "  ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.REPEAT) {
                    repeatTokenActive = true;
                } else if (type == QueryTemplateTokenPojo.TokenType.CRITERION
                        || type == QueryTemplateTokenPojo.TokenType.QUERY_HELPER) {
                	List<PropertyMapper<Q, ?>> criterionMappers = new ArrayList<>();
                    boolean cumulativePrpsParticipates = false;
                    if (evalPrps != null) {
                    	try {
                    		this.createEvalRunnerIfNecessary(state);
                    		Object evalResult = state.getEvalRunner().eval(state, evalPrps);
                    		if (evalResult == null) {
                    			cumulativePrpsParticipates = false;
                    		} else {                    			
                    			cumulativePrpsParticipates = (boolean) evalResult;
                    		}
						} catch (Throwable e) {
							throw new QueryTemplateException("Error evaluating expression: '" + evalPrps + "'", e);
						}
                    } else {
                        boolean anyInsteadOfAll = false;
                        if (filterPrps.length == 0) {
                            throw new QueryTemplateException("'" + QueryTemplateTokenPojo.TokenType.PARAMETER
                                    + "' not defined. Before: '" + this.queryTextTokens.get(index.getValue()).getValue() + "'");
                        }
                    	Pattern rxReservedAnyReplacer = Pattern.compile(this.config.getReservedAnyProperty());
                    	Pattern rxReservedAnyMatcher = Pattern.compile(" *" + this.config.getReservedAnyProperty() + ".*");
                    	if (rxReservedAnyMatcher.matcher(filterPrps[0]).matches()) {
                    		anyInsteadOfAll = true;
                    		filterPrps[0] = rxReservedAnyReplacer.matcher(filterPrps[0]).replaceAll("").trim();
                    		
                    		//criterionMappers can not be populated here because the property names are not known when eval is used. The property names will be known when eval is executed.
                    	}
                    	
                    	cumulativePrpsParticipates = !anyInsteadOfAll;

                    	for (String filterPrp : filterPrps) {
                    		String filterPrpTrim = filterPrp.trim();
                    		boolean negateVerification = false;
                    		if (filterPrpTrim.startsWith("!")) {
                    			negateVerification = true;
                    			filterPrpTrim = filterPrpTrim.replace("!", "").trim();
                    		}
                    		
                    		// Whether the property is participating in the query.
                    		PropertyMapper<Q, ?> mapper = null;
                    		if (this.mappersByPropertyNameMap.containsKey(filterPrpTrim)) {
                    			mapper = this.mappersByPropertyNameMap.get(filterPrpTrim);
                    		}
                    		
                    		if (mapper != null) {
                    			criterionMappers.add(mapper);
                    			boolean itemPrpParticipates = this.mappersByPropertyNameMap.get(filterPrpTrim)
                    					.getParticipatesInQuery().isParticipating(filter, filterPrpTrim);
                    			// Negating the verification because of the presence of !.
                    			if (negateVerification) {
                    				itemPrpParticipates = !itemPrpParticipates;
                    			}
                    			
                    			// Transforming the verification from conjunctive (AND's) to
                    			// disjunctive (OR's).
                    			if (anyInsteadOfAll) {
                    				cumulativePrpsParticipates = cumulativePrpsParticipates || itemPrpParticipates;
                    			} else {
                    				cumulativePrpsParticipates = cumulativePrpsParticipates && itemPrpParticipates;
                    			}
                    		} else {
                    			throw new QueryTemplateException("unmapped property listed: " + filterPrpTrim);
                    		}
                    	}
                    }

                    String criterionPrp = "";
                    if (cumulativePrpsParticipates) {
                        if (type == QueryTemplateTokenPojo.TokenType.CRITERION) {
                        	// #region criterionMappersByParameterUsage
                        	Set<ParameterMapper<Q, ?>> criterionParametersUsagedSet = new LinkedHashSet<>();
                			String parameterUsagePatternStr = this.config.getParameterUsagePrefix() + this.config.getParameterNamePattern();
                			Pattern parameterUsagePattern = Pattern.compile(parameterUsagePatternStr);
                			Matcher matcher = parameterUsagePattern.matcher(this.queryTextTokens.get(index.getValue()).getValue());
                			int matcherIndex = 0;
                			while (true) {
                				if (!matcher.find(matcherIndex)) {
                					break;
                				}
                				matcherIndex = matcher.end();
                				String parameterName = matcher.group(1);
                				PropertyMapper<Q, ?> mapperItem = this.mappersByParamNameMap.get(parameterName);
                				if (mapperItem != null) {
                					ParameterMapper<Q, ?> parameterMapper = mapperItem.getParameterMappers().get(parameterName);
                					criterionParametersUsagedSet.add(parameterMapper);
                				}
                			}
                        	// #endregion
                        	if (evalPrps != null) {
                        		criterionMappers.clear();
                        		for (ParameterMapper<Q, ?> parameterMapper : criterionParametersUsagedSet) {
									criterionMappers.add(parameterMapper.getOwner());
								}
                        	}
                            if (repeatTokenActive) {
                                criterionPrp = this.repeatArrayParamCriterion(filterPrpsStr, filter, criterionMappers,
                                        repeatConnector, this.queryTextTokens.get(index.getValue()).getValue(), state);
                            } else {          
                            	for (ParameterMapper<Q, ?> parameterMapper : criterionParametersUsagedSet) {
                            		if (parameterMapper.isRepeater() && parameterMapper.getOwner().getParticipatesInQuery().isParticipating(filter, parameterMapper.getOwner().getFilterPrp())) {
                            			throw new QueryTemplateException("The parameter '" + parameterMapper.getParameterName()
                            					+ "' is participating in the query, and is repeater, so it must be used with the "
                            					+ this.config.getRepeatToken().pattern().replace("\\", "") + " token. "
                            					+ this.tokenExceptionMessage(index.getValue(), this.queryTextTokens,
                            							"Disalowed repeater parameter usage"));
                            		}
                            	}
                            	
                    			criterionPrp = this.unpackArrayParamCriterionIfNecessary(filter, criterionMappers,
                    					this.queryTextTokens.get(index.getValue()).getValue(), state);
                            }
                        } else if (type == QueryTemplateTokenPojo.TokenType.QUERY_HELPER) {
                            if (repeatTokenActive) {
                                throw new QueryTemplateException(String.format(
                                        "There is a token '%3$s' before the query helper. Type='%1$s'; Value :'%2$s'.",
                                        this.queryTextTokens.get(index.getValue()).getType(),
                                        this.queryTextTokens.get(index.getValue()).getValue(),
                                        this.config.getRepeatToken().toString()));
                            }
                            criterionPrp = this.buildQueryHelper(filter, this.queryTextTokens.get(index.getValue()));
                        } else {
                            throw new QueryTemplateException(
                                    this.tokenExceptionMessage(index.getValue(), this.queryTextTokens, "UNEXPECTED TOKEN"));
                        }

                        queryTextMod.append(currentConnector).append(criterionPrp).append(" ");
                        paramParticipates.setValue(true);
                        currentConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }

                    break;
                } else {
                    throw new QueryTemplateException(
                            this.tokenExceptionMessage(index.getValue(), this.queryTextTokens, "UNEXPECTED TOKEN"));
                }
            } else {
                int errorIndex = Math.min(this.queryTextTokens.size() - 1, index.getValue());
                throw new QueryTemplateException("Expected token: " + QueryTemplateTokenPojo.TokenType.CRITERION + ". \n"
                        + String.format("Current token. Type='%1$s'; Value :'%2$s'. \nError: %3$s\n",
                                this.queryTextTokens.get(errorIndex).getType().toString(),
                                this.queryTextTokens.get(errorIndex).getValue(),
                                "\n" + this.showErrorPosition(this.queryTextEscapedSubs,
                                        this.queryTextTokens.get(errorIndex).getIndexInString())));
            }

            firstIteration = false;
            //index[0]++;
            index.setValue(index.getValue() + 1);
        }

        return queryTextMod.toString();
    }

    /**
     * Handles the part inside a parenthesis. If no inner part is included, the
     * parentheses are not included either.
     */
    private String buildParenthesisCriterion(OutputParam<Integer> index, String currentConnector,
            OutputParam<Boolean> anyParamParenthesis, Object filter, 
            QueryTemplateStateInternal<Q> state) {
        StringBuilder queryTextMod = new StringBuilder();
        anyParamParenthesis.setValue(false);
        String innerParenthesisConnector = "  ";

        while (true) {
            // The increment must be here at the beginning, otherwise nested parentheses
            // break: closing the inner parenthesis closes all of its parents.
            //index[0]++;
        	index.setValue(index.getValue() + 1);

        	queryTextMod.append(this.queryTextParts[index.getValue()]);

            if (index.getValue() < this.queryTextTokens.size() && this.queryTextTokens.get(index.getValue()) != null) {
                QueryTemplateTokenPojo.TokenType type = this.queryTextTokens.get(index.getValue()).getType();
                if (type == QueryTemplateTokenPojo.TokenType.AND) {
                    if (anyParamParenthesis.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.OR) {
                    if (anyParamParenthesis.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.NO_OPERATOR) {
                    innerParenthesisConnector = "  ";
                } else if (type == QueryTemplateTokenPojo.TokenType.OPEN_PARENTHESIS) {
                    //boolean[] anyParamSubParenthesis = { false };
                	OutputParam<Boolean> anyParamSubParenthesis = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParenthesisCriterion(index, innerParenthesisConnector,
                            anyParamSubParenthesis, filter, state));
                    anyParamParenthesis.setValue(anyParamParenthesis.getValue() || anyParamSubParenthesis.getValue());
                    if (anyParamParenthesis.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.PARAMETER) {
                    //boolean[] paramParticipates = { false };
                	OutputParam<Boolean> paramParticipates = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParameterCriterion(index, innerParenthesisConnector,
                            paramParticipates, anyParamParenthesis.getValue(), filter, state));
                    anyParamParenthesis.setValue(anyParamParenthesis.getValue() || paramParticipates.getValue());
                    if (anyParamParenthesis.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.CLOSE_PARENTHESIS) {
                    if (anyParamParenthesis.getValue()) {
                        queryTextMod = new StringBuilder(currentConnector + " "+this.config.getTargetReservedWordOpenParenthesis()+" " + queryTextMod + " "+this.config.getTargetReservedWordCloseParenthesis()+" ");
                    }
                    break;
                } else {
                    throw new QueryTemplateException(
                            this.tokenExceptionMessage(index.getValue(), this.queryTextTokens, "UNEXPECTED TOKEN"));
                }
            } else {
                throw new QueryTemplateException(
                        "Expected token: " + QueryTemplateTokenPojo.TokenType.CLOSE_PARENTHESIS);
            }
        }

        return queryTextMod.toString();
    }

    private String buildQueryHelper(Object filter, QueryTemplateTokenPojo queryHelperTokenTo) {
        String queryTextHelper;

        String queryHelperKey = queryHelperTokenTo.getValue().split(":")[1].trim();
        if (this.queryHelpers.containsKey(queryHelperKey)) {
            QueryTemplate<Q> queryHelper = this.queryHelpers.get(queryHelperKey);
            QueryTemplateState<Q> helperState = queryHelper.buildQueryState(filter);
            queryTextHelper = " " + helperState.getQueryString() + " ";
        } else {
            throw new QueryTemplateException(
                    "Query helper not loaded, use 'addQueryHelper'. Key: '" + queryHelperKey + "'");
        }

        return queryTextHelper;
    }

    /**
     * Splits into an array of tokens. The parameter and criterion tokens are placed
     * in the array with the delimiters removed.
     */
    private List<QueryTemplateTokenPojo> buildTokens(String queryText, int length, Pattern pt,
            Set<PropertyMapper<Q, ?>> usableMappers) {
        Matcher mt = pt.matcher(queryText);

        List<QueryTemplateTokenPojo> tokens = new ArrayList<>(length);

        while (mt.find()) {
            int start = mt.start();
            String tokenStr = mt.group();
            if (this.config.getFiltersToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.FILTERS, tokenStr, start));
            } else if (this.config.getWhereToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.WHERE, tokenStr, start));
            } else if (this.config.getAndToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.AND, tokenStr, start));
            } else if (this.config.getOrToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.OR, tokenStr, start));
            } else if (this.config.getNoOperatorToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.NO_OPERATOR, tokenStr, start));
            } else if (this.config.getRepeatToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.REPEAT, tokenStr, start));
            } else if (this.config.getOpenParenthesisToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.OPEN_PARENTHESIS, tokenStr, start));
            } else if (this.config.getCloseParenthesisToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.CLOSE_PARENTHESIS, tokenStr, start));
            } else if (this.config.getExtraToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.EXTRA, tokenStr, start));
            } else if (this.config.getQueryHelperToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.QUERY_HELPER,
                        this.config.getPropertiesDelimiterToken().matcher(tokenStr).replaceAll(""), start));
            } else if (this.config.getPropertiesToken().matcher(tokenStr).find()) {
                QueryTemplateTokenPojo token = new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.PARAMETER,
                        this.config.getPropertiesDelimiterToken().matcher(tokenStr).replaceAll(""), start);
                tokens.add(token);
                Pattern rxReservedEvalMatcher = Pattern.compile("(?s)\\s*" + this.config.getReservedEvalProperty() + ".*");
                if (rxReservedEvalMatcher.matcher(token.getValue()).matches()) {
                	//nothing?!
                } else {
                	// Storing the parameter in the list of possible parameters.
                	String[] filterPrps = token.getValue().split(",", -1);
                	for (String filterPrp : filterPrps) {
                		String cleanProperty = filterPrp;
                		Pattern rxReservedAnyReplacer = Pattern.compile(this.config.getReservedAnyProperty());
                		cleanProperty = cleanProperty.replace("!", "");
                		cleanProperty = rxReservedAnyReplacer.matcher(cleanProperty).replaceAll("");
                		cleanProperty = cleanProperty.trim();
                		
                		PropertyMapper<Q, ?> mapper = this.mappersByPropertyNameMap.get(cleanProperty);
                		if (mapper == null) {
                			throw new QueryTemplateException("Unmapped property listed: " + cleanProperty);
                		}
                		usableMappers.add(mapper);
                	}                	
                }
            } else if (this.config.getCriterionToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.CRITERION,
                        this.config.getCriterionDelimiterToken().matcher(tokenStr).replaceAll(""), start));
            } else {
                throw new QueryTemplateException("Unexpected token: '" + tokenStr + "'");
            }
        }
        
        // #region Finding usable mappers by parameter usage in the query text
		String parameterUsagePatternStr = this.config.getParameterUsagePrefix() + this.config.getParameterNamePattern();
		Pattern parameterUsagePatternPattern = Pattern.compile(parameterUsagePatternStr);
		
		Matcher matcher = parameterUsagePatternPattern.matcher(queryText);
		int matcherIndex = 0;
		
		while (true) {
			if (!matcher.find(matcherIndex)) {
				break;
			}
			matcherIndex = matcher.end();
			String parameterUsage = matcher.group(1);
			PropertyMapper<Q, ?> mapper = this.mappersByParamNameMap.get(parameterUsage);
			
			usableMappers.add(mapper);
		}
        // #endregion

        return tokens;
    }

    /**
     * Returns the usable parameters of the query, i.e. the parameters that appear in
     * it.
     *
     * @return the usable parameters.
     */
    @Override
	public Set<PropertyMapper<Q, ?>> getUsableMappers() {
        return this.usableMappers;
    }

    /**
     * Replaces the escaped characters with a temporary value, enabling characters
     * that would otherwise affect the 'substitution grammar'.
     *
     * @param text      the text.
     * @param escapeMap map relating 'string that replaced the character' and the
     *                  replaced character.
     * @return the replaced text.
     */
    private String replaceEscapes(String text, Map<String, String> escapeMap) {
        StringBuilder replacedText = new StringBuilder();
        Pattern rxSub = Pattern.compile(this.config.getEscapeCharacter() + "(.)");
        if (this.config.getEscapeCharacter() != null && !this.config.getEscapeCharacter().isEmpty()) {
            Matcher mts = rxSub.matcher(text);

            int lastIndex = 0;
            int i = 0;
            while (mts.find()) {
                String escapedChar = mts.group(1);
                String replacement = TEMPORARY_ESCAPE_PREFIX + String.format("%05d", i);
                replacedText.append(text, lastIndex, mts.start()).append(replacement);
                lastIndex = mts.end();

                escapeMap.put(replacement, escapedChar);
                i++;
            }

            if (LOG.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{ ");
                for (Map.Entry<String, String> entry : escapeMap.entrySet()) {
                    sb.append("'").append(entry.getKey()).append("'='").append(entry.getValue()).append("'; ");
                }
                sb.append("}");
                LOG.debug("Escape mapping: " + sb.toString());
            }

            replacedText.append(text, lastIndex, text.length());
            if (LOG.isDebugEnabled()) {
                LOG.debug("replacedText: '" + replacedText + "'");
            }
        }
        return replacedText.toString();
    }

    /**
     * @see #restoreEscapes(String, Map)
     */
    private void restoreEscapes(String[] texts, Map<String, String> escapeMap) {
        if (this.config.getEscapeCharacter() != null && !this.config.getEscapeCharacter().isEmpty()) {
            for (int i = 0; i < texts.length; i++) {
                texts[i] = this.restoreEscapes(texts[i], escapeMap);
            }
        }
    }

    /**
     * @see #restoreEscapes(String, Map)
     */
    private void restoreEscapesTokens(List<QueryTemplateTokenPojo> tokens, Map<String, String> escapeMap) {
        if (this.config.getEscapeCharacter() != null && !this.config.getEscapeCharacter().isEmpty()) {
            for (QueryTemplateTokenPojo token : tokens) {
                token.setValue(this.restoreEscapes(token.getValue(), escapeMap));
            }
        }
    }

    /**
     * Inverts the action of {@link #replaceEscapes(String, Map)}.
     *
     * @param text      the text.
     * @param escapeMap map relating 'string that replaced the character' and the
     *                  replaced character.
     * @return the restored text.
     */
    private String restoreEscapes(String text, Map<String, String> escapeMap) {
        String restoredText = text;
        if (this.config.getEscapeCharacter() != null && !this.config.getEscapeCharacter().isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("restoredText BEFORE: '" + restoredText + "'");
            }

            for (Map.Entry<String, String> entry : escapeMap.entrySet()) {
                if (restoredText != null) {
                    restoredText = restoredText.replace(entry.getKey(), entry.getValue());
                } else {
                    break;
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("restoredText AFTER: '" + restoredText + "'");
            }
        }

        return restoredText;
    }
}
