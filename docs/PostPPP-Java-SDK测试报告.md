# PostPPP Java SDK 测试报告

报告 v1.1  |  SDK 1.0.0  |  原生库 1.0  |  2026-10-01

## 结论摘要

结论为通过。本轮在 Linux amd64 Docker 环境执行 `mvn -B -o clean verify`，两个测试类共 12 项测试全部通过，Failures=0、Errors=0、Skipped=0，构建结果为 `BUILD SUCCESS`。测试覆盖 `PostpppInfo` ABI 映射、默认值、UTF-8 和定长字段、返回 JSON 的 Java 对象封装、错误码保留、native 版本和许可证错误路径。

| 测试内容 | 本轮结果 | 结论 |
| --- | --- | --- |
| PostpppInfo 映射 | 6 项；0 失败、0 错误、0 跳过 | 通过 |
| 应用与 native 冒烟 | 6 项；0 失败、0 错误、0 跳过 | 通过 |
| 总计 | 12 项；0 失败、0 错误、0 跳过 | 通过 |
| 构建 | `mvn -B -o clean verify`；BUILD SUCCESS | 通过 |

## 一、测试环境与计量方法

- Git 提交：`69e3223a0ff0452b35b0deebeaa2caca77335b93`
- 执行平台：Linux amd64（Docker `--platform linux/amd64`）
- Docker 镜像：`maven:3.9-eclipse-temurin-21`
- Docker 镜像 ID：`sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320`
- Java：Eclipse Temurin 21，工程编译 release 20
- Maven：容器内 Maven，离线模式 `-o`
- SDK：`com.navfirst:postppp-java-lib:1.0.0`
- native：4,710,480 bytes，SHA-256 `1b30df9419d26e350c819f659d9c8ecce9c251e372ed9f65c4c9bf4a09b9ee48`
- JAR：7,627,112 bytes，SHA-256 `cf04aba0a2ad6017b9935b8825127c48e040870acb6eb4f8597ab26934d048d4`

```sh
mvn -B -o clean verify
```

判定条件：测试数不少于 12，Failures、Errors、Skipped 均为 0，Maven 输出 `BUILD SUCCESS`，并归档 Surefire XML/TXT、日志和 SHA-256 清单。

### 计时与统计口径

测试套件耗时来自 Surefire 汇总，只表示当前容器内 Java 测试和 native 冒烟的执行时间。报告不把该耗时当作单站 PPP 解算性能，也不从没有真实观测窗口的用例推导收敛时间、吞吐量或精度。

## 二、数据来源与完整性

测试数据由固定 JSON 响应夹具、PostpppInfo 构造对象和 native 错误路径组成。测试不把不存在的观测文件当作 PPP 精度样本；真实观测、广播星历、SP3、CLK、BIA 和 ANTEX 文件由业务部署提供。

| 数据类别 | 本轮使用 | 完整性检查 |
| --- | --- | --- |
| JSON 响应夹具 | 成功响应、逐历元对象、-6 部分结果 | Gson 下划线字段、数组、嵌套时间和布尔值均断言 |
| 结构体输入 | `PostpppInfo.defaults()`、Builder 和路径 setter | 392 字节布局、数组长度、UTF-8 NUL 结尾和默认值断言 |
| 错误输入 | 空 JSON、缺少观测文件、不可用许可证、非法路径 | 统一解析为 code/error 或 `PostpppException` |
| 原生文件 | JAR 内 `linux-x86-64/libPostppp.so` | 文件大小、加载结果和 SHA-256 已归档 |

### 传输封装与星历

输入通过 Java 对象、JNA Structure 或 JSON 字符串进入 Go C ABI。nav、sp3、clk 的每个路径均保留 UTF-8 NUL 结尾的 native Memory；测试验证路径数量上限和引用生命周期。星历、精密轨道、钟差、BIA、ANTEX、ERP 与 BLQ 的真实内容不在本轮夹具中。

### 主测试站点与时间

本轮没有可复核的真实测站、独立坐标真值或八个独立小时窗口，因此不填写虚构的站点名、观测日期和定位误差。业务验收应提供可归档的 RINEX、产品文件、真值和运行日志。

路径字段按 UTF-8 传递，定长 `station_name`、`ant_type`、`radome` 按字节容量限制。当前证据没有有效许可证和真实日观测数据，因此本轮不声称完成坐标精度、收敛时间或吞吐量验收。

## 三、回归结果与独立站点解算

| 测试类 | 测试数 | 失败 | 错误 | 跳过 | 耗时 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `PostpppInfoTests` | 6 | 0 | 0 | 0 | 0.540 s |
| `PostpppJavaLibApplicationTests` | 6 | 0 | 0 | 0 | 3.797 s |
| 合计 | 12 | 0 | 0 | 0 | 4.337 s |

### 用例明细

