package com.forto.metrics.api.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum AggregationType {

    @JsonProperty("nonzmin")
    NonZeroMinumum("nonzmin"),
    @JsonProperty("first")
    First("first"),
    @JsonProperty("last")
    Last("last"),
    @JsonProperty("count")
    Count("count"),
    @JsonProperty("cumsum")
    CumulativeSum("cumsum"),
    @JsonProperty("min")
    Minimum("min"),
    @JsonProperty("sum")
    Sum("sum"),
    @JsonProperty("max")
    Maximum("max"),
    @JsonProperty("avg")
    Average("avg"); /*,
    NotImplementedYetException();
    StandardDeviation("dev"); */

    private static final Map<String, AggregationType> ENUM_MAP;

    static {
        Map<String, AggregationType> map = new HashMap<>();
        for (AggregationType instance : AggregationType.values()) {
            map.put(instance.getJsonName().toLowerCase(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    @Getter
    private final String jsonName;

    AggregationType(String jsonName) {
        this.jsonName = jsonName;
    }

    public static String[] getAggregators() {
        return new String[]{
                First.getJsonName(),
                Last.getJsonName(),
                Count.getJsonName(),
                CumulativeSum.getJsonName(),
                NonZeroMinumum.getJsonName(),
                Minimum.getJsonName(),
                Sum.getJsonName(),
                Maximum.getJsonName(),
                Average.getJsonName()/*,
                StandardDeviation.getJsonName()*/
        };
    }

    public static AggregationType get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
}
