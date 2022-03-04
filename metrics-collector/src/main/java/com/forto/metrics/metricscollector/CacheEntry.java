package com.forto.metrics.metricscollector;

import java.util.Objects;

public class CacheEntry {

    private String metricName;
    private String tagString;

    public CacheEntry(String metricName, String tagString) {
        this.tagString = tagString;
        this.metricName = metricName;
    }

    public String getMetricName() {
        return this.metricName;
    }

    public String getTagString() {
        return this.tagString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheEntry that = (CacheEntry) o;
        // ensure nulls.
        if(!Objects.equals(this.metricName, that.metricName)) return false;
        if(!Objects.equals(this.tagString, that.tagString)) return false;
        return true;
    }

    public int hashCode() {
        int code = Objects.hash(this.metricName, this.tagString);
        return code;
    }
}
