package com.navfirst.postppp.lib.library;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** JNA declaration of the exported functions in libPostppp.so. */
public interface PostpppLibrary extends Library {

    String POSTPPP_NAME = "Postppp";

    /** A synchronized instance is needed because the native solver has process-global state. */
    PostpppLibrary INSTANCE = (PostpppLibrary) Native.synchronizedLibrary(
            Native.load(POSTPPP_NAME, PostpppLibrary.class,
                    Map.of(Library.OPTION_STRING_ENCODING, StandardCharsets.UTF_8.name()))
    );

    /** Reset a structure and apply the native defaults. */
    void initPostpppInput(PostpppInfo input);

    Pointer getPVersion();

    Pointer getUuid();

    Pointer setLicense(String path);

    Pointer getLicense(String path);

    /** JSON entry point: inputJson is a UTF-8 JSON object. */
    Pointer solvePostppp(String inputJson, String license);

    /** Structure entry point matching {@code solvePostpppStruct(PostpppInput *, char *)}. */
    Pointer solvePostpppStruct(PostpppInfo postpppInfo, String license);

    /** Free any string returned by a native exported function. */
    void freeP(Pointer value);
}
