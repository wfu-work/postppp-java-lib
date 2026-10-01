package com.navfirst.postppp.lib.services.impl;

import com.navfirst.postppp.lib.library.PostpppInfo;
import com.navfirst.postppp.lib.library.PostpppLibrary;
import com.navfirst.postppp.lib.services.PostpppLibService;
import com.sun.jna.Pointer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/** Default synchronous Java service backed by the postppp-go-lib shared library. */
@Service
public class PostpppLibServiceImpl implements PostpppLibService {

    private final PostpppLibrary postpppLibrary = PostpppLibrary.INSTANCE;

    @Override
    public String getVersion() {
        return readAndFree(this.postpppLibrary.getPVersion());
    }

    @Override
    public String startUuid() {
        return readAndFree(this.postpppLibrary.getUuid());
    }

    @Override
    public String setLicense(String path) {
        return readAndFree(this.postpppLibrary.setLicense(path));
    }

    @Override
    public String getLicense(String path) {
        return readAndFree(this.postpppLibrary.getLicense(path));
    }

    @Override
    public String solvePostppp(PostpppInfo postpppInfo, String license) {
        if (postpppInfo == null) {
            throw new IllegalArgumentException("postpppInfo 不能为空");
        }
        return readAndFree(this.postpppLibrary.solvePostpppStruct(postpppInfo, license));
    }

    @Override
    public String solvePostppp(String inputJson, String license) {
        return readAndFree(this.postpppLibrary.solvePostppp(inputJson, license));
    }

    private String readAndFree(Pointer value) {
        if (value == null) {
            return null;
        }
        try {
            return value.getString(0, StandardCharsets.UTF_8.name());
        } finally {
            this.postpppLibrary.freeP(value);
        }
    }
}
