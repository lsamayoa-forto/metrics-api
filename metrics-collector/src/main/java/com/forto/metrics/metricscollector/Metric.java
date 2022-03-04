package com.forto.metrics.metricscollector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Getter
public class Metric {
    private final static DateTimeFormatter DAY_BUCKET_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String metricName;
    private final float value;
    private final Number sampleRate;
    private final long timestamp;
    private final Map<String, String> tags;

    @JsonCreator
    public Metric(
            @JsonProperty("metric_name") final String metricName,
            @JsonProperty("value") final float value,
            @JsonProperty("sampleRate") final Number sampleRate,
            @JsonProperty("timestamp") final long timestamp,
            @JsonProperty("tags") final Map<String, String> tags) {
        this.metricName = metricName;
        this.value = value;
        this.sampleRate = sampleRate;
        this.timestamp = timestamp;
        this.tags = tags;
    }

    public int getDayBucket() {
        return Integer.parseInt(ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.timestamp), ZoneOffset.UTC)
                .format(DAY_BUCKET_FORMAT));
    }

}
