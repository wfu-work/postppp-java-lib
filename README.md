# postppp-java-lib

通过 JNA 封装 [`postppp-go-lib`](../../nav-app/postppp-go-lib) 的 Java SDK，一次同步调用完成一个测站一天的静态精密单点定位（PPP）解算。提供 `PostpppInfo` 结构体和原始 JSON 两种入口，返回逐历元坐标、对流层延迟、日解坐标和质量统计。

当前原生解算支持 **Linux amd64、Linux arm64 和 macOS arm64**。JAR 按平台携带 `linux-x86-64/libPostppp.so`、`linux-aarch64/libPostppp.so` 和 `darwin-aarch64/libPostppp.dylib`。完整解算需要有效许可证及调用方提供的观测、广播星历、精密轨道、钟差、码偏差和天线文件；库不下载数据。

## 安装与构建

编译目标为 Java 20，推荐 JDK 21 和 Maven 3.6.3+：

```bash
mvn clean verify
mvn install

# 使用项目根目录 data/ 中的真实数据；许可证不提交到仓库
mvn -Dpostppp.test.license=/path/to/license.lic test
```

产物为普通依赖 JAR：`target/postppp-java-lib-1.0.0.jar`，不进行 Spring Boot 可执行 JAR 重打包。

```xml
<dependency>
    <groupId>com.navfirst</groupId>
    <artifactId>postppp-java-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

运行依赖 JNA 和 Gson；Spring Boot 支持为可选依赖，使用方无需安装 Lombok。普通 Java 程序可以直接创建 `PostpppLibServiceImpl`。版本接口返回原生 PPP SDK 版本（当前为 `1.0`），与 Java SDK 版本分别管理。

## 结构体解算

Java 服务的 `solvePostppp(PostpppInfo, String)` 调用 Go 导出的 `solvePostpppStruct(PostpppInput *, char *)`。以下示例需替换为 Linux 主机或容器可访问的实际路径：

```java
import com.navfirst.postppp.lib.library.PostpppInfo;
import com.navfirst.postppp.lib.services.PostpppLibService;
import com.navfirst.postppp.lib.services.impl.PostpppLibServiceImpl;
import com.navfirst.postppp.lib.utils.JsonUtils;