| 测试类 | 用例 | 状态 |
| --- | --- | --- |
| `PostpppInfoTests` | `partialResultIsRetainedForNoSolutionResponse` | 通过 |
| `PostpppInfoTests` | `responseJsonIsDecodedIntoTypedResultObjects` | 通过 |
| `PostpppInfoTests` | `postpppInfoMatchesNativeLayoutAndDefaults` | 通过 |
| `PostpppInfoTests` | `builderRetainsUtf8PathsAndFixedArrayLengths` | 通过 |
| `PostpppInfoTests` | `responseEnvelopePreservesCodeErrorAndResult` | 通过 |
| `PostpppInfoTests` | `fixedWidthAndPathInputsRejectNativeStringViolations` | 通过 |
| `PostpppJavaLibApplicationTests` | `javaDefaultsMatchNativeInitializer` | 通过 |
| `PostpppJavaLibApplicationTests` | `structureEntryPointReturnsGoErrorEnvelope` | 通过 |
| `PostpppJavaLibApplicationTests` | `unavailableLicenseReturnsErrorForRawAndTypedResponses` | 通过 |
| `PostpppJavaLibApplicationTests` | `resultFacadePreservesNativeErrorCode` | 通过 |
| `PostpppJavaLibApplicationTests` | `jsonEntryPointReturnsGoErrorEnvelope` | 通过 |
| `PostpppJavaLibApplicationTests` | `getVersion` | 通过 |

12 个用例均由 Surefire 报告为 passed；native 标准输出不影响断言结果。

### 现有 JUnit 回归

`PostpppInfoTests` 负责结构体布局、字段默认值、固定宽度字段和 typed JSON 夹具；`PostpppJavaLibApplicationTests` 负责 Spring 上下文、native 版本、两种入口、许可证错误和异常 facade。

### 八个独立小时窗口

当前证据没有八个独立小时的真实观测窗口，状态为待执行。补测时应固定同一产品版本和许可证，逐小时记录输入文件哈希、开始/结束时间、code、n_accept、have_coord、grade 和输出文件哈希。

### 相同输入重复解算

当前未进行真实数据的重复解算。正式验收应使用相同输入至少重复三次，比较 final_xyz、final_llh、rms_enu、converge_sec 和输出文件差异。

## 四、定位精度与重复性测试

### 测量目标与当前证据

本轮自动化测试证明 Java 对象能完整承载 native 返回字段，但没有真实测站和独立真值，因此当前精度、收敛、固定率、可用率和重复性均标记为待业务验收。

- `PostpppInfo` 字段顺序、392 字节 native size、默认值和固定数组长度通过验证。
- `nav/sp3/clk` 使用 `Pointer[4]`，路径以 UTF-8 NUL 结尾并保持 Memory 引用。
- `station_name[16]`、`ant_type[24]`、`radome[8]` 的 UTF-8 字节容量和非法输入拒绝通过验证。
- `PostpppResponse`、`PostpppResult`、`PostpppEpoch`、`PostpppTime` 的嵌套 JSON 解码通过验证。
- code=-6 时保留部分 result；raw 和 typed 入口保留一致的错误码。

### 正式精度验收方法

对每个真实站点保存独立参考坐标，并按 ECEF、ENU、纬度、经度和高程计算误差。至少报告固定率、可用率、P50/P95、RMSE、最大误差、收敛秒数和重复解算差异。

### 错误码与资源释放

| 场景 | 期望行为 | 结果 |
| --- | --- | --- |
| JSON 为空或缺少观测文件 | 返回 code=-1 的 Go 错误封装，不生成 result | 通过 |
| 许可证路径不可用 | raw 与 typed response 均保留许可证错误 | 通过 |
| code=-6 且带 result | 保留部分 result；结果 facade 抛异常 | 通过 |
| 响应格式非法 | `JsonUtils` 以 `invalid_response` 拒绝 | 通过 |
| native Pointer | 服务层在 finally 中读取并释放一次 | 通过 |

## 五、位移反应时间测试

### 区分位移响应和解算收敛

PostPPP Java SDK 当前接口是同步日解算，不提供逐历元回调时间戳。因此位移反应时间不能由本轮 JUnit 耗时推导，必须在采集时间、native 处理时间、结果写出时间和业务告警时间之间分别打点。

| 窗口 | 输入与指标 | 当前状态 |
| --- | --- | --- |
| 1 小时 | 首个位移历元、首个 accepted 历元和结果写出时间 | 待真实数据 |
| 8 小时 | 收敛前后窗口、converge_sec 和告警延迟 | 待真实数据 |
| 全天 | 完整日结果、n_gap、max_gap_sec | 待真实数据 |

受控试验还需要已知阶跃幅值和准确发生时间，并报告从发生到 accepted 变化的延迟。

## 六、5 MB 数据解码与全流程性能

### 样本大小与边界

JSON 请求上限为 64 MiB，JNA 路径和固定数组有更小的 ABI 限制。性能验收应至少准备 5 MB JSON、5 MB 观测片段和全天 RINEX 三类样本。

| 制品 | 大小 | SHA-256 | 判定 |
| --- | ---: | --- | --- |
| `libPostppp.so` | 4,710,480 bytes | `1b30df9419d26e350c819f659d9c8ecce9c251e372ed9f65c4c9bf4a09b9ee48` | 已加载并通过 native 冒烟 |
| `postppp-java-lib-1.0.0.jar` | 7,627,112 bytes | `cf04aba0a2ad6017b9935b8825127c48e040870acb6eb4f8597ab26934d048d4` | Maven verify 生成 |

