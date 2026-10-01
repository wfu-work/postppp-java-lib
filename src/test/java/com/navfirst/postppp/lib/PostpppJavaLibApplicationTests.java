package com.navfirst.postppp.lib;

import com.navfirst.postppp.lib.domains.PostpppResponse;
import com.navfirst.postppp.lib.domains.PostpppResult;
import com.navfirst.postppp.lib.library.PostpppInfo;
import com.navfirst.postppp.lib.services.PostpppLibService;
import com.navfirst.postppp.lib.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Native smoke test; the Go shared library currently supports Linux amd64. */
@SpringBootTest
@EnabledOnOs(OS.LINUX)
@EnabledIfSystemProperty(named = "os.arch", matches = "amd64|x86_64")
class PostpppJavaLibApplicationTests {

    @Autowired
    private PostpppLibService postpppLibService;

    @Test
    void getVersion() {
        String version = this.postpppLibService.getVersion();
        System.out.println("版本号："+ version);
    }

    @Test
    void javaDefaultsMatchNativeInitializer() {
        PostpppInfo input = PostpppInfo.builder()
                .year(2026).month(7).day(1)
                .bia("/data/bias.BIA")
                .atx("/data/igs20.atx")
                .obsFile("/data/ABCD1820.26o")
                .build();
        input.setNavPaths("/data/BRDC.rnx");
        input.setSp3Paths("/data/orbit.SP3");
        input.setClkPaths("/data/clock.CLK");
        input.setStationNameText("ABCD");

        String json = this.postpppLibService.solvePostppp(input, "/data/license.lic");
        JsonUtils.Response response = JsonUtils.parseResponse(json);
        if (response.code() != 0) {
            System.err.println(response.error());
        }
        if (response.result() != null) {
            System.out.println(response.result());
        }

        PostpppResponse responseResult = this.postpppLibService.solvePostpppResponse(input, "/data/license.lic");
        if (!responseResult.isSuccess()) {
            System.err.printf("PPP failed (%d): %s%n", responseResult.getCode(), responseResult.getError());
        }
        PostpppResult result = responseResult.getResult();
        if (result != null) {
            System.out.println(result.getName());
            System.out.println(result.getFinalXyz()[0]);
            System.out.println(result.getEpoch().size());
        }
    }

}
