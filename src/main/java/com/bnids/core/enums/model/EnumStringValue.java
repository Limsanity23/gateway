package com.bnids.core.enums.model;

/**
 * @author yannishin
 */
public class EnumStringValue {
    private String key;
    private String value;

    public EnumStringValue(EnumStringModel enumModel) {
        key = enumModel.getKey();
        value = enumModel.getValue();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
