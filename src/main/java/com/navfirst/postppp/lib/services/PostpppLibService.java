package com.navfirst.postppp.lib.services;

import com.navfirst.postppp.lib.library.PostpppInfo;
import com.navfirst.postppp.lib.domains.PostpppResponse;
import com.navfirst.postppp.lib.domains.PostpppResult;
import com.navfirst.postppp.lib.utils.JsonUtils;

/** High-level synchronous facade for the postppp-go-lib C ABI. */
public interface PostpppLibService {

    String getVersion();

    String startUuid();

    /** Validate and cache a license in the native process. */
    String setLicense(String path);

    /** Read license metadata without changing the native cache. */
    String getLicense(String path);

    /** Solve one station/day using the JNA {@link PostpppInfo} structure. */
    String solvePostppp(PostpppInfo postpppInfo, String license);

    /** Solve one station/day using the native JSON entry point. */
    String solvePostppp(String inputJson, String license);

    /** Return the typed response envelope, including error codes and partial diagnostic results. */
    default PostpppResponse solvePostpppResponse(PostpppInfo postpppInfo, String license) {
        return JsonUtils.parsePostpppResponse(solvePostppp(postpppInfo, license));
    }

    /** Return the typed response envelope for a JSON request. */
    default PostpppResponse solvePostpppResponse(String inputJson, String license) {
        return JsonUtils.parsePostpppResponse(solvePostppp(inputJson, license));
    }

    /** Return a successful result; native failures throw PostpppException. */
    default PostpppResult solvePostpppResult(PostpppInfo postpppInfo, String license) {
        return JsonUtils.decode(solvePostppp(postpppInfo, license), PostpppResult.class);
    }

    /** Return a successful result for a JSON request; native failures throw PostpppException. */
    default PostpppResult solvePostpppResult(String inputJson, String license) {
        return JsonUtils.decode(solvePostppp(inputJson, license), PostpppResult.class);
    }
}
