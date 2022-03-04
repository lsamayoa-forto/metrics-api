package com.forto.metrics.api.suggestions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class LookupResponse {

    private final String type = "LOOKUP";
    private final String metric;
    private final int limit;
    private final int time;
    private final int startIndex = 0;
    private final int totalResults = 100000;
    private final List<TagInfo> tags;
    private final List<LookupResult> results;
}
