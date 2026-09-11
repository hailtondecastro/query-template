package io.github.querytemplate;

/**
 * Token transfer object used internally by {@link QueryTemplateDefault}.
 */
class QueryTemplateTokenPojo {

    /**
     * Token types.
     */
    public enum TokenType {
        FILTERS,
        WHERE,
        AND,
        OR,
        NO_OPERATOR,
        OPEN_PARENTHESIS,
        CLOSE_PARENTHESIS,
        EXTRA,
        PARAMETER,
        REPEAT,
        CRITERION,
        QUERY_HELPER
    }

    private TokenType type;
    private String value;
    private int indexInString;

    public QueryTemplateTokenPojo(TokenType type, String value, int indexInString) {
        this.type = type;
        this.value = value;
        this.indexInString = indexInString;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getIndexInString() {
        return indexInString;
    }

    public void setIndexInString(int indexInString) {
        this.indexInString = indexInString;
    }
}