public class Example {
    public static void main(String[] args) {
        PostpppLibService sdk = new PostpppLibServiceImpl();
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

        String json = sdk.solvePostppp(input, "/data/license.lic");
        JsonUtils.Response response = JsonUtils.parseResponse(json);
        System.out.println(sdk.getVersion());
        if (response.code() != 0) {
            System.err.println(response.error());
        }
        if (response.result() != null) {
            System.out.println(response.result()); // -6 也可能带有诊断结果
        }
    }
}
```

`new PostpppInfo()`、`PostpppInfo.defaults()` 和 `PostpppInfo.builder().build()` 均带有 Go 结构体初始化器的处理默认值，无需额外调用原生初始化。手动调用 `PostpppLibrary.initPostpppInput` 会清空已填写的路径和测站信息，应在填写之前调用。

## PostpppInfo 字段映射

映射以 Go `main.go` 中的 `PostpppInput` 为准，而不是底层 `static/postppp.h` 中的 `postppp_opt_t`。在当前 64 位 ABI 下大小为 **392 字节**，字段顺序和数组长度必须保持一致。所有 C `int32_t` 映射为 Java `int`，`double` 映射为 Java `double`；以下数组表示固定长度，不能替换成不同长度的数组。

| Java 字段 | C / JSON 字段名 | Java 类型 | 默认值与含义 |
| --- | --- | --- | --- |
| `year`, `month`, `day` | `year`, `month`, `day` | `int` | 全 0 从观测首历元确定 GPST 日期；否则同时指定有效年月日，年范围 1970～2099 |
| `interval` | `interval` | `double` | 0 自动推断采样间隔（秒），不能为负 |
| `navsys` | `navsys` | `int` | 13，即 GPS+GAL+BDS；GPS=1、GLO=2、GAL=4、BDS=8、QZS=32，按位或组合；SDK 不建议启用 GLO |
| `elevMask` | `elev_mask` | `double` | 7°，范围 `[0,90)` |
| `soltype` | `soltype` | `int` | 2 双向组合；0 前向、1 反向 |
| `tropopt` | `tropopt` | `int` | 4 ZTD+梯度；3 只估计 ZTD |
| `bdsfreq` | `bdsfreq` | `int` | 4 B1I+B3I；5 B1C+B2A |
| `tidecorr` | `tidecorr` | `int` | 1 固体潮；0 关闭、2 全潮汐改正（需 ERP 与 BLQ） |
| `tropPrn` | `trop_prn` | `double` | 0 使用内核默认过程噪声，不能为负 |
| `maxSigma` | `max_sigma` | `double` | 0.020 米，3D 位置中误差门限；0 关闭门限 |
| `enuOffset` | `enu_offset` | `double[3]` | 全 0，E/N/U 平移（米）；仅影响 ENU 相关成果，XYZ 保持解算原值 |
| `outDir` | `out_dir` | `String` | `null`；空值时不写文件 |
| `writePos` | `write_pos` | `int` | 0；非 0 写逐历元 `.pos` |
| `writeTro` | `write_tro` | `int` | 0；非 0 写 SINEX_TRO |
| `writeQc` | `write_qc` | `int` | 0；非 0 追加质量报告 |
| `troInterval` | `tro_interval` | `double` | 300 秒；0 也回退到 300，负值无效 |
| `software`, `agency` | `software`, `agency` | `String` | `null`；转换时回退到 `postppp-go-lib` 和 `PPP` |
| `verbose` | `verbose` | `int` | 0；C ABI 无日志回调，单独开启此字段不会输出日志 |
| `nav` | `nav` | `Pointer[4]` | 广播星历路径，至少一项；按顺序读到一个成功即止 |
| `sp3`, `clk` | `sp3`, `clk` | `Pointer[4]` | 精密轨道、钟差各至少一项，多个文件累加读取，可给跨日产品 |
| `bia`, `atx` | `bia`, `atx` | `String` | 必需的 OSB/BIA 码偏差与 ANTEX 天线文件路径 |
| `erp`, `blq` | `erp`, `blq` | `String` | 可选；`tidecorr=2` 时都必须提供 |
| `ionex` | `ionex` | `String` | 可选电离层格网；无电离层组合不依赖它 |
| `obsFile` | `obs_file` | `String` | 必需的 RINEX 或 RTCM3 观测文件路径 |
| `stationName` | `station_name` | `byte[16]` | 全 0；最多 15 个 UTF-8 字节加 NUL |
| `approxXyz` | `approx_xyz` | `double[3]` | 全 0；ECEF 先验坐标（米），也是 ENU 参考基准；全零则使用观测文件头坐标 |
| `antType` | `ant_type` | `byte[24]` | 全 0；最多 23 个 UTF-8 字节加 NUL |
| `radome` | `radome` | `byte[8]` | 全 0；最多 7 个 UTF-8 字节加 NUL |
| `antDelta` | `ant_delta` | `double[3]` | 全 0；天线 E/N/U 偏心（米） |

`nav/sp3/clk` 对应 C 的 `const char *[4]`。JNA 不支持在结构体中直接写入 `String[]`，因此使用 `Pointer[4]` 并通过 `setNavPaths`、`setSp3Paths`、`setClkPaths` 分配 UTF-8、NUL 结尾字符串。对象保留相应 `Memory` 引用，空项映射为 `NULL`，每类最多 4 项。也可使用 `setNav(String...)` 等别名。

定长 C `char[]` 必须映射为 `byte[]`，不能使用 Java `char[]`（一个 Java 字符占两个字节）。通过 `setStationNameText`、`setAntTypeText`、`setRadomeText` 或相应的字符串 setter 填写，并用 `getStationNameText` 等读取。超出 UTF-8 字节容量或包含 NUL 会抛出 `IllegalArgumentException`。

观测文件头中的测站、天线等信息会覆盖对应输入字段；这些输入也用于文件头信息缺失时兜底。所有数值必须是有限数。需要落盘时同时设置 `outDir` 和输出开关，例如 `input.outDir="/data/result"; input.writePos=1; input.writeTro=1;`。

## JSON 入口

`solvePostppp(String, String)` 调用 Go 的 `solvePostppp`，使用表中的 C / JSON 字段名。未提供的处理参数采用 Go 默认值；`nav/sp3/clk` 为最多 4 项的字符串数组，开关支持 JSON 布尔值或整数 0/1。

```java
String request = """
    {"year":2026,"month":7,"day":1,
     "nav":["/data/BRDC.rnx"],
     "sp3":["/data/orbit.SP3"],
     "clk":["/data/clock.CLK"],
     "bia":"/data/bias.BIA",
     "atx":"/data/igs20.atx",
     "obs_file":"/data/ABCD1820.26o",
     "station_name":"ABCD"}
    """;
