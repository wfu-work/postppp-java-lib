package com.navfirst.postppp.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** GPST 时间，由 GPS 周和周内秒组成。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostpppTime {

    /** GPS 周（从 GPS 起始周开始计数）。 */
    @SerializedName("week")
    private int week;

    /** 周内秒（Time Of Week，单位：秒）。 */
    @SerializedName("tow")
    private double tow;
}
