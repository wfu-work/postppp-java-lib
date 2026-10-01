package com.navfirst.postppp.lib.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.navfirst.postppp.lib.domains.PostpppResponse;
import com.navfirst.postppp.lib.domains.PostpppResult;
import com.navfirst.postppp.lib.exceptions.PostpppException;

import java.nio.charset.StandardCharsets;

/** JSON helpers for the postppp-go-lib request and response envelopes. */
public final class JsonUtils {
    private static final int MAX_REQUEST_BYTES = 64 * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private JsonUtils() {
    }

    /** Serialize a JSON-ready object; map keys must use the Go ABI field names. */
    public static String toJson(Object value) {
        try {
            return GSON.toJson(value);
        } catch (IllegalArgumentException e) {
            throw new PostpppException("invalid_request", "无法序列化 PPP 请求", e);
        }
    }

    /** Optional Java input validation, including a 64 MiB SDK limit. */
    public static void validateInput(String json) {
        if (json == null || json.isBlank()) {
            throw new PostpppException(-1, "solvePostppp JSON 为空");
        }
        if (json.indexOf('\0') >= 0) {
            throw new PostpppException(-1, "JSON 不能包含 NUL 字符");
        }
        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (++i >= json.length() || !Character.isLowSurrogate(json.charAt(i))) {
                    throw new PostpppException(-1, "JSON 包含未配对的代理字符");
                }
            } else if (Character.isLowSurrogate(ch)) {
                throw new PostpppException(-1, "JSON 包含未配对的代理字符");
            }
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_REQUEST_BYTES) {
            throw new PostpppException("request_too_large", "JSON 请求超过 64 MiB");
        }
    }

    /** Parse a native response without discarding the original JSON string. */
    public static Response parseResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new PostpppException("invalid_response", "Native PPP response is empty");
        }
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                throw new PostpppException("invalid_response", "Native PPP response must be an object");
            }
            JsonObject object = root.getAsJsonObject();
            JsonElement codeValue = object.get("code");
            if (codeValue == null || !codeValue.isJsonPrimitive() || !codeValue.getAsJsonPrimitive().isNumber()) {
                throw new PostpppException("invalid_response", "Native PPP response is missing numeric code");
            }
            int code = codeValue.getAsBigDecimal().intValueExact();
            JsonElement errorValue = object.get("error");
            String error = null;
            if (errorValue != null && !errorValue.isJsonNull()) {
                if (!errorValue.isJsonPrimitive() || !errorValue.getAsJsonPrimitive().isString()) {
                    throw new PostpppException("invalid_response", "Native PPP error must be a string");
                }
                error = errorValue.getAsString();
            }
            JsonObject result = null;
            JsonElement resultValue = object.get("result");
            if (resultValue != null && !resultValue.isJsonNull()) {
                if (!resultValue.isJsonObject()) {
                    throw new PostpppException("invalid_response", "Native PPP result must be an object");
                }
                result = resultValue.getAsJsonObject();
            }
            if (code == 0 && result == null) {
                throw new PostpppException("invalid_response", "Native PPP success is missing result");
            }
            return new Response(json, code, error, result);
        } catch (PostpppException e) {
            throw e;
        } catch (JsonParseException | IllegalStateException | ArithmeticException | NumberFormatException e) {
            throw new PostpppException("invalid_response", "无法解析 native PPP response", e);
        }
    }

    /** Decode the full response, retaining partial results even when code is nonzero. */
    public static PostpppResponse parsePostpppResponse(String json) {
        Response response = parseResponse(json);
        PostpppResult result = response.result() == null ? null
                : decodeResult(response.result(), PostpppResult.class);
        return new PostpppResponse(response.code(), response.error(), result);
    }

    /** Decode a successful response directly to its typed PPP result. */
    public static PostpppResult parsePostpppResult(String json) {
        return decode(json, PostpppResult.class);
    }

    /** Decode a successful native result, preserving the native numeric error code on failure. */
    public static <T> T decode(String json, Class<T> type) {
        Response response = parseResponse(json);
        if (response.code() != 0) {
            throw new PostpppException(response.code(), response.error() == null ? "PPP 解算失败" : response.error());
        }
        if (response.result() == null) {
            throw new PostpppException("invalid_response", "Native PPP response is missing result");
        }
        return decodeResult(response.result(), type);
    }

    private static <T> T decodeResult(JsonObject result, Class<T> type) {
        try {
            return GSON.fromJson(result, type);
        } catch (JsonParseException | IllegalStateException | ArithmeticException | NumberFormatException e) {
            throw new PostpppException("invalid_response", "无法解析 PPP result", e);
        }
    }

    public record Response(String json, int code, String error, JsonObject result) {
    }
}
