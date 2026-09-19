package io.github.querytemplate;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * List of the most common {@link FillVerifier}s.
 */
public class FillVerifiers {

    private FillVerifiers() {
    }

    /**
     * Filled if: value != null.
     */
    public static final FillVerifier NOTNULL =
        (filter, filterPrp) -> {
            try {
                return PropertyUtils.getProperty(filter, filterPrp) != null;
            } catch (Exception e) {
                throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
            }
        };

    /**
     * Filled if: value != null AND value != 0.
     */
    public static final FillVerifier NUMBER_NOTZERO =
        (filter, filterPrp) -> {
            try {
                Object n = PropertyUtils.getProperty(filter, filterPrp);
                if (n == null) {
                    return false;
                } else {
                    return ((Number) n).longValue() != 0;
                }
            } catch (Exception e) {
                throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
            }
        };

    /**
     * Used when the evaluation is only meant to decide whether to include a piece
     * of Query, but there is no parameter value to be filled.
     */
    public static final FillVerifier FRAGMENT_INCLUSION =
        (filter, filterPrp) -> {
            try {
                FragmentInclusion fragmentInclusion = (FragmentInclusion) PropertyUtils.getProperty(filter, filterPrp);
                return fragmentInclusion == FragmentInclusion.INCLUDE;
            } catch (Exception e) {
                throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
            }
        };

    /**
     * Filled if: value != null AND value.toString().length() != 0.
     */
    public static final FillVerifier STRING_NOTEMPTY =
        (filter, filterPrp) -> {
            try {
                Object o = PropertyUtils.getProperty(filter, filterPrp);
                if (o == null) {
                    return false;
                } else {
                    return o.toString().length() != 0;
                }
            } catch (Exception e) {
                throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
            }
        };

    /**
     * Filled if: value != null AND ((Collection) value).size() &gt; 0.
     */
    public static final FillVerifier COLLECTION_NOTEMPTY =
        (filter, filterPrp) -> {
            try {
                Collection<?> clObj = (Collection<?>) PropertyUtils.getProperty(filter, filterPrp);
                if (clObj == null) {
                    return false;
                } else {
                    return !clObj.isEmpty();
                }
            } catch (Exception e) {
                throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
            }
        };

    /**
     * Filled if: value != null AND java.lang.reflect.Array.getLength(value) &gt; 0.
     */
    public static final FillVerifier ARRAY_NOTEMPTY =
        (filter, filterPrp) -> {
            try {
                Object objs = PropertyUtils.getProperty(filter, filterPrp);
                if (objs == null) {
                    return false;
                } else {
                    return Array.getLength(objs) != 0;
                }
            } catch (Exception e) {
                throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
            }
        };
        
	public static final FillVerifier BOOLEAN_TRUE = (filter,
		filterPrp) -> {
		try {
			Boolean b = (Boolean) PropertyUtils.getProperty(filter, filterPrp);
			return b != null && b;
		} catch (Exception e) {
			throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
		}
	};
}
