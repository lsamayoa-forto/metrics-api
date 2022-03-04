package com.forto.metrics.api.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Period {

    Millisecond("ms"),

    Second("s"),

    Minute("m"),

    Hour("h"),

    Day("d");

    private static final Map<String, Period> ENUM_MAP;

    static {
        Map<String, Period> map = new HashMap<>();
        for (Period instance : Period.values()) {
            map.put(instance.getName().toLowerCase(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    private final String periodAbbreviation;

    Period(String periodAbbreviation) {
        this.periodAbbreviation = periodAbbreviation;
    }

    public static Period get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }

    public String getName() {
        return this.periodAbbreviation;
    }
}
