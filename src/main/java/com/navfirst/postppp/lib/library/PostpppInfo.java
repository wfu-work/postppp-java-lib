package com.navfirst.postppp.lib.library;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JNA mapping of the native {@code PostpppInput} structure exported by
 * postppp-go-lib.
 *
 * <p>The field order and Java types deliberately follow the C declaration in
 * {@code main.go}. In particular, C {@code int32_t} values are represented by
 * {@code int}, C {@code double} values by {@code double}, and C
 * {@code const char *[4]} values by {@code Pointer[4]}. JNA does not support
 * {@code String[]} as a field inside a {@code Structure}; use
 * {@link #setNavPaths(String...)} (and the corresponding SP3/CLK methods) for
 * the string form.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Structure.FieldOrder({
        "year", "month", "day", "interval", "navsys", "elevMask", "soltype", "tropopt",
        "bdsfreq", "tidecorr", "tropPrn", "maxSigma", "enuOffset", "outDir", "writePos",
        "writeTro", "writeQc", "troInterval", "software", "agency", "verbose", "nav", "sp3",
        "clk", "bia", "atx", "erp", "ionex", "blq", "obsFile", "stationName", "approxXyz",
        "antType", "radome", "antDelta"
})
public class PostpppInfo extends MyStructure {

    public static final int MAX_FILE = 4;
    public static final int STATION_NAME_SIZE = 16;
    public static final int ANT_TYPE_SIZE = 24;
    public static final int RADOME_SIZE = 8;

    public static final int SYS_GPS = 0x01;
    public static final int SYS_GLO = 0x02;
    public static final int SYS_GAL = 0x04;
    public static final int SYS_BDS = 0x08;
    public static final int SYS_QZS = 0x20;
    public static final int SOL_FORWARD = 0;
    public static final int SOL_BACKWARD = 1;
    public static final int SOL_COMBINED = 2;
    public static final int TROP_ZTD = 3;
    public static final int TROP_ZTD_GRAD = 4;
    public static final int BDS_B1I_B3I = 4;
    public static final int BDS_B1C_B2A = 5;

    private static final List<String> FIELD_ORDER = List.of(
            "year", "month", "day", "interval", "navsys", "elevMask", "soltype", "tropopt",
            "bdsfreq", "tidecorr", "tropPrn", "maxSigma", "enuOffset", "outDir", "writePos",
            "writeTro", "writeQc", "troInterval", "software", "agency", "verbose", "nav", "sp3",
            "clk", "bia", "atx", "erp", "ionex", "blq", "obsFile", "stationName", "approxXyz",
            "antType", "radome", "antDelta"
    );

    /** GPST year; zero lets the native library use the first observation epoch. */
    public int year;
    /** GPST month; zero lets the native library use the first observation epoch. */
    public int month;
    /** GPST day; zero lets the native library use the first observation epoch. */
    public int day;
    /** Observation interval in seconds; zero means automatic detection. */
    public double interval;

    /** Satellite-system mask: GPS=1, GLO=2, GAL=4, BDS=8, QZS=32. */
    @Builder.Default
    public int navsys = SYS_GPS | SYS_GAL | SYS_BDS;
    /** Elevation mask in degrees. */
    @Builder.Default
    public double elevMask = 7.0;
    /** Solution direction: 0 forward, 1 backward, 2 combined. */
    @Builder.Default
    public int soltype = SOL_COMBINED;
    /** Troposphere option: 3 ZTD, 4 ZTD plus horizontal gradients. */
    @Builder.Default
    public int tropopt = TROP_ZTD_GRAD;
    /** BeiDou frequency combination: 4 B1I+B3I, 5 B1C+B2A. */
    @Builder.Default
    public int bdsfreq = BDS_B1I_B3I;
    /** Tide correction: 0 off, 1 solid tide, 2 full tide. */
    @Builder.Default
    public int tidecorr = 1;
    /** Troposphere process noise; zero selects the native default. */
    public double tropPrn;
    /** 3D position quality gate in metres; zero disables the gate. */
    @Builder.Default
    public double maxSigma = 0.020;
    /** ENU offset in metres. */
    @Builder.Default
    public double[] enuOffset = new double[3];

    /** Optional output directory; null or an empty string disables file output. */
    public String outDir;
    public int writePos;
    public int writeTro;
    public int writeQc;
    /** SINEX_TRO interval in seconds. */
    @Builder.Default
    public double troInterval = 300.0;
    public String software;
    public String agency;
    public int verbose;

    /** Native pointers for {@code const char *nav[4]}. */
    @Builder.Default
    public Pointer[] nav = new Pointer[MAX_FILE];
    /** Native pointers for {@code const char *sp3[4]}. */
    @Builder.Default
    public Pointer[] sp3 = new Pointer[MAX_FILE];
    /** Native pointers for {@code const char *clk[4]}. */
    @Builder.Default
    public Pointer[] clk = new Pointer[MAX_FILE];

    public String bia;
    public String atx;
    public String erp;
    public String ionex;
    public String blq;
    public String obsFile;

    /** Native {@code char station_name[16]}; UTF-8 bytes including trailing NUL. */
    @Builder.Default
    public byte[] stationName = new byte[STATION_NAME_SIZE];
    /** ECEF approximate coordinate in metres. */
    @Builder.Default
    public double[] approxXyz = new double[3];
    /** Native {@code char ant_type[24]}; UTF-8 bytes including trailing NUL. */
    @Builder.Default
    public byte[] antType = new byte[ANT_TYPE_SIZE];
    /** Native {@code char radome[8]}; UTF-8 bytes including trailing NUL. */
    @Builder.Default
    public byte[] radome = new byte[RADOME_SIZE];
    /** Antenna E/N/U offset in metres. */
    @Builder.Default
    public double[] antDelta = new double[3];

    /* Keep the native strings alive for as long as this Structure is used. */
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    private final transient Memory[] navMemory = new Memory[MAX_FILE];
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    private final transient Memory[] sp3Memory = new Memory[MAX_FILE];
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    private final transient Memory[] clkMemory = new Memory[MAX_FILE];

    @Override
    protected List<String> getFieldOrder() {
        return FIELD_ORDER;
    }

    /** Check fixed array sizes before JNA writes the native structure. */
    @Override
    public void write() {
        requireLength(nav, MAX_FILE, "nav");
        requireLength(sp3, MAX_FILE, "sp3");
        requireLength(clk, MAX_FILE, "clk");
        requireLength(enuOffset, 3, "enuOffset");
        requireLength(approxXyz, 3, "approxXyz");
        requireLength(antDelta, 3, "antDelta");
        requireLength(stationName, STATION_NAME_SIZE, "stationName");
        requireLength(antType, ANT_TYPE_SIZE, "antType");
        requireLength(radome, RADOME_SIZE, "radome");
        setStringEncoding(StandardCharsets.UTF_8.name());
        super.write();
    }

    private static void requireLength(Object array, int length, String field) {
        if (array == null || java.lang.reflect.Array.getLength(array) != length) {
            throw new IllegalArgumentException(field + " 数组长度必须为 " + length);
        }
    }

    /** Apply the same processing defaults as {@code initPostpppInput}. */
    public static PostpppInfo defaults() {
        return new PostpppInfo();
    }

    /** Set the four broadcast-navigation paths as UTF-8 C strings. */
    public void setNavPaths(String... paths) {
        if (nav == null || nav.length != MAX_FILE) {
            nav = new Pointer[MAX_FILE];
        }
        setPaths(paths, nav, navMemory);
    }

    /** Alias for {@link #setNavPaths(String...)}. */
    public void setNav(String... paths) {
        setNavPaths(paths);
    }

    /** Set the four SP3 precise-orbit paths as UTF-8 C strings. */
    public void setSp3Paths(String... paths) {
        if (sp3 == null || sp3.length != MAX_FILE) {
            sp3 = new Pointer[MAX_FILE];
        }
        setPaths(paths, sp3, sp3Memory);
    }

    /** Alias for {@link #setSp3Paths(String...)}. */
    public void setSp3(String... paths) {
        setSp3Paths(paths);
    }

    /** Set the four CLK precise-clock paths as UTF-8 C strings. */
    public void setClkPaths(String... paths) {
        if (clk == null || clk.length != MAX_FILE) {
            clk = new Pointer[MAX_FILE];
        }
        setPaths(paths, clk, clkMemory);
    }

    /** Alias for {@link #setClkPaths(String...)}. */
    public void setClk(String... paths) {
        setClkPaths(paths);
    }

    /** Set the fixed-width station name after checking its UTF-8 byte length. */
    public void setStationNameText(String value) {
        copyFixed(value, stationName, STATION_NAME_SIZE, "stationName");
    }

    /** Alias accepting the logical string form of the fixed-width field. */
    public void setStationName(String value) {
        setStationNameText(value);
    }

    /** Set the fixed-width antenna type after checking its UTF-8 byte length. */
    public void setAntTypeText(String value) {
        copyFixed(value, antType, ANT_TYPE_SIZE, "antType");
    }

    /** Alias accepting the logical string form of the fixed-width field. */
    public void setAntType(String value) {
        setAntTypeText(value);
    }

    /** Set the fixed-width radome name after checking its UTF-8 byte length. */
    public void setRadomeText(String value) {
        copyFixed(value, radome, RADOME_SIZE, "radome");
    }

    /** Alias accepting the logical string form of the fixed-width field. */
    public void setRadome(String value) {
        setRadomeText(value);
    }

    /** Return the NUL-terminated station name decoded as UTF-8. */
    public String getStationNameText() {
        return readFixed(stationName);
    }

    /** Return the NUL-terminated antenna type decoded as UTF-8. */
    public String getAntTypeText() {
        return readFixed(antType);
    }

    /** Return the NUL-terminated radome name decoded as UTF-8. */
    public String getRadomeText() {
        return readFixed(radome);
    }

    private static void setPaths(String[] paths, Pointer[] pointers, Memory[] memories) {
        if (paths == null) {
            paths = new String[0];
        }
        if (paths.length > MAX_FILE) {
            throw new IllegalArgumentException("最多只能提供 " + MAX_FILE + " 个路径");
        }
        Memory[] replacement = new Memory[MAX_FILE];
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (path.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("路径不能包含 NUL 字符");
            }
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            Memory memory = new Memory(bytes.length + 1L);
            memory.write(0, bytes, 0, bytes.length);
            memory.setByte(bytes.length, (byte) 0);
            replacement[i] = memory;
        }
        System.arraycopy(replacement, 0, pointers, 0, MAX_FILE);
        System.arraycopy(replacement, 0, memories, 0, MAX_FILE);
    }

    private static void copyFixed(String value, byte[] target, int size, String field) {
        if (target == null || target.length != size) {
            throw new IllegalArgumentException(field + " 数组长度必须为 " + size);
        }
        if (value == null) {
            value = "";
        }
        if (value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " 不能包含 NUL 字符");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= size) {
            throw new IllegalArgumentException(field + " 最多只能使用 " + (size - 1) + " 个 UTF-8 字节");
        }
        Arrays.fill(target, (byte) 0);
        System.arraycopy(bytes, 0, target, 0, bytes.length);
    }

    private static String readFixed(byte[] value) {
        if (value == null) {
            return "";
        }
        int length = 0;
        while (length < value.length && value[length] != 0) {
            length++;
        }
        return new String(value, 0, length, StandardCharsets.UTF_8);
    }
}
