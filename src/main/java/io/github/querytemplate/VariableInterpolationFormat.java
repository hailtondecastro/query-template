package io.github.querytemplate;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that provides functionality for variable interpolation in strings. It
 * allows replacing placeholders in the form of {@code ${name}} with
 * corresponding values from a provided map. The interpolation can be performed
 * recursively up to a specified maximum depth.
 */
public class VariableInterpolationFormat {
	private static final Logger LOG = LoggerFactory.getLogger(VariableInterpolationFormat.class);	
	
    /**
     * Default regex for placeholders in the form {@code ${name}}. The variable
     * name is captured in the named group {@code name}.
     * Value: {@code "\\$\\{(?<name>[a-zA-Z0-9_]+)\\}"}.
     */
    public static final String DEFAULT_REGEX = "\\$\\{(?<name>[a-zA-Z0-9_]+)\\}";

    private Pattern placeholderPattern;

	private int maxDepth;

    public VariableInterpolationFormat() {
        this(Pattern.compile(DEFAULT_REGEX), 10);
    }

    public VariableInterpolationFormat(String regex) {
        this(Pattern.compile(regex), 10);
    }
    public VariableInterpolationFormat(Pattern placeholderPattern, int maxDepth) {
        this.maxDepth = maxDepth;
		this.placeholderPattern = Objects.requireNonNull(placeholderPattern, "Pattern can not be null.");
    }

	/**
	 * Interpolates the given template string by replacing placeholders with
	 * corresponding values from the provided map. The interpolation is performed
	 * recursively up to a maximum depth of 10.
	 *
	 * @param template the template string containing placeholders
	 * @param params   a map containing variable names and their corresponding
	 *                 values
	 * @return the interpolated string with placeholders replaced by their values
	 */
	public String interpolate(String template,
		Map<String, Object> params) {
		return inerpolateRecursive(template, params, 0);
	}
    
    private String interpolateNonRecursive(String template, Map<String, Object> params) {
        //List<Object> orderedArgs = new ArrayList<>();
        //Map<String, Integer> nameToIndex = new HashMap<>();

        Matcher matcher = placeholderPattern.matcher(template);
        //StringBuilder indexedTemplate = new StringBuilder();
        StringBuilder resultSB = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group("name");
            //String formatOptions = extractOptions(matcher);

            String variableValue = null;
            if (!params.containsKey(key)) {
            	variableValue = "";
            	LOG.warn("Parameter not found for key: " + key);
            } else {
            	variableValue = params.get(key).toString();            	
            }

//            int index = nameToIndex.computeIfAbsent(key, k -> {
//                orderedArgs.add(params.get(k));
//                return orderedArgs.size() - 1;
//            });
            
            matcher.appendReplacement(resultSB, Matcher.quoteReplacement(variableValue));
//            matcher.appendReplacement(indexedTemplate, 
//                Matcher.quoteReplacement("{" + index + formatOptions + "}"));
        }
        matcher.appendTail(resultSB);

        return resultSB.toString();
    }
    
	private String inerpolateRecursive(String template, Map<String, Object> params, int currentDepth) {
        if (currentDepth > maxDepth) {
            throw new IllegalStateException("Maximum interpolation depth exceeded.");
        }
        String interpolated = interpolateNonRecursive(template, params);
        if (interpolated.equals(template)) {
            return interpolated;
        } else {
            return inerpolateRecursive(interpolated, params, currentDepth + 1);
        }
    }

//    private String extractOptions(Matcher matcher) {
//        try {
//            String opts = matcher.group("options");
//            return opts != null ? opts : "";
//        } catch (IllegalArgumentException e) {
//            // Caso a regex customizada não defina o grupo opcional (?<options>...)
//            return "";
//        }
//    }
}
