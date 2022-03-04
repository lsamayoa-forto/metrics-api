package com.forto.metrics.api.metrics;

import com.forto.metrics.api.tags.TagGroup;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class MetricsResponse {

    private final String metric;
    private final Map<String, String> tags;
    private final List<String> aggregatedTags;
    private final Map<String, Double> dps;

    public MetricsResponse(String metric, TagGroup group, Map<String, Double> dps) {
        this.metric = metric;
        this.tags = group.getTags();
        this.aggregatedTags = List.of(group.getAggregatedTags());
        this.dps = dps;
    }
}
