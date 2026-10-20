package io.github.querytemplate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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

class QueryTemplateDefault<Q, F> implements QueryTemplateInternal<Q, F> {

	/** Used to temporarily replace the character. */
    private static String TEMPORARY_ESCAPE_PREFIX = "_PREF_TEMP_ESC_";
	
    private static final Logger LOG = LoggerFactory.getLogger(QueryTemplateDefault.class);

//    /** Possible parameters in the query. */
//    private Set<PropertyMapper<Q, F, ?>> usableMappers = null;

    //private boolean compactQueryText = false;

    /** Regular expression that splits the query string into the parts to be assembled. */
    private String splitterIntoPartsPattern = "";

    /** Fixed parts of the query. */
    private String[] queryTextParts;
    /** Variable parts of the query. */
    private List<QueryTemplateTokenPojo> queryTextTokens;

    /** QueryTemplate used as Helpers to assemble inner parts of the query. */
    private Map<String, QueryTemplateInternal<Q, F>> queryHelpers = new LinkedHashMap<>();

    /** Query after compaction and substitution. */
    private String queryTextEscapedSubs = "";
    
    //private List<PropertyMapper<Q, F, ?>> mappers = new ArrayList<>();
    //private Map<String, PropertyMapper<Q, F, ?>> mappersMap;
    private Map<String, PropertyMapper<Q, F, ?>> mappersByParamNameMap;
    private Map<String, PropertyMapper<Q, F, ?>> mappersByPropertyNameMap;
    
    //private String sqlHqlQueryOriginal;
    
    private QueryTemplateConfig<Q, F> config;
    
    private QueryTemplateInternal<Q, F> parent;
    
    /**
     * TODO: Make a QueryTemplateBuilder Using the Builder Pattern to make it easier to build the object with all the parameters sharing then with QueryTemplateConfig.
     * Builds the object passing all the possible parameters.
     */
    QueryTemplateDefault(QueryTemplateConfig<Q, F> config) {
		this.config = config;
        this.setUp();
    }
    
    QueryTemplateDefault(QueryTemplateConfig<Q, F> config, QueryTemplateInternal<Q, F> parent) {
		this.config = config;
		this.parent = parent;
        this.setUp();
    }
    
    @Override
	public void setUp() {
		if (LOG.isTraceEnabled()) {
			LOG.trace("setUp: Setting up QueryTemplateDefault with config: " + this.config);
		}
    	
    	String queryText = this.config.getQueryTextOriginal();
    	
		this.mappersByParamNameMap = new LinkedHashMap<>();
		this.mappersByPropertyNameMap = new LinkedHashMap<>();
    	for (String propertyNameItem : this.config.getMappersConfig().keySet()) {
    		PropertyMapperConfig<Q, F, Object, Object> mapperConfig = (PropertyMapperConfig<Q, F, Object, Object>) config.getMappersConfig().get(propertyNameItem);
    		PropertyMapper<Q, F, Object> mapper = 
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
    				ParameterMapperConfig<Q, F, Object, Object> parameterMapperConfig = (ParameterMapperConfig<Q, F, Object, Object>) mapperConfig.getParameterMappers().get(parameterName);
    				mapper.addParameter(parameterName)
    					.repeater(parameterMapperConfig.isRepeater())
    					.unpackListItems(parameterMapperConfig.isUnpackListItems())
    					.onParticipatesNamed(parameterMapperConfig.getOnParticipatesNamed())
    					.onParticipatesPositional(parameterMapperConfig.getOnParticipatesPositional());
    			}
    		}
			for (String parameterName : mapper.getParameterMappers().keySet()) {
				ParameterMapper<Q, F, Object> parameterMapper = mapper.getParameterMappers().get(parameterName);
	    		this.mappersByParamNameMap.put(parameterMapper.getParameterName(), mapper);
			}
    		this.mappersByPropertyNameMap.put(mapper.getFilterPrp(), mapper);
    	}
    	
        if (this.config.isCompactQueryText()) {
            queryText = this.compactInputQueryText(this.config.getQueryTextOriginal());
        }
		if (this.config.isRemoveComments()) {
			queryText = this.removeComments(queryText);
		}

