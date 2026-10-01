package com.navfirst.postppp.lib;

import com.navfirst.postppp.lib.domains.PostpppResponse;
import com.navfirst.postppp.lib.library.PostpppInfo;
import com.navfirst.postppp.lib.library.PostpppLibrary;
import com.navfirst.postppp.lib.exceptions.PostpppException;
import com.navfirst.postppp.lib.services.PostpppLibService;
import com.navfirst.postppp.lib.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(version).isEqualTo("1.0");
    }

    @Test
    void unavailableLicenseReturnsErrorForRawAndTypedResponses() {
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
        assertThat(response.code()).isEqualTo(-1);
        assertThat(response.error()).contains("许可证");

        PostpppResponse responseResult = this.postpppLibService.solvePostpppResponse(input, "/data/license.lic");
        assertThat(responseResult.isSuccess()).isFalse();
        assertThat(responseResult.getCode()).isEqualTo(-1);
        assertThat(responseResult.getError()).contains("许可证");
        assertThat(responseResult.getResult()).isNull();
    }

    @Test
    void javaDefaultsMatchNativeInitializer() {
        PostpppInfo input = PostpppInfo.builder().year(2026).navsys(1).maxSigma(0).build();
        input.setNavPaths("/data/nav.rnx");
        input.setStationNameText("ABCD");
        PostpppLibrary.INSTANCE.initPostpppInput(input);

        PostpppInfo expected = PostpppInfo.defaults();
        expected.write();
        assertThat(input.getPointer().getByteArray(0, input.size()))
                .containsExactly(expected.getPointer().getByteArray(0, expected.size()));
    }

    @Test
    void jsonEntryPointReturnsGoErrorEnvelope() {
        PostpppResponse response = this.postpppLibService.solvePostpppResponse("{}", null);
        assertThat(response.getCode()).isEqualTo(-1);
        assertThat(response.getError()).contains("观测文件不能为空");
        assertThat(response.hasResult()).isFalse();
    }

    @Test
    void structureEntryPointReturnsGoErrorEnvelope() {
        PostpppResponse response = this.postpppLibService.solvePostpppResponse(PostpppInfo.defaults(), null);
        assertThat(response.getCode()).isEqualTo(-1);
        assertThat(response.getError()).contains("观测文件不能为空");
        assertThat(response.hasResult()).isFalse();
    }

    @Test
    void resultFacadePreservesNativeErrorCode() {
        assertThatThrownBy(() -> this.postpppLibService.solvePostpppResult("{}", null))
                .isInstanceOfSatisfying(PostpppException.class,
                        error -> assertThat(error.getCode()).isEqualTo("-1"))
                .hasMessageContaining("观测文件不能为空");
    }

}
