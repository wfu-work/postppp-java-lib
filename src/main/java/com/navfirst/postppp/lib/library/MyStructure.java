package com.navfirst.postppp.lib.library;

import com.sun.jna.Structure;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 创建：馥溪凝
 * 日期：2021/10/11 12:31
 * 描述：com.navfirst.dmonitor.lib.library
 */
public class MyStructure extends Structure {

    @Override
    protected List<String> getFieldOrder() {
        List<Field> fieldList = this.getFieldList();
        return fieldList.stream().map(Field::getName).collect(Collectors.toList());
    }

    /**
     * 结构体的引用
     *
     * @author maoko
     *
     */
    public static class ByReference extends MyStructure implements Structure.ByReference {
    }

    /**
     * 结构体对象
     *
     * @author fanpei
     *
     */
    public static class ByValue extends MyStructure implements Structure.ByValue {
    }

}