//        this.usableMappers = new LinkedHashSet<>();

        this.splitterIntoPartsPattern = "(?s)" + this.config.getFiltersToken() + "|" + this.config.getWhereToken() + "|" + this.config.getAndToken() + "|" + this.config.getOrToken()
                + "|" + this.config.getNoOperatorToken() + "|" + this.config.getRepeatToken() + "|" + this.config.getOpenParenthesisToken() + "|"
                + this.config.getCloseParenthesisToken() + "|" + this.config.getExtraToken() + "|" + this.config.getPropertiesToken() + "|" + this.config.getCriterionToken();
        Pattern pt = Pattern.compile(this.splitterIntoPartsPattern);

        Map<String, String> escapeMap = new LinkedHashMap<>();
        this.queryTextEscapedSubs = this.replaceEscapes(queryText, escapeMap);

        this.queryTextParts = pt.split(this.queryTextEscapedSubs, -1);
        
		if (LOG.isDebugEnabled()) {
			LOG.debug("setUp: Splitting query text into parts with pattern: " + this.splitterIntoPartsPattern);
			LOG.debug("setUp: Resulting parts: " + this.queryTextParts.length + " parts.");
		}
        
        this.queryTextTokens = this.buildTokens(this.queryTextEscapedSubs, this.queryTextParts.length, pt
                /*this.usableMappers*/);

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
            LOG.trace("setUp: " + logStr.toString());
        }
    }
    
    /**
     * Sets up the query helpers, which are other QueryTemplate instances that will be used to assemble inner parts of the query.
     * Implicitly recursive, since the QueryTemplate constructor calls setUp(), which calls setUpQueryHelpers() again for the inner query helpers.
     */
    private void setUpQueryHelpers() {
    	for (String queryHelperKey : this.config.getQueryHelpers().keySet()) {
    		QueryTemplateConfig<Q, F> queryHelperConfig = this.config.getQueryHelpers().get(queryHelperKey);
    		// Implicitly recursive, since the QueryTemplate constructor calls setUp(), which calls setUpQueryHelpers() again for the inner query helpers.
    		QueryTemplateInternal<Q, F> queryTemplateHelper = (QueryTemplateInternal<Q, F>) QueryTemplateInternal.of(queryHelperConfig, this);
    		
    		//TODO: PRECISA DISSO?!
    		//this.usableMappers.addAll(queryTemplateHelper.getUsableMappers());
    		
    		this.queryHelpers.put(
    				queryHelperKey,
    				queryTemplateHelper
			);
    	}
    }

    private String shrinkBlankLines(String queryText) {
    	 return queryText.replaceAll("(?m)^\\s*$\\n?", "");
    }
    
    private String removeComments(String queryText) {
    	String blockCommentPatternStr = "(?s)/\\*[\\s\\S]*?\\*/";
    	String commentPatternStr = "(?m)^\\s*--.*$";
    	
    	String compacted = queryText;

        int length = compacted.length();
        do {
            compacted = compacted
            		.replaceAll(commentPatternStr, "")
            		.replaceAll(blockCommentPatternStr, "");
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
//    public QueryTemplate(String sqlHqlQuery, List<PropertyMapper<Q, F, ?>> mappers) {
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
//    public QueryTemplate(String sqlHqlQuery, List<PropertyMapper<Q, F, ?>> mappers, boolean compactSqlHqlQuery) {
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
    void setParamQueryNonRecursive(QueryTemplateStateInternal<Q, F> state, Q query, Map<Integer, Action> positionalParameterActions) {
        for (String parameterName : this.mappersByParamNameMap.keySet()) {
        	PropertyMapper<Q, F, ?> mapperItem = this.mappersByParamNameMap.get(parameterName);
        	PropertyMapper<Q, F, Object> mapperItemCasted = (PropertyMapper<Q, F, Object>) mapperItem;
        	ParameterMapper<Q, F, Object> parameterMapper = mapperItemCasted.getParameterMappers().get(parameterName);
            if (LOG.isDebugEnabled()) {
            	LOG.debug("setParamQueryNonRecursive: Tests whether the parameter is used in the query and whether it is participating in the query, based on the filter object.");
//            	if (!this.usableMappers.contains(mapperItemCasted)) {
//            		LOG.debug("setParamQueryNonRecursive: Property '" + mapperItemCasted.getFilterPrp() + "'. The parameter "+this.config.getParameterUsagePrefix()+ parameterName + " is NOT present in query text. ");
//            	} else {
//            		LOG.debug("setParamQueryNonRecursive: Property '" + mapperItemCasted.getFilterPrp() + "'. The parameter "+this.config.getParameterUsagePrefix()+ parameterName + " is PRESENT in query text. ");
//            	}
				if (mapperItemCasted.getParticipatesInQuery().isParticipating(state.getFilter(),
						mapperItemCasted.getFilterPrp())) {
					LOG.debug("setParamQueryNonRecursive: Property '" + mapperItemCasted.getFilterPrp() + "' is PARTICIPATING in the query. `ParticipatesInQuery.isParticipating()` returned true.");
				} else {
					LOG.debug("setParamQueryNonRecursive: Property '" + mapperItemCasted.getFilterPrp() + "' is NOT participating in the query. `ParticipatesInQuery.isParticipating()` returned false.");
	            }
            }
        	
            // Tests whether the parameter is used in the query and whether it is participating in the query, based on the filter object.
			if ( /*
					 * this.usableMappers.contains(mapperItemCasted) &&
					 */ mapperItemCasted.getParticipatesInQuery().isParticipating(state.getFilter(), mapperItemCasted.getFilterPrp()) ) {
                try {
                    Object value = PropertyUtils.getProperty(state.getFilter(), mapperItemCasted.getFilterPrp());
					if (LOG.isDebugEnabled()) {
						LOG.debug("setParamQueryNonRecursive: Preparing parameter to be assigned to the query. Property '" + mapperItemCasted.getFilterPrp() + "'. Parameter name: " + parameterMapper.getParameterName());
					}
                    //if (mapperItemCasted.isUnfoldEnumerable()) {
                    if (parameterMapper.isUnpackListItems() || parameterMapper.isRepeater()) {
    					if (LOG.isDebugEnabled()) {
    						LOG.debug("setParamQueryNonRecursive: Parameter '" + parameterMapper.getParameterName() + "' is a repeater or must be unpacked. Unpacking/Repeating the value into individual items to be assigned to the query.");
    					}
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
    					if (LOG.isTraceEnabled()) {
    						LOG.trace("setParamQueryNonRecursive: Unpacked/Repeated value for parameter '" + parameterMapper.getParameterName() + "' into collection with " + valueColl.size() + " items. Items: " + valueColl);
    					}
                    	int repeatIndex = 0;

                    	// #region foo
                		if (!this.config.isConvertNamedToPositionalParameters()) {
                			if (state.getQueryString().contains(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName())
                					&& (mapperItemCasted.getOnParticipatesNamed() != null
                						|| parameterMapper.getOnParticipatesNamed() != null
            						)
            					) {
                				for (Object valueItem : valueColl) {
                        			AssignedParameterInfoInternal<Object> assignedParameterInfo = new AssignedParameterInfoDefault<>();
                        			assignedParameterInfo.setPosition(null);
                        			assignedParameterInfo.setIndex(repeatIndex);
                        			assignedParameterInfo.setName(parameterMapper.getParameterName());
                        			assignedParameterInfo.setUnpackedRepeatedName(parameterMapper.getParameterName() + "_" + repeatIndex);
                        			assignedParameterInfo.setValue(valueItem);
                        			
                        			if (mapperItemCasted.getOnParticipatesNamed() != null) {
                        				if (LOG.isDebugEnabled()) {
                        					LOG.debug("setParamQueryNonRecursive: Mapped property '" + mapperItemCasted.getFilterPrp() + "' has a `AssignNamedParameter<Q, P>` callback. Assigning the parameter value to the query.");
                        				}
                        				if (LOG.isTraceEnabled()) {
                        					LOG.trace("setParamQueryNonRecursive: Assigning, just now, named parameter: " + assignedParameterInfo);
                        				}
                        				mapperItemCasted.getOnParticipatesNamed().accept(
                        						query, 
                        						assignedParameterInfo.getUnpackedRepeatedName(), 
                        						valueItem, 
                        						(AssignedParameterInfo<Object>) assignedParameterInfo
                						);
                        			}
									if (parameterMapper.getOnParticipatesNamed() != null) {
										if (LOG.isDebugEnabled()) {
                        					LOG.debug("setParamQueryNonRecursive: Mapped property/parameter '" + mapperItemCasted.getFilterPrp() + "/" + parameterMapper.getParameterName() + "' has a `AssignNamedParameter<Q, P>` callback. Assigning the parameter value to the query.");
                        				}
                        				if (LOG.isTraceEnabled()) {
                        					LOG.trace("setParamQueryNonRecursive: Assigning, just now, named parameter: " + assignedParameterInfo);
                        				}
										parameterMapper.getOnParticipatesNamed().accept(
												query,
												assignedParameterInfo.getUnpackedRepeatedName(), valueItem,
												(AssignedParameterInfo<Object>) assignedParameterInfo
										);
									}
                					
                					repeatIndex++;
                				}
	                		}
                		} else {
            				Map<Integer, List<AssignedParameterInfo<?>>> assignedParameterInfosByIndex = state
            						.getPropertyMapperItemIndexToAssignedParameterInfo().get(mapperItemCasted);
                			if (assignedParameterInfosByIndex.size() > 0
                					&& (mapperItemCasted.getOnParticipatesPositional() != null
                							|| parameterMapper.getOnParticipatesPositional() != null
                					)
                				) {
                				
                				for (Object valueItem : valueColl) {
                					List<AssignedParameterInfo<?>> assignedParameterInfos = assignedParameterInfosByIndex.get(repeatIndex);
                					for (AssignedParameterInfo<?> assignedParameterInfo : assignedParameterInfos) {
                						AssignedParameterInfoInternal<Object> assignedParameterInfoCasted = (AssignedParameterInfoInternal<Object>) assignedParameterInfo;
                						assignedParameterInfoCasted.setValue(valueItem);
                						// delaying the execution of the parameter setting to avoid issues with the order of execution and potential side effects
                						if (mapperItemCasted.getOnParticipatesPositional() != null) {
                							if (LOG.isDebugEnabled()) {
                								LOG.debug("setParamQueryNonRecursive: Mapped property '" + mapperItemCasted.getFilterPrp() + "' has a `AssignPositionalParameter<Q, P>` callback. Cataloging positional parameter value for future and ordered assignment to the query.");
                							}
                							if (LOG.isTraceEnabled()) {
                								LOG.trace("setParamQueryNonRecursive: Cataloging positional parameter for future and ordered assignment: " + assignedParameterInfo);
                							}
                							positionalParameterActions.put(
                									assignedParameterInfo.getPosition(),
                									() -> mapperItemCasted.getOnParticipatesPositional()
                									.accept(query,
                											assignedParameterInfo.getPosition(),
                											valueItem,
                											(AssignedParameterInfo<Object>) assignedParameterInfo));
                						}
                						if (parameterMapper.getOnParticipatesPositional() != null) {
                							if (LOG.isDebugEnabled()) {
                								LOG.debug("setParamQueryNonRecursive: Mapped property/parameter '" + mapperItemCasted.getFilterPrp() + "/" + parameterMapper.getParameterName() + "' has a `AssignPositionalParameter<Q, P>` callback. Cataloging positional parameter value for future and ordered assignment to the query.");
                							}
                							if (LOG.isTraceEnabled()) {
                								LOG.trace("setParamQueryNonRecursive: Cataloging positional parameter for future and ordered assignment: " + assignedParameterInfo);
                							}
                							positionalParameterActions.put(
                									assignedParameterInfo.getPosition(),
                									() -> parameterMapper.getOnParticipatesPositional()
                									.accept(query,
                											assignedParameterInfo.getPosition(), 
                											valueItem,
                											(AssignedParameterInfo<Object>) assignedParameterInfo));
                						}
                					}

                					repeatIndex++;
                				}
                			}
                		}
                    	// #endregion
                    	
                		// #region foo2
//                		for (Object valueItem : valueColl) {
//                			if (!this.config.isConvertNamedToPositionalParameters()) {
//                				if (state.getQueryString().contains(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName())
//                						&& (mapperItemCasted.getOnParticipatesNamed() != null
//                						|| parameterMapper.getOnParticipatesNamed() != null
//                								)
//                						) {
//                					AssignedParameterInfoInternal<Object> assignedParameterInfo = new AssignedParameterInfoDefault<>();
//                					assignedParameterInfo.setPosition(null);
//                					assignedParameterInfo.setIndex(repeatIndex);
//                					assignedParameterInfo.setName(parameterMapper.getParameterName());
//                					assignedParameterInfo.setUnpackedRepeatedName(parameterMapper.getParameterName() + "_" + repeatIndex);
//                					assignedParameterInfo.setValue(valueItem);
//                					
//                					if (mapperItemCasted.getOnParticipatesNamed() != null) {
//                						if (LOG.isDebugEnabled()) {
//                							LOG.debug("setParamQueryNonRecursive: Mapped property '" + mapperItemCasted.getFilterPrp() + "' has a `AssignNamedParameter<Q, P>` callback. Assigning the parameter value to the query.");
//                						}
//                						if (LOG.isTraceEnabled()) {
//                							LOG.trace("setParamQueryNonRecursive: Assigning, just now, named parameter: " + assignedParameterInfo);
//                						}
//                						mapperItemCasted.getOnParticipatesNamed().accept(
//                								query, 
//                								assignedParameterInfo.getUnpackedRepeatedName(), 
//                								valueItem, 
//                								(AssignedParameterInfo<Object>) assignedParameterInfo
//                								);
//                					}
//                					if (parameterMapper.getOnParticipatesNamed() != null) {
//                						if (LOG.isDebugEnabled()) {
//                							LOG.debug("setParamQueryNonRecursive: Mapped property/parameter '" + mapperItemCasted.getFilterPrp() + "/" + parameterMapper.getParameterName() + "' has a `AssignNamedParameter<Q, P>` callback. Assigning the parameter value to the query.");
//                						}
//                						if (LOG.isTraceEnabled()) {
//                							LOG.trace("setParamQueryNonRecursive: Assigning, just now, named parameter: " + assignedParameterInfo);
//                						}
//                						parameterMapper.getOnParticipatesNamed().accept(
//                								query,
//                								assignedParameterInfo.getUnpackedRepeatedName(), valueItem,
//                								(AssignedParameterInfo<Object>) assignedParameterInfo
//                								);
//                					}
//                				}
//                			} else {
//                				Map<Integer, List<AssignedParameterInfo<?>>> assignedParameterInfosByIndex = state
//                						.getPropertyMapperItemIndexToAssignedParameterInfo().get(mapperItemCasted);
//                				if (assignedParameterInfosByIndex.size() > 0
//                						&& (mapperItemCasted.getOnParticipatesPositional() != null
//                						|| parameterMapper.getOnParticipatesPositional() != null
//                								)
//                						) {
//                					List<AssignedParameterInfo<?>> assignedParameterInfos = assignedParameterInfosByIndex.get(repeatIndex);
//                					for (AssignedParameterInfo<?> assignedParameterInfo : assignedParameterInfos) {
//                						AssignedParameterInfoInternal<Object> assignedParameterInfoCasted = (AssignedParameterInfoInternal<Object>) assignedParameterInfo;
//                						assignedParameterInfoCasted.setValue(valueItem);
//                						// delaying the execution of the parameter setting to avoid issues with the order of execution and potential side effects
//                						if (mapperItemCasted.getOnParticipatesPositional() != null) {
//                							if (LOG.isDebugEnabled()) {
//                								LOG.debug("setParamQueryNonRecursive: Mapped property '" + mapperItemCasted.getFilterPrp() + "' has a `AssignPositionalParameter<Q, P>` callback. Cataloging positional parameter value for future and ordered assignment to the query.");
//                							}
//                							if (LOG.isTraceEnabled()) {
//                								LOG.trace("setParamQueryNonRecursive: Cataloging positional parameter for future and ordered assignment: " + assignedParameterInfo);
//                							}
//                							positionalParameterActions.put(
//                									assignedParameterInfo.getPosition(),
//                									() -> mapperItemCasted.getOnParticipatesPositional()
//                									.accept(query,
//                											assignedParameterInfo.getPosition(),
//                											valueItem,
//                											(AssignedParameterInfo<Object>) assignedParameterInfo));
//                						}
//                						if (parameterMapper.getOnParticipatesPositional() != null) {
//                							if (LOG.isDebugEnabled()) {
//                								LOG.debug("setParamQueryNonRecursive: Mapped property/parameter '" + mapperItemCasted.getFilterPrp() + "/" + parameterMapper.getParameterName() + "' has a `AssignPositionalParameter<Q, P>` callback. Cataloging positional parameter value for future and ordered assignment to the query.");
//                							}
//                							if (LOG.isTraceEnabled()) {
//                								LOG.trace("setParamQueryNonRecursive: Cataloging positional parameter for future and ordered assignment: " + assignedParameterInfo);
//                							}
//                							positionalParameterActions.put(
//                									assignedParameterInfo.getPosition(),
//                									() -> parameterMapper.getOnParticipatesPositional()
//                									.accept(query,
//                											assignedParameterInfo.getPosition(), 
//                											valueItem,
//                											(AssignedParameterInfo<Object>) assignedParameterInfo));
//                						}
//                					}
//                				}
//                			}
//                			repeatIndex++;
//                		}
                		// #endregion
                		
                    } else if (value instanceof FragmentInclusion) {
                    	// nothing
                    } else {
                    	if (!this.config.isConvertNamedToPositionalParameters()) {
                    		if (state.getQueryString().contains(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName())
                    				&& (mapperItemCasted.getOnParticipatesNamed() != null
                    					|| parameterMapper.getOnParticipatesNamed() != null
                    				)
                    			) {
                    			AssignedParameterInfoInternal<Object> assignedParameterInfo = new AssignedParameterInfoDefault<>();
                    			assignedParameterInfo.setPosition(null);
                    			assignedParameterInfo.setIndex(null);
                    			assignedParameterInfo.setName(parameterMapper.getParameterName());
                    			assignedParameterInfo.setUnpackedRepeatedName(null);
                    			assignedParameterInfo.setValue(value);
								if (mapperItemCasted.getOnParticipatesNamed() != null) {
                    				if (LOG.isDebugEnabled()) {
                    					LOG.debug("setParamQueryNonRecursive: Mapped property '" + mapperItemCasted.getFilterPrp() + "' has a `AssignNamedParameter<Q, P>` callback. Assigning the parameter value to the query.");
                    				}
                    				if (LOG.isTraceEnabled()) {
                    					LOG.trace("setParamQueryNonRecursive: Assigning, just now, named parameter: " + assignedParameterInfo);
                    				}
									mapperItemCasted.getOnParticipatesNamed()
										.accept(query, 
												parameterMapper.getParameterName(), 
												value, 
												(AssignedParameterInfo<Object>) assignedParameterInfo
									);
								}
								if (parameterMapper.getOnParticipatesNamed() != null) {
									if (LOG.isDebugEnabled()) {
                    					LOG.debug("setParamQueryNonRecursive: Mapped property/parameter '" + mapperItemCasted.getFilterPrp() + "/" + parameterMapper.getParameterName() + "' has a `AssignNamedParameter<Q, P>` callback. Assigning the parameter value to the query.");
                    				}
                    				if (LOG.isTraceEnabled()) {
                    					LOG.trace("setParamQueryNonRecursive: Assigning, just now, named parameter: " + assignedParameterInfo);
                    				}
									parameterMapper.getOnParticipatesNamed()
										.accept(query,
												parameterMapper.getParameterName(), 
												value,
												(AssignedParameterInfo<Object>) assignedParameterInfo
										);
								}
                    		}		
                    	} else {
                    		List<AssignedParameterInfo<?>> assignedParameterInfos = state.getPropertyMapperToAssignedParameterInfo().get(mapperItemCasted);
                    		if (assignedParameterInfos.size() > 0
                    				&& (mapperItemCasted.getOnParticipatesPositional() != null
                    					|| parameterMapper.getOnParticipatesPositional() != null
                					)
                				) {
                    			for (AssignedParameterInfo<?> assignedParameterInfo : assignedParameterInfos) {
                					AssignedParameterInfoInternal<Object> assignedParameterInfoCasted = (AssignedParameterInfoInternal<Object>) assignedParameterInfo;
									assignedParameterInfoCasted.setValue(value);
                    				// delaying the execution of the parameter setting to avoid issues with the order of execution and potential side effects
									if (mapperItemCasted.getOnParticipatesPositional() != null) {
                        				if (LOG.isDebugEnabled()) {
                        					LOG.debug("setParamQueryNonRecursive: Mapped property '" + mapperItemCasted.getFilterPrp() + "' has a `AssignPositionalParameter<Q, P>` callback. Cataloging positional parameter value for future and ordered assignment to the query.");
                        				}
                        				if (LOG.isTraceEnabled()) {
                        					LOG.trace("setParamQueryNonRecursive: Cataloging positional parameter for future and ordered assignment: " + assignedParameterInfo);
                        				}
	                    				positionalParameterActions.put(
	                    						assignedParameterInfo.getPosition(), 
	                    						() -> mapperItemCasted.getOnParticipatesPositional()
	                    							.accept(query, 
	                    									assignedParameterInfo.getPosition(), 
	                    									value, 
	                    									(AssignedParameterInfo<Object>) assignedParameterInfo
                									)
                        				);
									}
									if (parameterMapper.getOnParticipatesPositional() != null) {
                        				if (LOG.isDebugEnabled()) {
                        					LOG.debug("setParamQueryNonRecursive: Mapped property/parameter '" + mapperItemCasted.getFilterPrp() + "/" + parameterMapper.getParameterName() + "' has a `AssignPositionalParameter<Q, P>` callback. Cataloging positional parameter value for future and ordered assignment to the query.");
                        				}
                        				if (LOG.isTraceEnabled()) {
                        					LOG.trace("setParamQueryNonRecursive: Cataloging positional parameter for future and ordered assignment: " + assignedParameterInfo);
                        				}
										positionalParameterActions.put(
                                                assignedParameterInfo.getPosition(),
                                                () -> parameterMapper.getOnParticipatesPositional()
                                                    .accept(query,
                                                            assignedParameterInfo.getPosition(),
                                                            value,
                                                            (AssignedParameterInfo<Object>) assignedParameterInfo
                                                    )
                                        );
									}
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
    public void setParamQueryRecursive(QueryTemplateState<Q, F> state, Q query, Map<Integer, Action> positionalParameterActions) {
        // Setting the parameters of the inner queries.
        for (String queryHelperKey : this.queryHelpers.keySet()) {
            if (this.queryHelpers.containsKey(queryHelperKey)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Setting parameters for inner query helper with key: '" + queryHelperKey + "'");
				}
            	QueryTemplateInternal<Q, F> queryHelper = this.queryHelpers.get(queryHelperKey);
                queryHelper.setParamQueryRecursive(state, query, positionalParameterActions);
            } else {
                throw new QueryTemplateException(
                        "Query helper not loaded, use 'addQueryHelper'. Key: '" + queryHelperKey + "'");
            }
        }

        this.setParamQueryNonRecursive((QueryTemplateStateInternal<Q, F>) state, query, positionalParameterActions);
    }

    /**
     * Assigns values to the parameters based on the participation of the properties in the query, including inner queries.
     *
     * @param positionalParameterActions a map of parameter positions to actions that set the parameter values on the query. This is used to delay the execution of setting the parameters until all actions are collected, ensuring that they are executed in order of their target parameter positions.
     * @param state        the state of the query template.
     * @param query        the query to set the parameters on.
     */
    @Override
	public void setParamQuery(QueryTemplateState<Q, F> state, Q query,  Map<Integer, Action> positionalParameterActions) {
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
	public void setParamQuery(QueryTemplateState<Q, F> state, Q query) {
    	Map<Integer, Action> positionalParameterActions = new LinkedHashMap<>();
    	this.setParamQuery(state, query, positionalParameterActions);
    }

	private void processPositionalParameters(QueryTemplateStateInternal<Q, F> state) {
		for (String prpName : this.mappersByPropertyNameMap.keySet()) {
			PropertyMapper<Q, F, ?> mapperItem = this.mappersByPropertyNameMap.get(prpName);
			if (!state.getPropertyMapperToAssignedParameterInfo()
					.containsKey(this.mappersByPropertyNameMap.get(mapperItem))) {
				state.getPropertyMapperToAssignedParameterInfo().put(mapperItem, new ArrayList<>());
			}
			if (!state.getPropertyMapperItemIndexToAssignedParameterInfo().containsKey(mapperItem)) {
				state.getPropertyMapperItemIndexToAssignedParameterInfo().put(mapperItem, new LinkedHashMap<>());
			}
		}
		if (this.config.isConvertNamedToPositionalParameters()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Converting named parameters to positional parameters in the query string.");
			}
			String parameterWithIndexSufixPatternStr = this.config.getParameterUsagePrefix() + this.config.getParameterNamePattern();
			String indexSufixPatternStr = "^(.+)_(\\d+)$";
			Pattern parameterWithIndexSufixPattern = Pattern.compile(parameterWithIndexSufixPatternStr);
			Pattern indexSufixPattern = Pattern.compile(indexSufixPatternStr);
			
			Matcher matcher = parameterWithIndexSufixPattern.matcher(state.getQueryString());
			int parameterPosition = this.config.getParameterBasePosition();
			int matcherIndex = 0;
			
			StringBuilder positionalPametersQuerySB = new StringBuilder();
			while (true) {
				if (!matcher.find(matcherIndex)) {
					break;
				}
				positionalPametersQuerySB.append(state.getQueryString().substring(matcherIndex, matcher.start()));
				positionalPametersQuerySB.append(this.config.getTargetReservedWordPositionalParameterMarker());
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
				if (LOG.isDebugEnabled()) {
					LOG.debug("Processing parameter: " + parameterWithIndexSufix + ". Parameter name: " + parameterName
							+ ". Parameter index (unpacked/repeated) sufix: " + parameterIndexSufix);
				}
				if (!this.mappersByParamNameMap.containsKey(parameterName)) {
					throw new QueryTemplateException(
							"Parameter name '" + parameterName + "' is not present in the mappers.");
				}
				if (!parameterWithIndexSufix.equals(parameterName) && this.mappersByParamNameMap.containsKey(parameterWithIndexSufix) && this.mappersByParamNameMap.containsKey(parameterName)) {
					throw new QueryTemplateException("Parameter with index sufix '" + parameterWithIndexSufix + "' and parameter name '" + parameterName + "' are both present in the mappers map. This is not allowed.");
                }

				PropertyMapper<Q, F, ?> mapperItem = this.mappersByParamNameMap.get(parameterName);
				ParameterMapper<Q, F, ?> parameterMapper = mapperItem.getParameterMappers().get(parameterName);
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
					AssignedParameterInfoInternal<Object> assignedParameterInfo = new AssignedParameterInfoDefault<>();
					assignedParameterInfo.setPosition(parameterPosition);
					assignedParameterInfo.setName(parameterName);
					assignedParameterInfo.setUnpackedRepeatedName(parameterWithIndexSufix);
					assignedParameterInfo.setIndex(itemIndex);
					itemIndexToAssignedParameterInfo.get(itemIndex).add(assignedParameterInfo);
					parameterPosition++;
					if (LOG.isDebugEnabled()) {
						LOG.debug("Parameter '" + parameterWithIndexSufix
								+ "' is a repeater or must be unpacked. Added to the map of property mapper item index to assigned parameter info. Position: "
								+ parameterPosition + ". Index: " + itemIndex);
					}
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
					if (LOG.isDebugEnabled()) {
						LOG.debug("Parameter '" + parameterName
								+ "' is NOT a repeater or must be unpacked. Added to the map of property mapper to assigned parameter info. Position: "
								+ parameterPosition);
					}
				}
			}
			positionalPametersQuerySB.append(state.getQueryString().substring(matcherIndex));
			
	        String positionalPametersQueryStr = positionalPametersQuerySB.toString();
	        
	        if (this.config.isShrinkBlankLinesAfterBuild()) {
	        	positionalPametersQueryStr = this.shrinkBlankLines(positionalPametersQueryStr);
	        }
			
			state.setQueryString(positionalPametersQueryStr);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Final query string with positional parameters: " + positionalPametersQueryStr);
			}
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
//            F filter, Set<String> usableParameters) {
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
	public QueryTemplateState<Q, F> buildQueryState(F filter) {
    	QueryTemplateStateInternal<Q, F> state = new QueryTemplateStateDefault<Q, F>(this, null, filter, new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    	
        // Auxiliary variable to access the mapper by property name.
        Map<String, PropertyMapper> mappersByPrp = new LinkedHashMap<>();
        for (String filterPrp : this.mappersByPropertyNameMap.keySet()) {
			PropertyMapper<Q, F, ?> mapperItem = this.mappersByPropertyNameMap.get(filterPrp);
            if (mappersByPrp.containsKey(mapperItem.getFilterPrp())) {
                throw new QueryTemplateException(
                        String.format("buildQueryState: Error while adding property '%s'", mapperItem.getFilterPrp()));
            }
            mappersByPrp.put(mapperItem.getFilterPrp(), mapperItem);
		}

        StringBuilder queryTextMod = new StringBuilder();

        // Whether any parameter was already participating in the query.
        boolean anyProperty = false;
        // Connector.
        String connector = " "+this.config.getTargetReservedWordWhere()+" ";

        for (int i = 0; i < this.queryTextParts.length; i++) {
            queryTextMod.append(this.queryTextParts[i]);

            if (i < this.queryTextTokens.size() && this.queryTextTokens.get(i) != null) {
                QueryTemplateTokenPojo.TokenType type = this.queryTextTokens.get(i).getType();
				if (LOG.isTraceEnabled()) {
					LOG.trace("buildQueryState: Processing token " + i + " of type " + type + " with value '"
							+ this.queryTextTokens.get(i).getValue() + "'");
				}
                if (type == QueryTemplateTokenPojo.TokenType.FILTERS) {
                    anyProperty = false;
                    connector = " "+this.config.getTargetReservedWordAnd()+" ";
					if (LOG.isTraceEnabled()) {
						LOG.trace("buildQueryState: Resetting anyProperty to false and connector to AND (Dafault) for FILTERS token.");
					}
                } else if (type == QueryTemplateTokenPojo.TokenType.WHERE) {
                    connector = " "+this.config.getTargetReservedWordWhere()+" ";
                    if (LOG.isTraceEnabled()) {
                    	LOG.trace("buildQueryState: Setting connector to WHERE for WHERE token.");
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.AND) {
                    if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                        connector = " "+this.config.getTargetReservedWordAnd()+" ";
						if (LOG.isTraceEnabled()) {
							LOG.trace("buildQueryState: Found AND token and current connector does not contain WHERE. Setting connector to AND.");
						}
                    } else {
                    	if (LOG.isTraceEnabled()) {
                    		LOG.trace("buildQueryState: Found AND token but current connector already contains WHERE. Not changing connector.");
                    	}
                    }
//					if (LOG.isTraceEnabled()) {
//						LOG.trace("buildQueryState: Current connector: '" + connector + "'. Not setting it to AND because it already contains WHERE.");
//					}
                } else if (type == QueryTemplateTokenPojo.TokenType.OR) {
                    if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                        connector = " "+this.config.getTargetReservedWordOr()+" ";
                        if (LOG.isTraceEnabled()) {
                        	LOG.trace("buildQueryState: Setting connector to OR for OR token.");
                        }
                    }
                    if (LOG.isTraceEnabled()) {
                    	LOG.trace("buildQueryState: Current connector: '" + connector + "'. Not setting it to OR because it already contains WHERE.");
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.NO_OPERATOR) {
                    if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                        connector = "  ";
						if (LOG.isTraceEnabled()) {
							LOG.trace("buildQueryState: Setting connector to empty for NO_OPERATOR token. doble space.");
						}
                    }
					if (LOG.isTraceEnabled()) {
						LOG.trace("buildQueryState: Current connector: '" + connector
								+ "'. Not setting it to empty (doble space) for NO_OPERATOR token (  ) because it already contains WHERE.");
					}
                } else if (type == QueryTemplateTokenPojo.TokenType.OPEN_PARENTHESIS) {
                	OutputParam<Integer> indexOutput =  new OutputParam<Integer>(i);
                	OutputParam<Boolean> anyPropertyParenthesis = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParenthesisCriterion(indexOutput, connector, anyPropertyParenthesis,
                            filter, state));
					if (LOG.isTraceEnabled()) {
						if (anyPropertyParenthesis.getValue()) {
							LOG.trace(
									"buildQueryState: Parenthesis criterion has participating property/criterion. Setting anyProperty to true and next connector to AND (Default).");
						} else {
							LOG.trace(
									"buildQueryState: Parenthesis criterion has NO participating property/criterion. anyProperty remains "
											+ anyProperty);
						}
					}
                    i = indexOutput.getValue();
                    anyProperty = anyProperty || anyPropertyParenthesis.getValue();
                    if (anyProperty) {
                        connector = " "+this.config.getTargetReservedWordAnd()+" ";
                        //   There is no need to check if the current connector already contains WHERE, 
                        // because here we are setting only the default connector for the next connector 
                        // property/criterion after the parenthesis, which is AND. 
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
                	if (LOG.isTraceEnabled()) {
                		LOG.trace("buildQueryState: Processing EXTRA token: Fowarding to the next token to check for a CRITERION token. Current token index: " + i + ". Current token type: " + this.queryTextTokens.get(i).getType());
                	}
                    // Adding connector if it exists.
                    if (this.queryTextTokens.get(i).getType() == QueryTemplateTokenPojo.TokenType.AND) {
                        i++;
                        if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                            connector = " "+this.config.getTargetReservedWordAnd()+" ";
                            if (LOG.isTraceEnabled()) {
                            	LOG.trace(
                            			"buildQueryState: Processing EXTRA token. Found AND token after EXTRA token and current connector does not contain WHERE. Setting connector to AND. Forwarding to the next token. Current token index: "
                            					+ i + ". Current token type: " + this.queryTextTokens.get(i).getType());
                            }
                        } else {
                        	if (LOG.isTraceEnabled()) {
                        		LOG.trace(
                        				"buildQueryState: Processing EXTRA token. Found AND token after EXTRA token but current connector already contains WHERE. Not changing connector. Forwarding to the next token. Current token index: " 
                        						+ i + ". Current token type: " + this.queryTextTokens.get(i).getType());
                        	}                        	
                        }
                    } else if (this.queryTextTokens.get(i).getType() == QueryTemplateTokenPojo.TokenType.OR) {
                        i++;
                        if (!connector.contains(this.config.getTargetReservedWordWhere())) {
                            connector = " "+this.config.getTargetReservedWordOr()+" ";
							if (LOG.isTraceEnabled()) {
								LOG.trace(
										"buildQueryState: Processing EXTRA token. Found OR token after EXTRA token and current connector does not contain WHERE. Setting connector to OR. Forwarding to the next token. Current token index: "
												+ i + ". Current token type: " + this.queryTextTokens.get(i).getType());
							}
                        } else {
							if (LOG.isTraceEnabled()) {
								LOG.trace(
										"buildQueryState: Processing EXTRA token. Found OR token after EXTRA token but current connector already contains WHERE. Not changing connector. Forwarding to the next token. Current token index: "
												+ i + ". Current token type: " + this.queryTextTokens.get(i).getType());
							}
                        }
                    } else if (this.queryTextTokens.get(i).getType() == QueryTemplateTokenPojo.TokenType.NO_OPERATOR) {
                        i++;
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildQueryState: Processing EXTRA token. Found NO_OPERATOR token after EXTRA token. Not changing connector. Forwarding to the next token. Current token index: "
											+ i + ". Current token type: " + this.queryTextTokens.get(i).getType());
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
                    if (anyProperty) {
                        queryTextMod.append(connector).append(this.queryTextTokens.get(i).getValue());
						if (LOG.isTraceEnabled()) {
							LOG.trace("buildQueryState: Processing EXTRA token. There is criterion with participating parameters. Appending connector '" + connector
									+ "' and CRITERION token value '" + this.queryTextTokens.get(i).getValue()
									+ "' to queryTextMod.");
						}
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.QUERY_HELPER) {
                    queryTextMod.append(" ")
                            .append(this.buildQueryHelper(filter, this.queryTextTokens.get(i))).append(" ");
                } else if (type == QueryTemplateTokenPojo.TokenType.PARAMETER) {
                	OutputParam<Integer> indexOutput =  new OutputParam<Integer>(i);
                	OutputParam<Boolean> propertyParticipatesOutput = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildPropertyCriterion(indexOutput, connector, propertyParticipatesOutput, anyProperty,
                            filter, state));
                    i = indexOutput.getValue();
                    anyProperty = anyProperty || propertyParticipatesOutput.getValue();
                    if (anyProperty) {
                        connector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                    if (LOG.isTraceEnabled()) {
                    	LOG.trace(
                    			"buildQueryState: Processed PARAMETER token. Appended parameter criterion to queryTextMod. Updated indexOutput: "
                    					+ indexOutput.getValue() + ". propertyParticipatesOutput: "
                    					+ propertyParticipatesOutput.getValue() + ". anyProperty: " 
                    					+ anyProperty + ". Next connector set to AND (Default) for the next parameter/criterion.");
                    }
                } else {
                    throw new QueryTemplateException(
                            this.tokenExceptionMessage(i, this.queryTextTokens, "UNEXPECTED TOKEN"));
                }
            }
        }
        
        String queryTextModStr = queryTextMod.toString();
        
        if (this.config.isShrinkBlankLinesAfterBuild()) {
        	queryTextModStr = this.shrinkBlankLines(queryTextModStr);
        }
        
        state.setQueryString(queryTextModStr);
        
		if (LOG.isTraceEnabled()) {
			LOG.trace("buildQueryState: Final query string after processing all tokens: " + queryTextModStr);
		}
        
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

    private String unpackArrayParamCriterionIfNecessary(F filter, List<PropertyMapper<Q, F, ?>> criterionMappers, String criterion, QueryTemplateState<Q, F> state) {
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("unpackArrayParamCriterionIfNecessary: Checking if any parameter needs to be unpacked in the criterion: " + criterion);
    	}
    	
        StringBuilder repeatedCriterion = new StringBuilder();

        StringBuilder filterPrpsStr = new StringBuilder();
        String comma = "";
        // Checking the existence of some enumerable.
        Map<PropertyMapper, Object> unpackableValues = new LinkedHashMap<>();
        for (PropertyMapper<Q, F, ?> mapper : criterionMappers) {
            Object value = PropertyUtils.getProperty(filter, mapper.getFilterPrp());

            // In the case `any` is used one or more properties can be not participating in the query.
            if (mapper.getParticipatesInQuery().isParticipating(filter, mapper.getFilterPrp())) {
            	for (String parameterName : mapper.getParameterMappers().keySet()) {
					ParameterMapper<Q, F, ?> parameterMapper = mapper.getParameterMappers().get(parameterName);
					if (parameterMapper.isUnpackListItems()) {
						unpackableValues.put(mapper, value);
						
						if (LOG.isTraceEnabled()) {
							LOG.trace("unpackArrayParamCriterionIfNecessary: Parameter '" + parameterName
									+ "' is marked to unpack list items. Adding to unpackableValues map.");
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace("unpackArrayParamCriterionIfNecessary: Parameter '" + parameterName
									+ "' is NOT marked to unpack list items. Not adding to unpackableValues map.");
						}
					}
				}
            }
        }

        
        Collection<Object> valueColl = null;

        String currentTargetListItemConnector = "";
        String criterionSub = criterion;
        
		for (PropertyMapper<Q, F, ?> mapper : unpackableValues.keySet()) {
			Object value = unpackableValues.get(mapper);
            valueColl = CollectionUtil.toCollection(value);
            if (valueColl == null) {
                throw new QueryTemplateException(
                        "Some parameter of enumerable type expected. parameter: '" + filterPrpsStr + "'");
            }
			for (String parameterName : mapper.getParameterMappers().keySet()) {
				ParameterMapper<Q, F, ?> parameterMapper = mapper.getParameterMappers().get(parameterName);
				if (parameterMapper.isUnpackListItems()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace("unpackArrayParamCriterionIfNecessary: Unpacking parameter '" + parameterName
								+ "' with values: " + valueColl);
					}
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
					
					if (LOG.isTraceEnabled()) {
						LOG.trace("unpackArrayParamCriterionIfNecessary: Replaced parameter reference '"
								+ this.config.getParameterUsagePrefix() + parameterMapper.getParameterName()
								+ "' in criterion. New criterion: " + criterionSub);
					}
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
    private String repeatArrayParamCriterion(String filterPrpsStr, F filter, List<PropertyMapper<Q, F, ?>> criterionMappers,
            String repeatConnector, String criterion, QueryTemplateState<Q, F> state) {
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("repeatArrayParamCriterion: Processing criterion for repeatable parameters. Criterion: "
    				+ criterion + ". Repeat connector: '" + repeatConnector + "'. Filter properties: " + filterPrpsStr);
    	}
    	
        StringBuilder repeatedCriterion = new StringBuilder();

        String comma = "";
        Collection<Object> firstRepeaterValue = null;
        PropertyMapper firstRepeaterMapper = null;
        Set<PropertyMapper> repeatableMappers = new LinkedHashSet<>();
        // Checking the existence of some enumerable.
        for (PropertyMapper<Q, F, ?> mapper : criterionMappers) {
            Object value = PropertyUtils.getProperty(filter, mapper.getFilterPrp());

            for (String parameterName : mapper.getParameterMappers().keySet()) {
            	ParameterMapper<Q, F, ?> parameterMapper = mapper.getParameterMappers().get(parameterName);
            	// In the case `any` is used one or more properties can be not participating in the query.
            	if (value != null) {
            		if (parameterMapper.isRepeater()) {
						if (LOG.isTraceEnabled()) {
							LOG.trace("repeatArrayParamCriterion: Parameter '" + parameterName
									+ "' is marked as repeater. Adding to repeatableMappers set.");
						}
            			repeatableMappers.add(mapper);
            			if (firstRepeaterValue == null) {
            				firstRepeaterValue = CollectionUtil.toCollection(value);
            				firstRepeaterMapper = mapper;
            				
							if (LOG.isTraceEnabled()) {
								LOG.trace("repeatArrayParamCriterion: First repeater parameter found: '"
										+ parameterMapper.getParameterName() + "' with values: " + firstRepeaterValue);
							}
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
                    "Some property repitable expected. Properties: '" + filterPrpsStr + "'. Criterion: '" + criterion + "'");
        }

        String tempConnector = "  ";
        int repeatIndex = 0;
        for (Object valueItem : firstRepeaterValue) {
            repeatedCriterion.append(tempConnector);
            String criterionSub = criterion;
            for (PropertyMapper<Q, F, ?> repeatableMapper : repeatableMappers) {
            	for (String parameterName : repeatableMapper.getParameterMappers().keySet()) {
					ParameterMapper<Q, F, ?> parameterMapper = repeatableMapper.getParameterMappers().get(parameterName);
					if (parameterMapper.isRepeater()) {
						criterionSub = criterionSub.replace(this.config.getParameterUsagePrefix() + parameterMapper.getParameterName(),
								this.config.getParameterUsagePrefix() + parameterMapper.getParameterName() + "_" + repeatIndex);
						
						if (LOG.isTraceEnabled()) {
							LOG.trace("repeatArrayParamCriterion: Replaced parameter reference '"
									+ this.config.getParameterUsagePrefix() + parameterMapper.getParameterName()
									+ "' in criterion for repeater. New criterionSub: " + criterionSub);
						}
                    } else {
						if (LOG.isTraceEnabled()) {
							LOG.trace("repeatArrayParamCriterion: Parameter '" + parameterName
									+ "' is NOT a repeater. No replacement needed in criterionSub.");
						}
                    }
				}
            }

            repeatedCriterion.append(criterionSub);

            tempConnector = repeatConnector;
            repeatIndex++;
        }

        return repeatedCriterion.toString();
    }

    private void createEvalRunnerIfNecessary(QueryTemplateStateInternal<Q, F> state) throws Throwable {
		if (LOG.isDebugEnabled()) {
			LOG.debug("createEvalRunnerIfNecessary: Checking if EvalRunner needs to be created for the state.");
		}
    	if (state.getEvalRunner() == null) {
    		if (LOG.isDebugEnabled()) {
    			LOG.debug("createEvalRunnerIfNecessary: EvalRunner is null. Creating a new EvalRunner using the provided creator function. state.getEvalRunner(): " + state.getEvalRunner());
    		}
    		EvalRunner evalRunner = this.config.getEvalRunnerCreator().apply(state);
    		Map<String, Boolean> prpParticipations = new LinkedHashMap<>();
    		Map<String, Object> prpValues = new LinkedHashMap<>();
    		for (String filterPrp : this.mappersByPropertyNameMap.keySet()) {
    			PropertyMapper<Q, F, ?> mapperItem = this.mappersByPropertyNameMap.get(filterPrp);
    			Object prpValue = PropertyUtils.getProperty(state.getFilter(), filterPrp);
    			Boolean prpParticipation = mapperItem.getParticipatesInQuery().isParticipating(state.getFilter(), filterPrp);
    			prpParticipations.put(filterPrp, prpParticipation);
    			prpValues.put(filterPrp, prpValue);
    		}
			if (LOG.isTraceEnabled()) {
				LOG.trace("createEvalRunnerIfNecessary: Clearing existing bindings, setting new bindings for EvalRunner and associating it with the state. "
						+ ". prpParticipations/pp: "
						+ prpParticipations + ". prpValues/pv: " + prpValues);
			}
			
			evalRunner.clearBindings(state);
			evalRunner.binding(state, "prpParticipations", prpParticipations);
			evalRunner.binding(state, "pp", prpParticipations);
			evalRunner.binding(state, "prpValues", prpValues);
			evalRunner.binding(state, "pv", prpValues);
			
    		state.setEvalRunner(evalRunner);
    	} else {
    		if (LOG.isDebugEnabled()) {
    			LOG.debug("createEvalRunnerIfNecessary: EvalRunner already exists for the state. No need to create a new one. state.getEvalRunner(): " + state.getEvalRunner());
    		}
    	}
    }
    
    /**
     * Assembles a part of the query that depends on the parameter(s) currently
     * being processed. Returns the part along with the connector.
     */
    private String buildPropertyCriterion(OutputParam<Integer> indexOutput, String currentConnector,
    		OutputParam<Boolean> propertyParticipatesOutput, 
    		boolean anyPropertyBefore, F filter, 
            QueryTemplateStateInternal<Q, F> state) {
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("buildPropertyCriterion: Starting to build parameter criterion. Current indexOutput: " + indexOutput.getValue());
    	}
        StringBuilder queryTextMod = new StringBuilder();
        boolean repeatTokenActive = false;
        String repeatConnector = "  ";
        propertyParticipatesOutput.setValue(false);
        String[] filterPrps = new String[] {};
        String evalPrps = null;
        String filterPrpsStr = "";

        boolean firstIteration = true;
        while (true) {
            if (!firstIteration) {
            	queryTextMod.append(this.queryTextParts[indexOutput.getValue()]);
            	if (LOG.isTraceEnabled()) {
            		LOG.trace("buildPropertyCriterion: Appended queryTextParts[" + indexOutput.getValue() + "] to queryTextMod.");
            	}
            }

            if (indexOutput.getValue() < this.queryTextTokens.size() && this.queryTextTokens.get(indexOutput.getValue()) != null) {
                QueryTemplateTokenPojo.TokenType type = this.queryTextTokens.get(indexOutput.getValue()).getType();
                if (type == QueryTemplateTokenPojo.TokenType.PARAMETER) {
                	String tokenValue = this.queryTextTokens.get(indexOutput.getValue()).getValue();
                	if (LOG.isTraceEnabled()) {
                		LOG.trace("buildPropertyCriterion: Processing PARAMETER token with value '" + tokenValue + "' at index " + indexOutput.getValue());
                	}
                    Pattern rxReservedEvalReplacer = Pattern.compile(this.config.getReservedEvalProperty());
                    Pattern rxReservedEvalMatcher = Pattern.compile("(?s)\\s*" + this.config.getReservedEvalProperty() + ".*");
                    if (rxReservedEvalMatcher.matcher(tokenValue).matches()) {
                    	evalPrps = rxReservedEvalReplacer.matcher(tokenValue).replaceAll("").trim();
                    	if (LOG.isTraceEnabled()) {
                    		LOG.trace("buildPropertyCriterion: PARAMETER token value '" + tokenValue
                    				+ "' matches the reserved eval property pattern. Extracting eval expression.");
                    	}
                    } else {
                    	evalPrps = null;
                    	repeatTokenActive = false;
                    	filterPrpsStr = this.queryTextTokens.get(indexOutput.getValue()).getValue();
                    	filterPrps = filterPrpsStr.split(",", -1);
                    	if (LOG.isTraceEnabled()) {
                    		LOG.trace("buildPropertyCriterion: PARAMETER token value '" + tokenValue
                    				+ "' does NOT match the reserved eval property pattern. Extracted filter properties: "
                    				+ Arrays.toString(filterPrps));
                    	}
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.AND) {
                    repeatConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    if (anyPropertyBefore) {
                        currentConnector = " "+this.config.getTargetReservedWordAnd()+" ";
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildPropertyCriterion: AND token found. Setting currentConnector to AND because there is some property/criterion participating before. `properties`: " + filterPrpsStr);
						}
                    } else {
                    	if (LOG.isTraceEnabled()) {
                    		LOG.trace(
                    				"buildPropertyCriterion: AND token found. Not changing currentConnector because there is NO property/criterion participating before. `properties`: " + filterPrpsStr);
                    	}
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.OR) {
                    repeatConnector = " "+this.config.getTargetReservedWordOr()+" ";
                    if (anyPropertyBefore) {
                        currentConnector = " "+this.config.getTargetReservedWordOr()+" ";
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildPropertyCriterion: OR token found. Setting currentConnector to OR because there is some property/criterion participating before. `properties`: " + filterPrpsStr);
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildPropertyCriterion: OR token found. Not changing currentConnector because there is NO property/criterion participating before. `properties`: " + filterPrpsStr);
						}
					}
                } else if (type == QueryTemplateTokenPojo.TokenType.NO_OPERATOR) {
                    repeatConnector = "  ";
                    if (LOG.isTraceEnabled()) {
						LOG.trace(
								"buildPropertyCriterion: NO_OPERATOR token found. Setting repeatConnector to empty (double space).");
                    }
                    if (!currentConnector.contains(this.config.getTargetReservedWordWhere())) {
                        currentConnector = "  ";
                        if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildPropertyCriterion: NO_OPERATOR token found. Setting currentConnector to empty (double space) because it does not contain WHERE.");
                        }
                    } else {
                    	if (LOG.isTraceEnabled()) {
                    		LOG.trace("buildPropertyCriterion: NO_OPERATOR token found. Not changing currentConnector because it already contains WHERE.");
                    	}
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.REPEAT) {
                    repeatTokenActive = true;
					if (LOG.isTraceEnabled()) {
						LOG.trace("buildPropertyCriterion: REPEAT token found. Setting repeatTokenActive to true.");
					}
                } else if (type == QueryTemplateTokenPojo.TokenType.CRITERION
                        || type == QueryTemplateTokenPojo.TokenType.QUERY_HELPER) {
					if (LOG.isTraceEnabled()) {
						LOG.trace("buildPropertyCriterion: CRITERION or QUERY_HELPER token found. Ending parameter criterion processing.");
					}
                	
                	List<PropertyMapper<Q, F, ?>> criterionMappers = new ArrayList<>();
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
							if (LOG.isTraceEnabled()) {
								LOG.trace("buildPropertyCriterion: Evaluated expression '" + evalPrps
										+ "' with result: " + cumulativePrpsParticipates);
							}
						} catch (Throwable e) {
							throw new QueryTemplateException("Error evaluating expression: '" + evalPrps + "'", e);
						}
                    } else {
                        boolean anyInsteadOfAll = false;
                        if (filterPrps.length == 0) {
                            throw new QueryTemplateException("'" + QueryTemplateTokenPojo.TokenType.PARAMETER
                                    + "' not defined. Before: '" + this.queryTextTokens.get(indexOutput.getValue()).getValue() + "'");
                        }
                    	Pattern rxReservedAnyReplacer = Pattern.compile(this.config.getReservedAnyProperty());
                    	Pattern rxReservedAnyMatcher = Pattern.compile(" *" + this.config.getReservedAnyProperty() + ".*");
                    	if (rxReservedAnyMatcher.matcher(filterPrps[0]).matches()) {
                    		anyInsteadOfAll = true;
                    		filterPrps[0] = rxReservedAnyReplacer.matcher(filterPrps[0]).replaceAll("").trim();
                    		
                    		//criterionMappers can not be populated here because the property names are not known when eval is used. The property names will be known from parameters found into the criterion.
                    		if (LOG.isTraceEnabled()) {
                    			LOG.trace("buildPropertyCriterion: anyInsteadOfAll set to " + anyInsteadOfAll);
                    		}
                    	}
                    	cumulativePrpsParticipates = !anyInsteadOfAll;

                    	for (String filterPrp : filterPrps) {
                    		String filterPrpTrim = filterPrp.trim();
                    		boolean negateVerification = false;
                    		if (filterPrpTrim.startsWith("!")) {
                    			negateVerification = true;
                    			filterPrpTrim = filterPrpTrim.replace("!", "").trim();
								if (LOG.isTraceEnabled()) {
									LOG.trace("buildPropertyCriterion: Negate verification for property '"
											+ filterPrpTrim + "'.");
								}
                    		}
                    		
                    		// Whether the property is participating in the query.
                    		PropertyMapper<Q, F, ?> mapper = null;
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
                    			if (LOG.isTraceEnabled()) {
                    				LOG.trace("buildPropertyCriterion: '" + filterPrp + "' returns " + itemPrpParticipates + ". Cumulative participation: "
                    						+ cumulativePrpsParticipates);
                    			}
                    		} else {
                    			throw new QueryTemplateException("unmapped property listed: " + filterPrpTrim);
                    		}
                    		
                    	}
                    }

                    String criterionPrp = "";
                    if (cumulativePrpsParticipates) {
                    	if (LOG.isTraceEnabled()) {
                    		LOG.trace("buildPropertyCriterion: Cumulative participation is true. Processing criterion for filter properties: "
									+ filterPrpsStr + ". Current token value: "
									+ this.queryTextTokens.get(indexOutput.getValue()).getValue());
                    	}
                        if (type == QueryTemplateTokenPojo.TokenType.CRITERION) {
                        	// #region criterionMappersByParameterUsage
                        	Set<ParameterMapper<Q, F, ?>> criterionParametersUsedSet = new LinkedHashSet<>();
                			String parameterUsagePatternStr = this.config.getParameterUsagePrefix() + this.config.getParameterNamePattern();
                			Pattern parameterUsagePattern = Pattern.compile(parameterUsagePatternStr);
                			Matcher matcher = parameterUsagePattern.matcher(this.queryTextTokens.get(indexOutput.getValue()).getValue());
                			int matcherIndex = 0;
                			while (true) {
                				if (!matcher.find(matcherIndex)) {
                					break;
                				}
                				matcherIndex = matcher.end();
                				String parameterName = matcher.group(1);
                				PropertyMapper<Q, F, ?> mapperItem = this.mappersByParamNameMap.get(parameterName);
                				if (mapperItem != null) {
                					ParameterMapper<Q, F, ?> parameterMapper = mapperItem.getParameterMappers().get(parameterName);
                					criterionParametersUsedSet.add(parameterMapper);
                				}
                			}
                        	// #endregion
                        	if (evalPrps != null) {
                        		criterionMappers.clear();
                        		for (ParameterMapper<Q, F, ?> parameterMapper : criterionParametersUsedSet) {
									criterionMappers.add(parameterMapper.getOwner());
								}
                        	}
                            if (repeatTokenActive) {
                                criterionPrp = this.repeatArrayParamCriterion(filterPrpsStr, filter, criterionMappers,
                                        repeatConnector, this.queryTextTokens.get(indexOutput.getValue()).getValue(), state);
                            } else {          
                            	for (ParameterMapper<Q, F, ?> parameterMapper : criterionParametersUsedSet) {
                            		if (parameterMapper.isRepeater() && parameterMapper.getOwner().getParticipatesInQuery().isParticipating(filter, parameterMapper.getOwner().getFilterPrp())) {
                            			throw new QueryTemplateException("The parameter '" + parameterMapper.getParameterName()
                            					+ "' is participating in the query, and is repeater, so it must be used with the "
                            					+ this.config.getRepeatToken().pattern().replace("\\", "") + " token. "
                            					+ this.tokenExceptionMessage(indexOutput.getValue(), this.queryTextTokens,
                            							"Disalowed repeater parameter usage"));
                            		}
                            	}
                            	
                    			criterionPrp = this.unpackArrayParamCriterionIfNecessary(filter, criterionMappers,
                    					this.queryTextTokens.get(indexOutput.getValue()).getValue(), state);
                            }
                        } else if (type == QueryTemplateTokenPojo.TokenType.QUERY_HELPER) {
                            if (repeatTokenActive) {
                                throw new QueryTemplateException(String.format(
                                        "There is a token '%3$s' before the query helper. Type='%1$s'; Value :'%2$s'.",
                                        this.queryTextTokens.get(indexOutput.getValue()).getType(),
                                        this.queryTextTokens.get(indexOutput.getValue()).getValue(),
                                        this.config.getRepeatToken().toString()));
                            }
                            criterionPrp = this.buildQueryHelper(filter, this.queryTextTokens.get(indexOutput.getValue()));
                        } else {
                            throw new QueryTemplateException(
                                    this.tokenExceptionMessage(indexOutput.getValue(), this.queryTextTokens, "UNEXPECTED TOKEN"));
                        }

                        queryTextMod.append(currentConnector).append(criterionPrp).append(" ");
                        propertyParticipatesOutput.setValue(true);
                        // Mistake: This has no effect, because the currentConnector is not used anymore after this point, `break` is used just after this point. The currentConnector is used only in the next iteration of the loop, but the loop will break immediately after this point, so the currentConnector is not used anymore.  
                        //currentConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                        
                    } else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildPropertyCriterion: No participating properties found for filter properties: "
											+ filterPrpsStr + ". Not appending any criterion to queryTextMod.");
						}
                    }

                    break;
                } else {
                    throw new QueryTemplateException(
                            this.tokenExceptionMessage(indexOutput.getValue(), this.queryTextTokens, "UNEXPECTED TOKEN"));
                }
            } else {
                int errorIndex = Math.min(this.queryTextTokens.size() - 1, indexOutput.getValue());
                throw new QueryTemplateException("Expected token: " + QueryTemplateTokenPojo.TokenType.CRITERION + ". \n"
                        + String.format("Current token. Type='%1$s'; Value :'%2$s'. \nError: %3$s\n",
                                this.queryTextTokens.get(errorIndex).getType().toString(),
                                this.queryTextTokens.get(errorIndex).getValue(),
                                "\n" + this.showErrorPosition(this.queryTextEscapedSubs,
                                        this.queryTextTokens.get(errorIndex).getIndexInString())));
            }

            firstIteration = false;
            //index[0]++;
            indexOutput.setValue(indexOutput.getValue() + 1);
        }

        if (LOG.isDebugEnabled()) {
			LOG.debug("buildPropertyCriterion: Finished building parameter criterion. propertyParticipatesOutput: "
					+ propertyParticipatesOutput.getValue()+ "\n. "
					+ "Final queryTextMod: "
					+ queryTextMod.toString());
        }
        
        return queryTextMod.toString();
    }

    /**
     * Handles the part inside a parenthesis. If no inner part is included, the
     * parentheses are not included either.
     */
    private String buildParenthesisCriterion(OutputParam<Integer> indexOutput, String currentConnector,
            OutputParam<Boolean> anyPropertyParenthesisOutput, F filter, 
            QueryTemplateStateInternal<Q, F> state) {
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("buildParenthesisCriterion: Starting to build parenthesis criterion. Current indexOutput: " + indexOutput.getValue());
    	}
    	
        StringBuilder queryTextMod = new StringBuilder();
        anyPropertyParenthesisOutput.setValue(false);
        String innerParenthesisConnector = "  ";

        while (true) {
            // The increment must be here at the beginning, otherwise nested parentheses
            // break: closing the inner parenthesis closes all of its parents.
            //index[0]++;
        	indexOutput.setValue(indexOutput.getValue() + 1);

        	queryTextMod.append(this.queryTextParts[indexOutput.getValue()]);

            if (indexOutput.getValue() < this.queryTextTokens.size() && this.queryTextTokens.get(indexOutput.getValue()) != null) {
                QueryTemplateTokenPojo.TokenType type = this.queryTextTokens.get(indexOutput.getValue()).getType();
                if (type == QueryTemplateTokenPojo.TokenType.AND) {
                    if (anyPropertyParenthesisOutput.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildParenthesisCriterion: AND token found. Setting innerParenthesisConnector to AND because there is some property/criterion participating in the parenthesis.");
						}
                    } else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildParenthesisCriterion: AND token found. Not changing innerParenthesisConnector because there is NO property/criterion participating in the parenthesis.");
						}
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.OR) {
                    if (anyPropertyParenthesisOutput.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                        if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildParenthesisCriterion: OR token found. Setting innerParenthesisConnector to OR because there is some property/criterion participating in the parenthesis.");
                        }
                    } else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildParenthesisCriterion: OR token found. Not changing innerParenthesisConnector because there is NO property/criterion participating in the parenthesis.");
						}
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.NO_OPERATOR) {
                    innerParenthesisConnector = "  ";
                    if (LOG.isTraceEnabled()) {
						LOG.trace(
								"buildParenthesisCriterion: NO_OPERATOR token found. Setting innerParenthesisConnector to empty (double space).");
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.OPEN_PARENTHESIS) {
                	if (LOG.isTraceEnabled()) {
                		LOG.trace("buildParenthesisCriterion: OPEN_PARENTHESIS token found. Recursively processing inner parenthesis.");
                	}
                	OutputParam<Boolean> anyPropertySubParenthesis = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParenthesisCriterion(indexOutput, innerParenthesisConnector,
                            anyPropertySubParenthesis, filter, state));
					if (LOG.isTraceEnabled()) {
						if (anyPropertySubParenthesis.getValue()) {
							LOG.trace(
									"buildParenthesisCriterion: Inner Parenthesis has participating property/criterion. Setting anyPropertyParenthesisOutput to true and next inner connector to AND (Default).");
						} else {
							LOG.trace(
									"buildParenthesisCriterion: Inner Parenthesis criterion has NO participating property/criterion. anyPropertyParenthesisOutput remains "
											+ anyPropertyParenthesisOutput);
						}
					}
                    anyPropertyParenthesisOutput.setValue(anyPropertyParenthesisOutput.getValue() || anyPropertySubParenthesis.getValue());
                    if (anyPropertyParenthesisOutput.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                        //   There is no need to check if the current connector is the firts one (AND, OR, NO_OPERATOR, etc.), 
                        // because here we are setting only the default connector for the next connector 
                        // property/criterion after the parenthesis, which is AND.
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.PARAMETER) {
                	OutputParam<Boolean> propertyParticipatesOutput = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildPropertyCriterion(indexOutput, innerParenthesisConnector,
                            propertyParticipatesOutput, anyPropertyParenthesisOutput.getValue(), filter, state));                    
                    
                    anyPropertyParenthesisOutput.setValue(anyPropertyParenthesisOutput.getValue() || propertyParticipatesOutput.getValue());
                    if (anyPropertyParenthesisOutput.getValue()) {
                        innerParenthesisConnector = " "+this.config.getTargetReservedWordAnd()+" ";
                    }
                    if (LOG.isTraceEnabled()) {
                    	LOG.trace(
                    			"buildParenthesisCriterion: Processed PARAMETER token. Appended parameter criterion to queryTextMod. Updated indexOutput: "
                    					+ indexOutput.getValue() + ". propertyParticipatesOutput: "
                    					+ propertyParticipatesOutput.getValue() + ". anyPropertyParenthesisOutput: " 
                    					+ anyPropertyParenthesisOutput.getValue() + ". Next inner connector set to AND (Default) for the next inner property/criterion.");
                    }
                } else if (type == QueryTemplateTokenPojo.TokenType.CLOSE_PARENTHESIS) {
                    if (anyPropertyParenthesisOutput.getValue()) {
                        queryTextMod = new StringBuilder(currentConnector + " "+this.config.getTargetReservedWordOpenParenthesis()+" " + queryTextMod + " "+this.config.getTargetReservedWordCloseParenthesis()+" ");
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildParenthesisCriterion: CLOSE_PARENTHESIS token found. There is some property/criterion participating in the parenthesis. Wrapping queryTextMod with parentheses and prepending currentConnector.");
						}
                    } else {
						if (LOG.isTraceEnabled()) {
							LOG.trace(
									"buildParenthesisCriterion: CLOSE_PARENTHESIS token found. There is NO property/criterion participating in the parenthesis. Not wrapping queryTextMod with parentheses.");
						}
                    }
                    break;
                } else {
                    throw new QueryTemplateException(
                            this.tokenExceptionMessage(indexOutput.getValue(), this.queryTextTokens, "UNEXPECTED TOKEN"));
                }
            } else {
                throw new QueryTemplateException(
                        "Expected token: " + QueryTemplateTokenPojo.TokenType.CLOSE_PARENTHESIS);
            }
        }

        return queryTextMod.toString();
    }

    private String buildQueryHelper(F filter, QueryTemplateTokenPojo queryHelperTokenTo) {
        String queryTextHelper;

        String queryHelperKey = queryHelperTokenTo.getValue().split(":")[1].trim();
        if (this.queryHelpers.containsKey(queryHelperKey)) {
            QueryTemplate<Q, F> queryHelper = this.queryHelpers.get(queryHelperKey);
            QueryTemplateState<Q, F> helperState = queryHelper.buildQueryState(filter);
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
	private List<QueryTemplateTokenPojo> buildTokens(String queryText,
		int length,
		Pattern pt/*
					 * , Set<PropertyMapper<Q, F, ?>> usableMappers
					 */) {
        Matcher mt = pt.matcher(queryText);
        
		if (LOG.isDebugEnabled()) {
			LOG.debug("buildTokens: Starting tokenization of query text. Tokenization pattern: " + pt.pattern() + ". Query text length: " + length);
		}

        List<QueryTemplateTokenPojo> tokens = new ArrayList<>(length);

        while (mt.find()) {
            int start = mt.start();
            String tokenStr = mt.group();
			if (LOG.isTraceEnabled()) {
				LOG.trace("buildTokens: Found token at index " + start + ": '" + tokenStr + "'");
			}
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
//					if (LOG.isTraceEnabled()) {
//						LOG.trace("buildTokens: PARAMETER token value '" + token.getValue()
//								+ "' matches the eval property pattern ('"+this.config.getReservedEvalProperty() 
//								+ "'). No mappers are added to usableMappers.");
//					}
                } else {
//					if (LOG.isTraceEnabled()) {
//						LOG.trace("buildTokens: PARAMETER token value '" + token.getValue()
//								+ "' does NOT match the eval property pattern ('"
//								+ this.config.getReservedEvalProperty()
//								+ "'). Extracting property names and adding corresponding mappers to usableMappers.");
//					}
					if (LOG.isTraceEnabled()) {
						LOG.trace("buildTokens: Extracting property names from PARAMETER token value '"
								+ token.getValue() + "' to validate existence of corresponding mappers.");
					}
                	// Storing the parameter in the list of possible parameters.
                	String[] filterPrps = token.getValue().split(",", -1);
                	for (String filterPrp : filterPrps) {
                		String cleanProperty = filterPrp;
                		Pattern rxReservedAnyReplacer = Pattern.compile(this.config.getReservedAnyProperty());
                		cleanProperty = cleanProperty.replace("!", "");
                		cleanProperty = rxReservedAnyReplacer.matcher(cleanProperty).replaceAll("");
                		cleanProperty = cleanProperty.trim();
                		
                		PropertyMapper<Q, F, ?> mapper = this.mappersByPropertyNameMap.get(cleanProperty);
                		if (mapper == null) {
                			throw new QueryTemplateException("Unmapped property listed: " + cleanProperty);
                		}
//                		usableMappers.add(mapper);
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
		
//		while (true) {
//			if (!matcher.find(matcherIndex)) {
//				break;
//			}
//			matcherIndex = matcher.end();
//			String parameterUsage = matcher.group(1);
//			PropertyMapper<Q, F, ?> mapper = this.mappersByParamNameMap.get(parameterUsage);
//			
//			//usableMappers.add(mapper);
//		}
//        // #endregion
//		if (LOG.isDebugEnabled()) {
//			LOG.debug("buildTokens: Finished tokenization. Total tokens found: " + tokens.size() + ". Usable mappers found: " + usableMappers.size());
//		}
//		if (LOG.isTraceEnabled()) {
//			LOG.trace("buildTokens: Usable mappers: " + usableMappers.toString());
//		}

        return tokens;
    }

//    /**
//     * Returns the usable parameters of the query, i.e. the parameters that appear in
//     * it.
//     *
//     * @return the usable parameters.
//     */
//    @Override
//	public Set<PropertyMapper<Q, F, ?>> getUsableMappers() {
//        return this.usableMappers;
//    }

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

            if (LOG.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{ ");
                for (Map.Entry<String, String> entry : escapeMap.entrySet()) {
                    sb.append("'").append(entry.getKey()).append("'='").append(entry.getValue()).append("'; ");
                }
                sb.append("}");
                LOG.trace("replaceEscapes: Escape mapping: " + sb.toString());
            }

            replacedText.append(text, lastIndex, text.length());
            if (LOG.isTraceEnabled()) {
                LOG.trace("replaceEscapes: replacedText: '" + replacedText + "'");
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
            if (LOG.isTraceEnabled()) {
                LOG.trace("restoreEscapes: restoredText BEFORE: '" + restoredText + "'");
            }

            for (Map.Entry<String, String> entry : escapeMap.entrySet()) {
                if (restoredText != null) {
                    restoredText = restoredText.replace(entry.getKey(), entry.getValue());
                } else {
                    break;
                }
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("restoreEscapes: restoredText AFTER: '" + restoredText + "'");
            }
        }

        return restoredText;
    }
}