### 实测性能

本轮只记录 Maven 测试耗时和构建产物哈希，没有 5 MB 真实解码样本、真实 PPP 数据集或固定硬件基线，因此没有填写虚构的吞吐量或 P95。

### 判定边界

| 待测项目 | 建议方法 | 当前状态 |
| --- | --- | --- |
| 单站日解算耗时 | 固定观测窗口和产品，重复 10 次并统计 P50/P95 | 待真实数据 |
| 大文件容量 | 5 MB、50 MB、全天数据逐档执行并观察 RSS 回落 | 待真实数据 |
| 输出性能 | 分别开启 POS、TRO、QC，比较文件写入和磁盘空间 | 待真实数据 |
| 异常恢复 | 截断文件、错日期、缺产品、无许可证，确认进程可继续服务 | 错误路径已覆盖，长时恢复待测 |

## 七、多任务并发解算

### 任务隔离与公平比较

每个任务应使用独立 PostpppInfo、许可证上下文和 outDir，并固定相同的产品版本。并发对比需记录任务提交、native 开始、native 结束和结果落盘时间。

### 平台矩阵

| 平台 | 资源目录 | 本轮结论 |
| --- | --- | --- |
| Linux x86-64 | `linux-x86-64/libPostppp.so` | 已执行，12 项全部通过 |
| Linux AArch64 | `linux-aarch64/libPostppp.so` | 未在本轮容器中执行，需要同架构运行时验证 |
| macOS Apple Silicon | `darwin-aarch64/libPostppp.so` | 当前资源文件为 Linux ELF，不能作为 macOS native 通过依据 |
| Windows | 未提供 | 不纳入本轮 |

### 结果一致性与仍需验证的边界

当前没有真实数据的并发结果样本，暂不声称多任务结果一致性或吞吐量通过。生产部署应为每个任务分配唯一输出目录，并在确认 native 线程安全前采用业务层串行保护或多进程隔离。

- 同一 `PostpppInfo` 在调用期间不得并发修改。
- native 全局许可证缓存应在每个进程中固定并在解算前校验。
- native 返回 Pointer 只由服务层读取和释放一次。

## 八、问题清单与验收建议

| 编号 | 级别 | 问题与影响 | 关闭条件 |
| --- | --- | --- | --- |
| TST-001 | P1 | 缺少真实测站和独立真值，不能验收绝对精度 | 提供匹配真值并统计 ECEF/ENU 误差、RMSE、P95 |
| TST-002 | P1 | 没有真实位移时刻和阶跃幅值，不能验收响应时间 | 完成受控位移试验并记录采集、计算、回调时间戳 |
| TST-003 | P2 | AArch64、macOS、Windows native 尚未完成同架构验证 | 补齐资源并执行相同测试矩阵 |
| TST-004 | P2 | 没有长时容量和并发基线 | 补充多站、长时、不同输出目录和异常恢复测试 |

当前 Java SDK 契约和 Linux amd64 native 冒烟可作为开发集成门禁。正式发布前仍应把真实 PPP 质量、精度、性能、平台矩阵和长时稳定性纳入独立验收。

## 附录 A、复现方法与证据索引

在仓库根目录执行：

```sh
python3 scripts/report-tests/run_report_tests.py
# 或直接执行
mvn -B -o clean verify
```

脚本会在 Linux amd64 Docker 中执行 clean verify，解析 Surefire XML，归档 Maven 日志、测试用例、源码基线和制品哈希。证据目录：[`docs/test-evidence/2026-10-01`](test-evidence/2026-10-01)。

- `maven-verify-linux-amd64.log`：完整构建与测试输出。
- `TEST-*.xml`、`*.txt`：Surefire 机器可读和人类可读结果。
- `summary.json`：环境、套件、用例、产物哈希和总计。
- `source-baseline.json`、`sha256.json`、`sha256.txt`：源码与证据完整性基线。

## 附录 B、历史结果与覆盖边界

### 原 v1.1 报告保留记录

本版本延续 v1.1 的 12/12 绿色回归结论，并把字段映射、typed JSON、输入限制、平台矩阵和待验收项目展开到正文。

### 历史构建制品

本轮归档的 native、JAR、Docker 镜像和源码基线均记录哈希，便于后续比较 ABI 或依赖升级带来的变化。

### 已覆盖

- PostpppInfo 392 字节结构体布局、默认值、路径数组、固定宽度 UTF-8 字段。
- 成功 JSON、-6 部分结果、错误 code/error、PostpppResponse 与 PostpppResult。
- 结构体和 JSON 两种入口、native 版本、许可证错误和 typed facade。

### 未覆盖或待业务验收

- 真实观测文件的日解算精度、收敛时间、固定率和坐标真值误差。
- 多平台 native、长时运行、并发吞吐、大文件容量和异常恢复。
- POS、SINEX_TRO、QC 输出文件的内容级验收和磁盘容量门禁。

报告结论：12/12 项测试通过，构建通过，证据已归档。
