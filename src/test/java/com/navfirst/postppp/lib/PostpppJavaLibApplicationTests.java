package com.navfirst.postppp.lib;

import com.navfirst.postppp.lib.domains.PostpppResponse;
import com.navfirst.postppp.lib.library.PostpppInfo;
import com.navfirst.postppp.lib.services.PostpppLibService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 创建：馥溪凝
 * 日期：2026/10/21 11:13
 * 描述：PostpppJavaLibApplicationTests
 */
@Slf4j
@SpringBootTest
@EnabledOnOs({OS.LINUX, OS.MAC})
@EnabledIfSystemProperty(named = "os.arch", matches = "amd64|x86_64|aarch64|arm64")
class PostpppJavaLibApplicationTests {

    @Autowired
    private PostpppLibService postpppLibService;

    @Test
    void getVersion() {
        String version = this.postpppLibService.getVersion();
        System.out.println("算法版本version: " + version);
    }

    @Test
    void getUuid() {
        String uuid = this.postpppLibService.getUuid();
        System.out.println("唯一标识uuid: " + uuid);
    }

    @Test
    void solvePostppp() {
        String invalidLicense = resolveLicensePath().toString();
        PostpppInfo input = PostpppInfo.builder()
                .year(2026).month(7).day(1)
                .bia("./data/WUM0MGXFIN_20261820000_01D_01D_OSB.BIA")
                .atx("./data/igs20_2425_merged.atx")
                .obsFile("./data/GT101820.26o")
                .build();
        input.setNavPaths("./data/BRDC00IGS_R_20261820000_01D_MN.rnx");
        input.setSp3Paths("./data/WUM0MGXFIN_20261820000_01D_05M_ORB.SP3");
        input.setClkPaths("./data/WUM0MGXFIN_20261820000_01D_30S_CLK.CLK");
        input.setStationNameText("GT10");

        String json = this.postpppLibService.solvePostppp(input, invalidLicense);
        log.info("response json: {}", json);

        PostpppResponse responseResult = this.postpppLibService.solvePostpppResponse(input, invalidLicense);
       log.info("responseResult: {}", responseResult);
    }

    private static Path resolveLicensePath() {
        String configured = System.getProperty("postppp.test.license");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath();
        }
        Path local = dataPath("license.lic");
        if (Files.isRegularFile(local)) {
            return local;
        }
        return Path.of(System.getProperty("user.home"), "Downloads", "license.lic").toAbsolutePath();
    }

    private static Path dataPath(String file) {
        return dataPath(Path.of(System.getProperty("postppp.test.data", "data")).toAbsolutePath(), file);
    }

    private static Path dataPath(Path dataDir, String file) {
        return dataDir.resolve(file);
    }

}
