package com.forto.metrics.api.metrics;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MetricQuery {

    private AggregationType aggregator;
    private String metric;
    @Builder.Default
    private boolean rate = false;
    @Builder.Default
    private RateOptions rateOptions = new RateOptions();
    private String downsample;
    @Builder.Default
    private Map<String, String> tags = new HashMap<>();
    @Builder.Default
    private List<Filter> filters = new ArrayList<>();
    @Builder.Default
    private boolean explicitTags = false;
    @Builder.Default
    private List<Float> percentiles = new ArrayList<>();
    @Builder.Default
    private Rollup rollupUsage = Rollup.NoFallback;

    public Downsample getTypedDownsample() {
        return new Downsample(this.downsample);
    }

    public Map<String, String> getTags() {
        if(this.tags == null) {
            return new HashMap<>();
        }

        return this.tags;
    }

    public List<Filter> getFilters() {
        if (!this.tags.isEmpty()) {
            // turn 'em into filters and empty.
            for (String k : this.tags.keySet()) {
                String iLiteralValue = this.tags.get(k);
                Filter f = new Filter(FilterType.ILiteralOr, k, iLiteralValue, false);
                this.filters.add(f);
            }
            // Empty the tags.
            this.tags.clear();
        }
        return filters;
    }

    public boolean hasDownsample() {
        return this.downsample != null;
    }
}
