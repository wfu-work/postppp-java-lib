package com.navfirst.postppp.lib.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Go PPP 接口返回的响应封装。
 *
 * <p>{@code code == 0} 表示解算成功；部分失败（尤其是 {@code -6}）仍可能带有
 * 可用于质量诊断的 {@link #result}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostpppResponse {

    /** 原生返回码，0 表示成功，非 0 表示参数、文件或解算错误。 */
    private int code;

    /** 原生错误描述；成功时通常为 {@code null}。 */
    private String error;

    /** PPP 结果；成功时必有，部分失败时可能存在。 */
    private PostpppResult result;

    public boolean isSuccess() {
        return code == 0;
    }

    public boolean hasResult() {
        return result != null;
    }

    /** {@code code == -6} 且结果对象存在时，表示这是一个部分结果。 */
    public boolean isPartialResult() {
        return code == -6 && result != null;
    }
}
