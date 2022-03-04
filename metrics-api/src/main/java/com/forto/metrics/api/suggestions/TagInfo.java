package com.forto.metrics.api.suggestions;

import lombok.Getter;

@Getter
public class TagInfo {

    private final String key;
    private final String value;

    public TagInfo(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
