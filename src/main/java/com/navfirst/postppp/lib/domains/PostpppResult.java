package com.navfirst.postppp.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * postppp-go-lib 返回的单测站单日完整解算结果。
 *
 * <p>字段名通过 {@link SerializedName} 与 Go JSON 协议保持一致；Java 调用方使用
 * Lombok 生成的驼峰 getter。坐标和偏差数组的顺序在字段注释中说明。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostpppResult {

    /** 测站名称，通常来自观测文件头或输入的测站名称。 */
    @SerializedName("name")
    private String name;

    /** 解算年份。 */
    @SerializedName("year")
    private int year;

    /** 年积日（Day Of Year，1～366）。 */
    @SerializedName("doy")
    private int doy;

    /** 按 GPST 排序的逐历元结果；未通过质量门限的历元也会保留用于诊断。 */
    @Builder.Default
    @SerializedName("epoch")
    private List<PostpppEpoch> epoch = new ArrayList<>();

    /** 是否存在通过质量检查的坐标结果。为 {@code false} 时不应解释坐标数组中的零值。 */
    @SerializedName("have_coord")
    private boolean haveCoord;

    /** 用于坐标汇总的合格历元数量。 */
    @SerializedName("n_coord")
    private int nCoord;

    /** 有效成果的起始时间。 */
    @SerializedName("first")
    private PostpppTime first;

    /** 有效成果的结束时间。 */
    @SerializedName("last")
    private PostpppTime last;

    /** 最后一个合格历元的 ECEF 坐标，顺序为 X、Y、Z，单位：米。 */
    @SerializedName("final_xyz")
    private double[] finalXyz;

    /** 最后一个合格历元的 ECEF 坐标标准差，顺序为 X、Y、Z，单位：米。 */
    @SerializedName("final_sigma")
    private double[] finalSigma;

    /** 最后一个合格历元的大地坐标，顺序为纬度（度）、经度（度）、大地高（米）。 */
    @SerializedName("final_llh")
    private double[] finalLlh;

    /** 所有合格历元的 ECEF 坐标平均值，顺序为 X、Y、Z，单位：米。 */
    @SerializedName("mean_xyz")
    private double[] meanXyz;

    /** 相对合格历元均值的 ENU 重复性 RMS，顺序为东、北、天，单位：米。 */
    @SerializedName("rms_enu")
    private double[] rmsEnu;

    /** 最后一个合格历元相对先验坐标的 ENU 偏差，已包含 ENU 偏移，顺序为东、北、天，单位：米。 */
    @SerializedName("bias_enu")
    private double[] biasEnu;

    /** 全天平均 ENU 偏差，可能包含收敛期结果，顺序为东、北、天，单位：米。 */
    @SerializedName("bias_enu_mean")
    private double[] biasEnuMean;

    /** 全天合格成果的平均总对流层延迟 ZTD，单位：米。 */
    @SerializedName("ztd_mean")
    private double ztdMean;

    /** 观测到的总历元数。 */
    @SerializedName("n_epoch")
    private int nEpoch;

    /** 使用 PPP 状态完成解算的历元数。 */
    @SerializedName("n_ppp")
    private int nPpp;

    /** 通过质量门限并被接受的历元数。 */
    @SerializedName("n_accept")
    private int nAccept;

    /** 因位置质量门限被剔除的历元数。 */
    @SerializedName("n_gated")
    private int nGated;

    /** 仅得到 SPP 结果、未得到 PPP 结果的历元数。 */
    @SerializedName("n_spp")
    private int nSpp;

    /** 没有可用定位结果的历元数。 */
    @SerializedName("n_none")
    private int nNone;

    /** 数据可用率，单位：百分比。 */
    @SerializedName("avail")
    private double avail;

    /** 平均可用卫星数。 */
    @SerializedName("mean_nsat")
    private double meanNsat;

    /** 观测中断窗口数（不含首次收敛期）。 */
    @SerializedName("n_gap")
    private int nGap;

    /** 最大观测中断时长，单位：秒。 */
    @SerializedName("max_gap_sec")
    private double maxGapSec;

    /** 最大观测中断的开始时间。 */
    @SerializedName("max_gap_start")
    private PostpppTime maxGapStart;

    /** 首次达到收敛条件所需时间，单位：秒。 */
    @SerializedName("converge_sec")
    private double convergeSec;

    /** 综合质量等级，通常为“可靠”“基本可靠”或“不可靠”。 */
    @SerializedName("grade")
    private String grade;

    /** 输出结果中记录的软件名称。 */
    @SerializedName("software")
    private String software;

    /** 输出结果中记录的机构名称。 */
    @SerializedName("agency")
    private String agency;

    /** 使用的精密产品标识。 */
    @SerializedName("products")
    private String products;

    /** 逐历元 POS 文件实际输出路径；未输出时为空。 */
    @SerializedName("pos_file")
    private String posFile;

    /** 对流层 SINEX_TRO 文件实际输出路径；未输出时为空。 */
    @SerializedName("tro_file")
    private String troFile;
}
