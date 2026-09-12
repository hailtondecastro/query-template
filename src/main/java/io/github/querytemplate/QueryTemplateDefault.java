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
    private Set<String> usableParameters = null;

    private boolean compactQueryText = false;

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
    private Map<String, PropertyMapper<Q, ?>> mappersMap;
    
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
    	String queryText = this.config.getQueryTextOriginal();
    	
		this.mappersMap = new LinkedHashMap<>();
    	for (String propertyNameItem : this.config.getMappersConfigMap().keySet()) {
    		PropertyMapperConfig<Q, Object> mapperConfig = (PropertyMapperConfig<Q, Object>) config.getMappersConfigMap().get(propertyNameItem);
    		PropertyMapper<Q, Object> mapper = 
    				new PropertyMapper<>(propertyNameItem);
    		mapper
    			.onFilled(mapperConfig.getOnFilledNamed())
    			.onFilled(mapperConfig.getOnFilledPositional())
    			.fillVerifier(mapperConfig.getFillVerifier())
    			.unpackListItems(mapperConfig.isUnpackListItems())
    			.repeater(mapperConfig.isRepeater());
    		this.mappersMap.put(mapper.getFilterPrp(), mapper);
    	}
    	
        if (this.compactQueryText) {
            queryText = this.compactInputQueryText(this.config.getQueryTextOriginal());
        }

        this.usableParameters = new LinkedHashSet<>();

        this.splitterIntoPartsPattern = "(?s)" + this.config.getFiltersToken() + "|" + this.config.getWhereToken() + "|" + this.config.getAndToken() + "|" + this.config.getOrToken()
                + "|" + this.config.getNoOperatorToken() + "|" + this.config.getRepeatToken() + "|" + this.config.getOpenParenthesisToken() + "|"
                + this.config.getCloseParenthesisToken() + "|" + this.config.getExtraToken() + "|" + this.config.getParamToken() + "|" + this.config.getCriterionToken();
        Pattern pt = Pattern.compile(this.splitterIntoPartsPattern);

        Map<String, String> escapeMap = new LinkedHashMap<>();
        this.queryTextEscapedSubs = this.replaceEscapes(queryText, escapeMap);

        this.queryTextParts = pt.split(this.queryTextEscapedSubs, -1);
        this.queryTextTokens = this.buildTokens(this.queryTextEscapedSubs, this.queryTextParts.length, pt,
                this.usableParameters);

        this.restoreEscapes(this.queryTextParts, escapeMap);
        this.restoreEscapesTokens(this.queryTextTokens, escapeMap);
        
        this.setUpQueryHelpers();

        if (LOG.isDebugEnabled()) {
            StringBuilder logStr = new StringBuilder("\nFixed parts:\n");
            for (int i = 0; i < this.queryTextParts.length; i++) {
                logStr.append(i).append(": ").append(this.queryTextParts[i]).append("\n");
            }
            logStr.append("Parts to assemble:\n");
            for (int i = 0; i < this.queryTextTokens.size(); i++) {
                logStr.append(i).append(": ").append(this.queryTextTokens.get(i).getValue()).append("\n");
            }
            LOG.debug(logStr.toString());
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
        String compacted = queryText.replace("\n", " ");
        compacted = compacted.replace("\r", " ");

        int length = compacted.length();
        do {
            compacted = compacted.replace("   ", " ");
            if (compacted.length() == length) {
                break;
            } else {
                length = compacted.length();
            }
        } while (true);

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
     * Assigns values to the parameters based on the fill state.
     *
     * @param filterObject     the filter object.
     * @param query            the query to set the parameters on.
     * @param usableParameters usable parameters in the query. Only parameters
     *                         contained in this set will be used.
     */
    void setParamQueryNonRecursive(QueryTemplateState<Q> state, Q query, Map<Integer, Action> positionalParameterActions) {
        for (String mapperItemKey : this.mappersMap.keySet()) {
        	PropertyMapper<Q, ?> mapperItem = this.mappersMap.get(mapperItemKey);
        	PropertyMapper<Q, Object> mapperItemCasted = (PropertyMapper<Q, Object>) mapperItem;
            // Tests whether the parameter is used in the query and whether it is filled.
            if (usableParameters.contains(mapperItemCasted.getFilterPrp())
                    && mapperItemCasted.getFillVerifier().isFilled(state.getFilter(), mapperItemCasted.getFilterPrp())) {
                try {
                    Object value = PropertyUtils.getProperty(state.getFilter(), mapperItemCasted.getFilterPrp());
                    //if (mapperItemCasted.isUnfoldEnumerable()) {
                    if (mapperItemCasted.isUnpackListItems() || mapperItemCasted.isRepeater()) {
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
                        		if (mapperItemCasted.getOnFilledNamed() != null) {
                                	mapperItemCasted.getOnFilledNamed().accept(query, mapperItemCasted.getFilterPrp() + "_" + repeatIndex, valueItem);
                                }
                        	} else {
								if (mapperItemCasted.getOnFilledPositional() != null) {
									List<Integer> parameterPositions = state
											.getPropertyMapperItemIndexToParameterPositions().get(mapperItemCasted)
											.get(repeatIndex);
									for (Integer parameterPosition : parameterPositions) {
										// delaying the execution of the parameter setting to avoid issues with the order of execution and potential side effects
										positionalParameterActions.put(parameterPosition, () -> mapperItemCasted.getOnFilledPositional().accept(query, parameterPosition, valueItem));
									}
								}
                        	}
                            repeatIndex++;
                        }
                    } else if (value instanceof FragmentInclusion) {
                        // nothing
                    } else {
                    	if (!this.config.isConvertNamedToPositionalParameters()) {
                    		if (mapperItemCasted.getOnFilledNamed() != null) {
                    			mapperItemCasted.getOnFilledNamed().accept(query, mapperItemCasted.getFilterPrp(), value);
                    		}                    		
                    	} else {
                    		if (mapperItemCasted.getOnFilledPositional() != null) {
                    			List<Integer> parameterPositions = state.getPropertyMapperToParameterPositions().get(mapperItemCasted);
                    			for (Integer parameterPosition : parameterPositions) {
                    				// delaying the execution of the parameter setting to avoid issues with the order of execution and potential side effects
                    				positionalParameterActions.put(parameterPosition, () -> mapperItemCasted.getOnFilledPositional().accept(query, parameterPosition, value));
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

        this.setParamQueryNonRecursive(state, query, positionalParameterActions);
    }

    /**
     * Assigns values to the parameters based on the fill state..
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
     * Assigns values to the parameters based on the fill state..
     *
     * @param state 	the state of the query template.
     * @param query 	the query to set the parameters on.
     */
    @Override
	public void setParamQuery(QueryTemplateState<Q> state, Q query) {
    	Map<Integer, Action> positionalParameterActions = new LinkedHashMap<>();
    	this.setParamQuery(state, query, positionalParameterActions);
    }

	private void processPositionalParameters(QueryTemplateState<Q> state) {
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
				String parameterIndexSufix = "";
				Matcher indexSufixMatcher = indexSufixPattern.matcher(parameterWithIndexSufix);
				indexSufixMatcher.find();
				if (!indexSufixMatcher.matches()) {
					parameterName = parameterWithIndexSufix;
				} else {
					parameterName = indexSufixMatcher.group(1);
					parameterIndexSufix = indexSufixMatcher.group(2);
				}
				if (!this.mappersMap.containsKey(parameterName)) {
					throw new QueryTemplateException(
							"Parameter name '" + parameterName + "' is not present in the mappers.");
				}
				if (!parameterWithIndexSufix.equals(parameterName) && this.mappersMap.containsKey(parameterWithIndexSufix) && this.mappersMap.containsKey(parameterName)) {
					throw new QueryTemplateException("Parameter with index sufix '" + parameterWithIndexSufix + "' and parameter name '" + parameterName + "' are both present in the mappers map. This is not allowed.");
                }

				PropertyMapper<Q, ?> mapperItem = this.mappersMap.get(parameterName);
				//if (mapperItem.isUnfoldEnumerable()) {
				//if (state.getIsRepeatablePropertyMapper().get(mapperItem)) {
				if (mapperItem.isRepeater() || mapperItem.isUnpackListItems()) {
					if (!state.getPropertyMapperItemIndexToParameterPositions().containsKey(mapperItem)) {
	                    state.getPropertyMapperItemIndexToParameterPositions().put(mapperItem, new LinkedHashMap<>());
	                }
					Map<Integer, List<Integer>> itemIndexToParameterPositions = state.getPropertyMapperItemIndexToParameterPositions().get(mapperItem);
					int itemIndex = Integer.parseInt(parameterIndexSufix);
					if (!itemIndexToParameterPositions.containsKey(itemIndex)) {
						itemIndexToParameterPositions.put(itemIndex, new ArrayList<>());
					}
					itemIndexToParameterPositions.get(itemIndex).add(parameterPosition);
					parameterPosition++;
				} else {
					if (!state.getPropertyMapperToParameterPositions().containsKey(mapperItem)) {
						state.getPropertyMapperToParameterPositions().put(mapperItem, new ArrayList<>());
					}
					List<Integer> parameterPositions = state.getPropertyMapperToParameterPositions().get(mapperItem);
					parameterPositions.add(parameterPosition);
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
     * Assembles the query according to the parameters that are filled.
     * @param filter the filter object.
     * @return the assembled query.
     */
    @Override
	public QueryTemplateState<Q> buildQueryState(Object filter) {
    	// TODO: Continuar daqui
    	QueryTemplateState<Q> state = new QueryTemplateState<>(this, null, filter, new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    	
        // Auxiliary variable to access the transfer objects by property name.
        Map<String, PropertyMapper> mappers = new LinkedHashMap<>();
        for (String filterPrp : this.mappersMap.keySet()) {
			PropertyMapper<Q, ?> mapperItem = this.mappersMap.get(filterPrp);
            if (mappers.containsKey(mapperItem.getFilterPrp())) {
                throw new QueryTemplateException(
                        String.format("Error while adding parameter '%s'", mapperItem.getFilterPrp()));
            }
            mappers.put(mapperItem.getFilterPrp(), mapperItem);
		}

        StringBuilder queryTextMod = new StringBuilder();

        // Whether any parameter was already filled.
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
                	//boolean[] paramFilled = { false };
                	OutputParam<Integer> index =  new OutputParam<Integer>(i);
                	OutputParam<Boolean> paramFilled = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParameterCriterion(index, connector, paramFilled, anyParam,
                            filter, state));
                    i = index.getValue();
                    anyParam = anyParam || paramFilled.getValue();
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

            // In the case `any` is used one or more properties can be unfilled.
            if (mapper.getFillVerifier().isFilled(filter, mapper.getFilterPrp())
            		&& mapper.isUnpackListItems()) {
            	unpackableValues.put(mapper, value);
            }
        }

        
        Collection<Object> valueColl = null;

        String currentTargetListItemConnector = "";
        String criterionSub = criterion;
        
		for (PropertyMapper mapper : unpackableValues.keySet()) {
			Object value = unpackableValues.get(mapper);
            valueColl = CollectionUtil.toCollection(value);
            if (valueColl == null) {
                throw new QueryTemplateException(
                        "Some parameter of enumerable type expected. parameter: '" + filterPrpsStr + "'");
            }
			int unpackIndex = 0;
			StringBuilder unpackedParameterReferences = new StringBuilder();
			for (Object valueItem : valueColl) {
				unpackedParameterReferences.append(currentTargetListItemConnector);
				unpackedParameterReferences.append(this.config.getParameterUsagePrefix() + mapper.getFilterPrp() + "_" + unpackIndex);
				currentTargetListItemConnector = this.config.getTargetItemListSeparatorMarker();
				unpackIndex++;
			}
        	unpackIndex = 0;
			criterionSub = criterionSub.replace(this.config.getParameterUsagePrefix() + mapper.getFilterPrp(), unpackedParameterReferences.toString());
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
        List<PropertyMapper> repeatableMappers = new ArrayList<>();
        // Checking the existence of some enumerable.
        for (PropertyMapper<Q, ?> mapper : criterionMappers) {
            Object value = PropertyUtils.getProperty(filter, mapper.getFilterPrp());

            // In the case `any` is used one or more properties can be unfilled.
            if (value != null) {
            	if (mapper.isRepeater()) {
            		repeatableMappers.add(mapper);
            		if (firstRepeaterValue == null) {
            			firstRepeaterValue = CollectionUtil.toCollection(value);
            			firstRepeaterMapper = mapper;
            		} else {
            			Collection<Object> currentRepeaterValue = CollectionUtil.toCollection(value);
						if (firstRepeaterValue.size() != currentRepeaterValue.size()) {
							throw new QueryTemplateException(
									"All repeatable parameters must have the same number of values. Parameter '"
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

        if (firstRepeaterValue == null) {
            throw new QueryTemplateException(
                    "Some parameter of enumerable type expected. parameter: '" + filterPrpsStr + "'");
        }

        String tempConnector = "  ";
        int repeatIndex = 0;
        for (Object valueItem : firstRepeaterValue) {
            repeatedCriterion.append(tempConnector);
            String criterionSub = criterion;
            for (PropertyMapper repeatableMapper : repeatableMappers) {
            	criterionSub = criterionSub.replace(this.config.getParameterUsagePrefix() + repeatableMapper.getFilterPrp(),
                    		this.config.getParameterUsagePrefix() + repeatableMapper.getFilterPrp() + "_" + repeatIndex);
            }

            repeatedCriterion.append(criterionSub);

            tempConnector = repeatConnector;
            repeatIndex++;
        }

        return repeatedCriterion.toString();
    }

    /**
     * Assembles a part of the query that depends on the parameter(s) currently
     * being processed. Returns the part along with the connector.
     */
    private String buildParameterCriterion(OutputParam<Integer> index, String currentConnector,
    		OutputParam<Boolean> paramFilled, 
    		boolean anyParamBefore, Object filter, 
            QueryTemplateState<Q> state) {
        StringBuilder queryTextMod = new StringBuilder();
        boolean repeatTokenActive = false;
        String repeatConnector = "  ";
        paramFilled.setValue(false);
        String[] filterPrps = new String[] {};
        String filterPrpsStr = "";

        boolean firstIteration = true;
        while (true) {
            if (!firstIteration) {
                queryTextMod.append(this.queryTextParts[index.getValue()]);
            }

            if (index.getValue() < this.queryTextTokens.size() && this.queryTextTokens.get(index.getValue()) != null) {
                QueryTemplateTokenPojo.TokenType type = this.queryTextTokens.get(index.getValue()).getType();
                if (type == QueryTemplateTokenPojo.TokenType.PARAMETER) {
                    repeatTokenActive = false;
                    filterPrpsStr = this.queryTextTokens.get(index.getValue()).getValue();
                    filterPrps = filterPrpsStr.split(",", -1);
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
                    boolean anyInsteadOfAll = false;

                    if (filterPrps.length == 0) {
                        throw new QueryTemplateException("'" + QueryTemplateTokenPojo.TokenType.PARAMETER
                                + "' not defined. Before: '" + this.queryTextTokens.get(index.getValue()).getValue() + "'");
                    }

                    Pattern rxReservedAnyReplacer = Pattern.compile(this.config.getReservedAnyParam());
                    Pattern rxReservedAnyMatcher = Pattern.compile(".*" + this.config.getReservedAnyParam() + ".*");
                    if (rxReservedAnyMatcher.matcher(filterPrps[0]).matches()) {
                        anyInsteadOfAll = true;
                        filterPrps[0] = rxReservedAnyReplacer.matcher(filterPrps[0]).replaceAll("").trim();
                    }

                    boolean paramsAreFilled = !anyInsteadOfAll;
                    List<PropertyMapper<Q, ?>> criterionMappers = new ArrayList<>();
                    for (String filterPrp : filterPrps) {
                        String filterPrpTrim = filterPrp.trim();
                        boolean negateVerification = false;
                        if (filterPrpTrim.startsWith("!")) {
                            negateVerification = true;
                            filterPrpTrim = filterPrpTrim.replace("!", "").trim();
                        }

                        // Whether the parameter is filled.
                        PropertyMapper mapper = null;
                        if (this.mappersMap.containsKey(filterPrpTrim)) {
                            mapper = this.mappersMap.get(filterPrpTrim);
                        }

                        if (mapper != null) {
                        	criterionMappers.add(mapper);
                            boolean thisParamIsFilled = this.mappersMap.get(filterPrpTrim)
                                    .getFillVerifier().isFilled(filter, filterPrpTrim);
                            // Negating the verification because of the presence of !.
                            if (negateVerification) {
                                thisParamIsFilled = !thisParamIsFilled;
                            }

                            // Transforming the verification from conjunctive (AND's) to
                            // disjunctive (OR's).
                            if (anyInsteadOfAll) {
                                paramsAreFilled = paramsAreFilled || thisParamIsFilled;
                            } else {
                                paramsAreFilled = paramsAreFilled && thisParamIsFilled;
                            }
                        } else {
                            throw new QueryTemplateException("unmapped property listed: " + filterPrpTrim);
                        }
                    }

                    String criterionPrp = "";
                    if (paramsAreFilled) {
                        if (type == QueryTemplateTokenPojo.TokenType.CRITERION) {
                            if (repeatTokenActive) {
                                criterionPrp = this.repeatArrayParamCriterion(filterPrpsStr, filter, criterionMappers,
                                        repeatConnector, this.queryTextTokens.get(index.getValue()).getValue(), state);
                            } else {                                
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
                    				PropertyMapper<Q, ?> mapperItem = this.mappersMap.get(parameterName);
                    				
									if (mapperItem.isRepeater() && mapperItem.getFillVerifier().isFilled(filter, mapperItem.getFilterPrp())) {
										throw new QueryTemplateException("The parameter '" + parameterName
												+ "' is filled and is repeater, so it must be used with the "
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
                        paramFilled.setValue(true);
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
            QueryTemplateState<Q> state) {
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
                    //boolean[] paramFilled = { false };
                	OutputParam<Boolean> paramFilled = new OutputParam<Boolean>(false);
                    queryTextMod.append(this.buildParameterCriterion(index, innerParenthesisConnector,
                            paramFilled, anyParamParenthesis.getValue(), filter, state));
                    anyParamParenthesis.setValue(anyParamParenthesis.getValue() || paramFilled.getValue());
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
            Set<String> usableParameters) {
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
                        this.config.getParamDelimiterToken().matcher(tokenStr).replaceAll(""), start));
            } else if (this.config.getParamToken().matcher(tokenStr).find()) {
                QueryTemplateTokenPojo token = new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.PARAMETER,
                        this.config.getParamDelimiterToken().matcher(tokenStr).replaceAll(""), start);
                tokens.add(token);
                // Storing the parameter in the list of possible parameters.
                String[] filterPrps = token.getValue().split(",", -1);
                for (String filterPrp : filterPrps) {
                    String cleanParam = filterPrp;
                    Pattern rxReservedAnyReplacer = Pattern.compile(this.config.getReservedAnyParam());
                    cleanParam = cleanParam.replace("!", "");
                    cleanParam = rxReservedAnyReplacer.matcher(cleanParam).replaceAll("");
                    cleanParam = cleanParam.trim();

                    usableParameters.add(cleanParam);
                }
            } else if (this.config.getCriterionToken().matcher(tokenStr).find()) {
                tokens.add(new QueryTemplateTokenPojo(QueryTemplateTokenPojo.TokenType.CRITERION,
                        this.config.getCriterionDelimiterToken().matcher(tokenStr).replaceAll(""), start));
            } else {
                throw new QueryTemplateException("Unexpected token: '" + tokenStr + "'");
            }
        }

        return tokens;
    }

    /**
     * Returns the usable parameters of the query, i.e. the parameters that appear in
     * it.
     *
     * @return the usable parameters.
     */
    @Override
	public Set<String> getUsableParameters() {
        return this.usableParameters;
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
