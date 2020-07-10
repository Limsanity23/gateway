package com.bnids.gateway.entity;

public enum WarningCarRegistEnum {
    PARKING_DURATION_VIOLATION("PARKING_DURATION_VIOLATION", "주차시간 위반"),
    NUMBER_ACCESS_VIOLATION("NUMBER_ACCESS_VIOLATION", "출입회수 위반");

    private String name;
    private String value;

    WarningCarRegistEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