String response = sdk.solvePostppp(request, "/data/license.lic");
```

`PostpppInfo` 是 native 内存映射，不应直接交给 Gson 序列化。组装 JSON 时可以给 `JsonUtils.toJson` 传入使用协议字段名的 `Map`；该工具不会自动把 Java 驼峰字段转换为 Go 字段名。

## 将返回 JSON 封装为 Java 对象

如果业务代码需要直接读取解算结果，可以使用 typed API。`solvePostpppResponse` 返回包含状态码、错误信息和可选结果的 `PostpppResponse`；成功时再通过 `getResult()` 取得 `PostpppResult`：

```java
import com.navfirst.postppp.lib.domains.PostpppResponse;
import com.navfirst.postppp.lib.domains.PostpppResult;

PostpppResponse response = sdk.solvePostpppResponse(input, "/data/license.lic");
if (!response.isSuccess()) {
    System.err.printf("PPP failed (%d): %s%n", response.getCode(), response.getError());
}

PostpppResult result = response.getResult();
if (result != null) {
    System.out.println(result.getName());
    System.out.println(result.getFinalXyz()[0]);
    System.out.println(result.getEpoch().size());
}
```

也可以直接调用 `solvePostpppResult(...)` 获取成功结果。该方法在 native 返回非零 `code` 时抛出 `PostpppException`，适合只处理成功分支的代码：

```java
PostpppResult result = sdk.solvePostpppResult(input, "/data/license.lic");
```

JSON 入口也提供同名重载：`solvePostpppResponse(String, String)` 和 `solvePostpppResult(String, String)`。`PostpppResponse` 对应外层 `code/error/result`，`PostpppResult` 完整对应 Go 的 `domains.Result`，其中 `epoch` 是 `List<PostpppEpoch>`，`time/first/last/max_gap_start` 是 `PostpppTime`。数组字段使用 Java `double[]`，例如 `final_xyz`、`final_llh` 和 `rms_enu`。Java getter 使用驼峰命名（如 `getFinalXyz()`），JSON 字段仍保持 Go 的下划线命名。

当 `code=-6`（没有合格 PPP 解）时，native 结果可能同时带有部分 `result`。此时 `solvePostpppResponse` 会保留该结果，便于诊断；`solvePostpppResult` 仍按错误处理并抛出异常。需要从已有 JSON 手动转换时，可使用 `JsonUtils.parsePostpppResponse(json)` 获取完整响应，或使用 `JsonUtils.parsePostpppResult(json)` / `JsonUtils.decode(json, PostpppResult.class)` 获取成功结果。

## 返回值、字段说明与质量信息

两种解算入口原样返回 JSON：成功为 `{"code":0,"result":{...}}`，失败包含 `code` 与字符串 `error`。参数或许可证错误为 -1，内存不足为 -2，导航、精密产品、观测文件、无合格 PPP 解、输出错误依次为 -3～-7。**-6 仍可能包含部分结果**，应保留以供诊断。

`JsonUtils.parseResponse` 保留原始 JSON、错误码、错误文本和原始 `result`；`JsonUtils.parsePostpppResponse` 将其转换为 `PostpppResponse`；`JsonUtils.decode(json, type)` 仅解码成功结果，失败抛出 `PostpppException`。

### 响应封装字段

| JSON 字段 | Java 属性 / getter | 类型 | 说明 |
| --- | --- | --- | --- |
| `code` | `code` / `getCode()` | `int` | 原生返回码，`0` 表示成功；非零值表示参数、文件或解算错误。 |
| `error` | `error` / `getError()` | `String` | 错误描述；成功时通常为空。 |
| `result` | `result` / `getResult()` | `PostpppResult` | PPP 结果；成功时存在，`code=-6` 时也可能包含部分结果。 |

### 时间字段：`PostpppTime`

| JSON 字段 | Java 属性 / getter | 类型 | 说明 |
| --- | --- | --- | --- |
| `week` | `week` / `getWeek()` | `int` | GPS 周。 |
| `tow` | `tow` / `getTow()` | `double` | 周内秒（Time Of Week），单位：秒。 |

### 逐历元字段：`PostpppEpoch`

| JSON 字段 | Java 属性 / getter | 类型 | 说明 |
| --- | --- | --- | --- |
| `time` | `time` / `getTime()` | `PostpppTime` | 历元 GPST 时间。 |
| `xyz` | `xyz` / `getXyz()` | `double[3]` | ECEF 坐标，顺序 X/Y/Z，单位：米。 |
| `sigma` | `sigma` / `getSigma()` | `double[3]` | ECEF 坐标标准差，顺序 X/Y/Z，单位：米。 |
| `sigma3d` | `sigma3d` / `getSigma3d()` | `double` | ECEF 三维位置标准差，单位：米。 |
| `enu` | `enu` / `getEnu()` | `double[3]` | 相对参考坐标的偏差，顺序东/北/天，单位：米。 |
| `ztd` | `ztd` / `getZtd()` | `double` | 总对流层延迟，单位：米。 |
| `zhd` | `zhd` / `getZhd()` | `double` | 干对流层延迟，单位：米。 |
| `zwd` | `zwd` / `getZwd()` | `double` | 湿对流层延迟，单位：米。 |
| `gn` / `ge` | `gn` / `getGn()`、`ge` / `getGe()` | `double` | 北向、东向对流层梯度，单位：米。 |
| `sd_ztd` | `sdZtd` / `getSdZtd()` | `double` | ZTD 标准差，单位：米。 |
| `sd_gn` / `sd_ge` | `sdGn` / `getSdGn()`、`sdGe` / `getSdGe()` | `double` | 北向、东向梯度标准差，单位：米。 |
| `nsat` | `nsat` / `getNsat()` | `int` | 参与该历元 PPP 解算的卫星数。 |
| `stat` | `stat` / `getStat()` | `int` | 历元解算状态：`0` 无解、`3` SPP、`4` PPP 浮点解、`6` PPP 固定解。 |
| `accepted` | `accepted` / `isAccepted()` | `boolean` | 是否通过质量门限并计入最终统计。 |

### 测站日结果字段：`PostpppResult`

| JSON 字段 | Java 属性 / getter | 类型 | 说明 |
| --- | --- | --- | --- |
| `name` | `name` / `getName()` | `String` | 测站名称。 |
| `year` / `doy` | `year` / `getYear()`、`doy` / `getDoy()` | `int` | 解算年份和年积日（1～366）。 |
| `epoch` | `epoch` / `getEpoch()` | `List<PostpppEpoch>` | 按 GPST 排序的逐历元结果，拒绝历元也会保留。 |
| `have_coord` | `haveCoord` / `isHaveCoord()` | `boolean` | 是否存在合格坐标；为 `false` 时不要解释坐标数组中的零值。 |
| `n_coord` | `nCoord` / `getNCoord()` | `int` | 参与坐标汇总的合格历元数。 |
| `first` / `last` | `first` / `getFirst()`、`last` / `getLast()` | `PostpppTime` | 有效成果的起始和结束时间。 |
| `final_xyz` | `finalXyz` / `getFinalXyz()` | `double[3]` | 最后一个合格历元的 ECEF 坐标，顺序 X/Y/Z，单位：米。 |
| `final_sigma` | `finalSigma` / `getFinalSigma()` | `double[3]` | 最后一个合格历元的 ECEF 标准差，顺序 X/Y/Z，单位：米。 |
| `final_llh` | `finalLlh` / `getFinalLlh()` | `double[3]` | 大地坐标，顺序纬度（度）/经度（度）/大地高（米）。 |
| `mean_xyz` | `meanXyz` / `getMeanXyz()` | `double[3]` | 合格历元 ECEF 坐标平均值，顺序 X/Y/Z，单位：米。 |
| `rms_enu` | `rmsEnu` / `getRmsEnu()` | `double[3]` | 相对合格历元均值的 ENU 重复性 RMS，顺序东/北/天，单位：米。 |
| `bias_enu` | `biasEnu` / `getBiasEnu()` | `double[3]` | 最后合格历元相对先验坐标的 ENU 偏差，已包含 ENU 偏移，顺序东/北/天，单位：米。 |
| `bias_enu_mean` | `biasEnuMean` / `getBiasEnuMean()` | `double[3]` | 全天平均 ENU 偏差，可能包含收敛期结果，顺序东/北/天，单位：米。 |
| `ztd_mean` | `ztdMean` / `getZtdMean()` | `double` | 全天合格成果的平均总对流层延迟，单位：米。 |
| `n_epoch` | `nEpoch` / `getNEpoch()` | `int` | 观测总历元数。 |
| `n_ppp` | `nPpp` / `getNPpp()` | `int` | 使用 PPP 状态完成解算的历元数。 |
| `n_accept` | `nAccept` / `getNAccept()` | `int` | 通过质量门限的历元数。 |
| `n_gated` | `nGated` / `getNGated()` | `int` | 因质量门限被剔除的历元数。 |
| `n_spp` | `nSpp` / `getNSpp()` | `int` | 仅得到 SPP、未得到 PPP 的历元数。 |
| `n_none` | `nNone` / `getNNone()` | `int` | 没有可用定位结果的历元数。 |
| `avail` | `avail` / `getAvail()` | `double` | 数据可用率，单位：百分比。 |
| `mean_nsat` | `meanNsat` / `getMeanNsat()` | `double` | 平均可用卫星数。 |
| `n_gap` | `nGap` / `getNGap()` | `int` | 观测中断窗口数，不含首次收敛期。 |
| `max_gap_sec` | `maxGapSec` / `getMaxGapSec()` | `double` | 最大观测中断时长，单位：秒。 |
| `max_gap_start` | `maxGapStart` / `getMaxGapStart()` | `PostpppTime` | 最大观测中断的开始时间。 |
| `converge_sec` | `convergeSec` / `getConvergeSec()` | `double` | 首次达到收敛条件的耗时，单位：秒。 |
| `grade` | `grade` / `getGrade()` | `String` | 综合质量等级，通常为“可靠”“基本可靠”或“不可靠”。 |
| `software` / `agency` | `software` / `getSoftware()`、`agency` / `getAgency()` | `String` | 输出结果中记录的软件和机构名称。 |
| `products` | `products` / `getProducts()` | `String` | 使用的精密产品标识。 |
| `pos_file` | `posFile` / `getPosFile()` | `String` | 逐历元 POS 文件实际路径；未输出时为空。 |
| `tro_file` | `troFile` / `getTroFile()` | `String` | SINEX_TRO 文件实际路径；未输出时为空。 |

未通过质量门限的历元仍保留在 `epoch` 中，其 `accepted=false`。完整结果字段与单位见 Go 项目的 [readme.md](../../nav-app/postppp-go-lib/readme.md)。

## 许可证、内存与线程

```java
String uuid = sdk.startUuid();
String licenseInfo = sdk.setLicense("/data/license.lic"); // 校验并缓存
String metadata = sdk.getLicense("/data/license.lic");    // 只读，不设置缓存
```

`setLicense` 成功返回许可证 JSON，失败返回空字符串；`getLicense` 成功返回许可证 JSON，失败可能返回普通错误文本，不能按解算响应解析。许可证中只有 `active=true` 表示有效。

解算时传非空许可证路径会校验该文件；传 `null` 或空字符串则使用先前成功设置的进程缓存，每次解算仍检查有效期与 UUID。缓存仅在同一进程有效。

服务自动将所有 native 返回的 `char *` 复制为 UTF-8 Java 字符串，并在 `finally` 中调用一次 `freeP`。直接使用低层 `PostpppLibrary` 时，返回类型为 `Pointer`，调用方负责读取和释放，不能重复释放。输入结构体与输入路径不应使用 `freeP` 释放。

服务同步执行；同一 JVM 内调用通过同步 JNA 实例串行执行，Go 内核也有进程级锁。调用期间不要修改输入对象及数组。多站并行处理应使用不同进程；Go 会话 API 的产品复用接口当前未由 C ABI 导出。

## Spring Boot 与动态库

Spring Boot 3 可自动注入 `PostpppLibService`。配置 `ppp.enabled=false` 关闭默认自动配置，也可以提供自定义服务 Bean。

JNA 以 `Postppp` 加载动态库，并按当前操作系统和架构查找 JAR 中对应的资源目录。使用外部库时，在 JVM 启动时传入 `-Djna.library.path=/path/to/native`，该目录需包含当前平台的 `libPostppp.so` 或 `libPostppp.dylib`；加载或链接失败直接抛出 `UnsatisfiedLinkError`。

在 Go 项目运行 `make docker-build` 可重新生成 `build/linux/amd64/libPostppp.so` 与 `libPostppp.h`。二者来自同一构建；替换 Java 资源时需与该头文件核对结构体和导出符号。当前 Java 字段映射需要同时具有 `solvePostppp`（JSON）和 `solvePostpppStruct`（结构体）两个入口。

## SDK 文档与测试报告

- [PostPPP Java SDK 集成开发指南](docs/PostPPP-Java-SDK开发指南.pdf)（PDF）
- [PostPPP Java SDK 集成开发指南](docs/PostPPP-Java-SDK开发指南.docx)（DOCX）
- [PostPPP Java SDK 测试报告](docs/PostPPP-Java-SDK测试报告.pdf)（PDF）
- [PostPPP Java SDK 测试报告](docs/PostPPP-Java-SDK测试报告.docx)（DOCX）
- [PostPPP Java SDK 测试报告](docs/PostPPP-Java-SDK测试报告.md)（Markdown）

测试报告对应的可复现证据归档在 [`docs/test-evidence/2026-10-01`](docs/test-evidence/2026-10-01)。普通 `mvn -B test` 会执行结构体、JSON 和错误路径测试；提供真实数据与许可证后，原生集成测试还会验证 GT10 全日 2877 个历元、结构体入口、JSON 入口以及 `.pos`、SINEX_TRO 和质量报告输出。

## 验证与迁移

普通测试验证结构体大小、默认值、UTF-8/定长数组与响应解析。原生测试在 Linux amd64、Linux arm64 和 macOS arm64 上运行；真实数据测试默认读取项目根目录 `data/`，许可证可通过 `-Dpostppp.test.license=/path/to/license.lic` 指定，数据目录可通过 `-Dpostppp.test.data=/path/to/data` 指定。没有完整数据或许可证时，真实数据测试会跳过，其余接口测试仍会执行。

本次映射移除了旧模型中的 `calMode/es/ee/ti/vrsMode/solstatic/fixThresh/rb/outfile` 和 `brdc/rover/base` 字节缓冲字段；这些字段不属于当前 Go ABI。调用方应改为上述 PPP 参数与文件路径。低层接口返回值改为 `Pointer` 以正确释放原生内存，业务代码建议通过服务获取 Java `String`。
