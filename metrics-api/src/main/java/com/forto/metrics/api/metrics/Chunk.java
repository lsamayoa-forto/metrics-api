package com.forto.metrics.api.metrics;

import java.time.Instant;

public class Chunk {

    private final Instant startTime;
    private final AggregationType aggregationType;

    private double sumValue;
    private long count;

    public Chunk(Instant instant, Downsample downsample) {
        this(instant, downsample.getNextChunk(), downsample.getAggregationType());
    }

    public Chunk(Instant instant, long increment, AggregationType aggregationType) {
        this.startTime = instant;
        this.aggregationType = aggregationType;
    }

    public long getStartTime() {
        return this.startTime.toEpochMilli();
    }

    public String getDpLabel() {
        return Long.valueOf(this.startTime.toEpochMilli()).toString();
    }

    public double getDpValue() {
        double amount = 0.0;
        switch(this.aggregationType) {
            case Count:
                amount = this.count;
                break;
            case Average:
                amount = this.count == 0 ? 0 : this.sumValue / this.count;
                break;
                /*
            case Minimum:
            case Maximum:
            case First:
            case Last:
            case Sum:
            case CumulativeSum:
                 */
            default:
                amount = this.sumValue;
                break;
        }

        return amount;
    }

    /**
     * horrific hack for cummulative sum
     *
     * @param value
     */
    public void setDpValue(long value) {
        this.count = 1;
        this.sumValue = value;
    }

    public void increment(double sumValue, long count) {
        // increment the core insides of this chunk.
        // so when we apply the downsample we will know what to do.
        this.count += count;

        switch(this.aggregationType) {
            case First:
                this.sumValue = this.count == 1 ? sumValue : this.sumValue;
                break;
            case Last:
                this.sumValue = sumValue;
                break;
            case Maximum:
                this.sumValue = Math.max(sumValue, this.sumValue);
                break;
            case Minimum:
                this.sumValue = this.count == 1 || (sumValue < this.sumValue) ? sumValue : this.sumValue;
                break;
            case NonZeroMinumum:
                this.sumValue = this.count == 1 || (sumValue < this.sumValue && sumValue != 0) ? sumValue : this.sumValue;
                break;
            default:
                this.sumValue += sumValue;
                break;
        }
    }

    public boolean hasValue() {
        return this.count > 0;
    }
}
