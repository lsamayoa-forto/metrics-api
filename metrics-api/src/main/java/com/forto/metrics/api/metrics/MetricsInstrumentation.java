package com.forto.metrics.api.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MetricsInstrumentation {

    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsInstrumentation(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordFetch(Duration duration, int totalQueries, int buckets) {
        // TODO: maybe better metric name yoh ?
        // TODO: maybe converting in to string too much on every metric send ?
        meterRegistry.timer("metrics.api.endpoint_latency",
                        "queries", String.valueOf(totalQueries),
                        "dayBuckets", String.valueOf(buckets))
                .record(duration);
    }
}
