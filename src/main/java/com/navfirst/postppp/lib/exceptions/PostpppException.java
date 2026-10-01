package com.navfirst.postppp.lib.exceptions;

import lombok.Getter;

import java.io.Serial;

/** Exception carrying a stable native PPP error code and optional message. */
@Getter
public class PostpppException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String field;
    private final String id;

    public PostpppException() {
        this("internal_error", "Unknown PPP error");
    }

    public PostpppException(String message) {
        this("internal_error", message);
    }

    public PostpppException(Exception cause) {
        this("internal_error", cause.getMessage(), cause);
    }

    public PostpppException(int code, String message) {
        this(Integer.toString(code), message);
    }

    public PostpppException(String code, String message) {
        this(code, message, null, null, null);
    }

    public PostpppException(String code, String message, Throwable cause) {
        this(code, message, null, null, cause);
    }

    public PostpppException(String code, String message, String field, String id) {
        this(code, message, field, id, null);
    }

    private PostpppException(String code, String message, String field, String id, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.field = field;
        this.id = id;
    }

    public String getMsg() {
        return getMessage();
    }
}
