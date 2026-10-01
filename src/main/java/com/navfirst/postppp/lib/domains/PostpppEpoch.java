package com.navfirst.postppp.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PPP 单个历元的解算结果。
 *
 * <p>坐标、偏差、对流层延迟和标准差的单位均为米；数组的顺序会在字段说明中标明。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostpppEpoch {

    /** 历元 GPST 时间。 */
    @SerializedName("time")
    private PostpppTime time;

    /** ECEF 地心地固坐标，顺序为 X、Y、Z，单位：米。 */
    @SerializedName("xyz")
    private double[] xyz;

    /** ECEF 坐标标准差，顺序为 X、Y、Z，单位：米。 */
    @SerializedName("sigma")
    private double[] sigma;

    /** ECEF 三维位置标准差，单位：米。 */
    @SerializedName("sigma3d")
    private double sigma3d;

    /** 相对参考坐标的 ENU 偏差，顺序为东、北、天，单位：米。 */
    @SerializedName("enu")
    private double[] enu;

    /** 总对流层延迟 ZTD，单位：米。 */
    @SerializedName("ztd")
    private double ztd;

    /** 干对流层延迟 ZHD，单位：米。 */
    @SerializedName("zhd")
    private double zhd;

    /** 湿对流层延迟 ZWD，单位：米。 */
    @SerializedName("zwd")
    private double zwd;

    /** 北向对流层梯度，单位：米。 */
    @SerializedName("gn")
    private double gn;

    /** 东向对流层梯度，单位：米。 */
    @SerializedName("ge")
    private double ge;

    /** ZTD 标准差，单位：米。 */
    @SerializedName("sd_ztd")
    private double sdZtd;

    /** 北向梯度标准差，单位：米。 */
    @SerializedName("sd_gn")
    private double sdGn;

    /** 东向梯度标准差，单位：米。 */
    @SerializedName("sd_ge")
    private double sdGe;

    /** 参与该历元 PPP 解算的卫星数。 */
    @SerializedName("nsat")
    private int nsat;

    /** 历元解算状态：0 无解、3 SPP、4 PPP 浮点解、6 PPP 固定解（沿用 Go/native 状态码）。 */
    @SerializedName("stat")
    private int stat;

    /** 是否通过位置质量门限并计入最终统计。 */
    @SerializedName("accepted")
    private boolean accepted;
}
