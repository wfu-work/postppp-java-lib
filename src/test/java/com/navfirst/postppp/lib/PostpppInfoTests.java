package com.navfirst.postppp.lib;

import com.navfirst.postppp.lib.domains.PostpppResponse;
import com.navfirst.postppp.lib.domains.PostpppResult;
import com.navfirst.postppp.lib.library.PostpppInfo;
import com.navfirst.postppp.lib.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostpppInfoTests {

    @Test
    void postpppInfoMatchesNativeLayoutAndDefaults() {
        PostpppInfo input = PostpppInfo.defaults();

        assertThat(input.size()).isEqualTo(392);
        assertThat(input.navsys).isEqualTo(13);
        assertThat(input.elevMask).isEqualTo(7.0);
        assertThat(input.soltype).isEqualTo(2);
        assertThat(input.tropopt).isEqualTo(4);
        assertThat(input.bdsfreq).isEqualTo(4);
        assertThat(input.tidecorr).isEqualTo(1);
        assertThat(input.maxSigma).isEqualTo(0.020);
        assertThat(input.troInterval).isEqualTo(300.0);

        input.setNavPaths("/data/nav.rnx");
        input.setStationNameText("ABCD");
        input.write();
        assertThat(input.nav[0].getString(0, "UTF-8")).isEqualTo("/data/nav.rnx");
        assertThat(input.getStationNameText()).isEqualTo("ABCD");
    }

    @Test
    void fixedWidthAndPathInputsRejectNativeStringViolations() {
        PostpppInfo input = PostpppInfo.defaults();

        assertThatThrownBy(() -> input.setStationNameText("1234567890123456"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> input.setNavPaths("a\0b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> input.setNavPaths("1", "2", "3", "4", "5"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builderRetainsUtf8PathsAndFixedArrayLengths() {
        PostpppInfo input = PostpppInfo.builder().year(2026).build();
        input.setNavPaths("/data/广播星历.rnx");
        input.setStationNameText("测站");
        input.write();
        assertThat(input.nav[0].getString(0, "UTF-8")).isEqualTo("/data/广播星历.rnx");
        assertThat(input.getStationNameText()).isEqualTo("测站");

        input.enuOffset = new double[2];
        assertThatThrownBy(input::write).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void responseEnvelopePreservesCodeErrorAndResult() {
        JsonUtils.Response response = JsonUtils.parseResponse(
                "{\"code\":0,\"result\":{\"name\":\"ABCD\"}}");

        assertThat(response.code()).isZero();
        assertThat(response.error()).isNull();
        assertThat(response.result().get("name").getAsString()).isEqualTo("ABCD");
    }

    @Test
    void responseJsonIsDecodedIntoTypedResultObjects() {
        String json = """
                {
                  "code": 0,
                  "result": {
                    "name": "ABCD",
                    "year": 2026,
                    "doy": 182,
                    "epoch": [{
                      "time": {"week": 2400, "tow": 100.5},
                      "xyz": [1, 2, 3],
                      "sigma": [0.1, 0.2, 0.3],
                      "sigma3d": 0.374,
                      "enu": [4, 5, 6],
                      "ztd": 2.3,
                      "zhd": 2.1,
                      "zwd": 0.2,
                      "gn": 0.01,
                      "ge": 0.02,
                      "sd_ztd": 0.03,
                      "sd_gn": 0.04,
                      "sd_ge": 0.05,
                      "nsat": 12,
                      "stat": 4,
                      "accepted": true
                    }],
                    "have_coord": true,
                    "n_coord": 1,
                    "first": {"week": 2400, "tow": 100.5},
                    "last": {"week": 2400, "tow": 100.5},
                    "final_xyz": [1, 2, 3],
                    "final_sigma": [0.1, 0.2, 0.3],
                    "final_llh": [30, 120, 50],
                    "mean_xyz": [1.1, 2.1, 3.1],
                    "rms_enu": [0.01, 0.02, 0.03],
                    "bias_enu": [0.1, 0.2, 0.3],
                    "bias_enu_mean": [0.4, 0.5, 0.6],
                    "ztd_mean": 2.3,
                    "n_epoch": 1,
                    "n_ppp": 1,
                    "n_accept": 1,
                    "n_gated": 0,
                    "n_spp": 0,
                    "n_none": 0,
                    "avail": 100,
                    "mean_nsat": 12,
                    "n_gap": 0,
                    "max_gap_sec": 0,
                    "max_gap_start": {"week": 2400, "tow": 0},
                    "converge_sec": 30,
                    "grade": "A",
                    "software": "postppp-go-lib",
                    "agency": "PPP",
                    "products": "igs",
                    "pos_file": "/tmp/ABCD.pos",
                    "tro_file": "/tmp/ABCD.tro"
                  }
                }
                """;

        PostpppResponse response = JsonUtils.parsePostpppResponse(json);
        PostpppResult result = response.getResult();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.hasResult()).isTrue();
        assertThat(result.getName()).isEqualTo("ABCD");
        assertThat(result.getFinalXyz()).containsExactly(1.0, 2.0, 3.0);
        assertThat(result.getEpoch()).hasSize(1);
        assertThat(result.getEpoch().get(0).getTime().getWeek()).isEqualTo(2400);
        assertThat(result.getEpoch().get(0).isAccepted()).isTrue();
        assertThat(result.getEpoch().get(0).getStat()).isEqualTo(4);
        assertThat(result.getMaxGapStart().getTow()).isZero();
        assertThat(JsonUtils.parsePostpppResult(json).getGrade()).isEqualTo("A");
    }

    @Test
    void partialResultIsRetainedForNoSolutionResponse() {
        String json = "{\"code\":-6,\"error\":\"无合格 PPP 解\","
                + "\"result\":{\"name\":\"ABCD\",\"n_epoch\":10}}";

        PostpppResponse response = JsonUtils.parsePostpppResponse(json);

        assertThat(response.getCode()).isEqualTo(-6);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isPartialResult()).isTrue();
        assertThat(response.getResult().getName()).isEqualTo("ABCD");
        assertThat(response.getResult().getNEpoch()).isEqualTo(10);
        assertThatThrownBy(() -> JsonUtils.parsePostpppResult(json))
                .isInstanceOf(com.navfirst.postppp.lib.exceptions.PostpppException.class)
                .hasMessageContaining("无合格 PPP 解");
    }
}
