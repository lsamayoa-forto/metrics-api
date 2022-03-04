package com.forto.metrics.api.metrics;

import java.util.ArrayList;
import java.util.List;

public class MetricsRequestValidator {

    private MetricRequest request;
    private List<String> reasons = new ArrayList<>();

    public MetricsRequestValidator(MetricRequest request) {
        this.request = request;
    }

    public boolean isValid() {
        boolean isValid = this.checkRule(request.getStart() > 946684800000L, "Start time is before 1st Jan 2000");
        isValid = isValid && this.checkRule(request.getQueries().size() > 0, "There are no queries in the request");
        for (MetricQuery q : request.getQueries()) {
            // we should have some dimensions.
            isValid = q.getTags() != null;
            isValid = isValid && checkRule((q.getAggregator() != AggregationType.First && q.getAggregator() != AggregationType.Last),
                    "First and Last are downsample only aggregators and cannot be used as a main aggregator");
        }
        // Send it back.
        return true;
    }

    private boolean checkRule(boolean rule, String brokenRuleText) {
        if (!rule) {
            this.reasons.add(brokenRuleText);
        }

        return rule;
    }
}
