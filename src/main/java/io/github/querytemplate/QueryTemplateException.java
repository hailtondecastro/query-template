package io.github.querytemplate;

/**
 * Standard exception used across the query-template package.
 */
public class QueryTemplateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public QueryTemplateException() {
        super();
    }

    /**
     * @param message the detail message.
     * @param cause   the cause.
     */
    public QueryTemplateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message the detail message.
     */
    public QueryTemplateException(String message) {
        super(message);
    }
}
