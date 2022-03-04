package com.forto.metrics.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Metric {

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

    public static Metric parseString(final String packet, long timestamp) {
        final String[] bits = packet.split("\\|#");
        final String[] tags = bits.length > 1 && bits[1].length() > 0 ?
                bits[1].split(",") : new String[]{};
        final String[] fieldBits = bits[0].split(":");
        final String metricName = fieldBits[0];
        final String[] fields = fieldBits[1].split("\\|");
        final String metricType = fields[1].trim();
        final String value = fields[0];

        final Map<String, String> tagMap = new HashMap<>();
        for (String tag : tags) {
            final String[] split = tag.split(":");
            tagMap.put(split[0], split[1]);
        }

        return new Metric(
                metricName,
                Float.parseFloat(value),
                1,
                timestamp,
                tagMap
        );
    }

    public String getMetricName() {
        return metricName;
    }

    public float getValue() {
        return value;
    }

    public Number getSampleRate() {
        return sampleRate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getTags() {
        return tags;
    }
}
