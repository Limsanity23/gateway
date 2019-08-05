package com.bnids.core.enums.model;

/**
 * @author yannishin
 */
public class EnumIntegerValue {
    private Integer key;
    private String value;

    public EnumIntegerValue(EnumIntegerModel enumModel) {
        key = enumModel.getKey();
        value = enumModel.getValue();
    }

    public Integer getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
