package com.forto.metrics.api.suggestions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class LookupResult {

    private final Map<String, String> tags = new HashMap<>();
    private final String metric;
    private final String tsuid = "";

    public LookupResult(String metric, String key, String value) {
        this.metric = metric;
        this.tags.put(key, value);
    }
}
