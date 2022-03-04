package com.forto.metrics.api.suggestions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum SuggestQueryType {

    @JsonProperty()
    Metrics("metrics"),
    @JsonProperty()
    TagKey("tagk"),
    @JsonProperty()
    TagValue("tagv"),
    TagsByMetric("tagsByMetric"),
    ValuesByMetricTag("valuesByMetricTag");

    public static final String REGEX_TEXT = "(?<metricName>\\w+((\\.|\\-|\\/|\\_)\\w+)*)(\\{(?<tagKey>\\w+(\\.\\w+)*)\\})?";

    private static final Map<String, SuggestQueryType> ENUM_MAP;

    static {
        Map<String, SuggestQueryType> map = new HashMap<>();
        for (SuggestQueryType instance : SuggestQueryType.values()) {
            map.put(instance.getJsonName().toLowerCase(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    @Getter
    private final String jsonName;

    SuggestQueryType(String jsonName) {
        this.jsonName = jsonName;
    }

    public static SuggestQueryType get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
}
