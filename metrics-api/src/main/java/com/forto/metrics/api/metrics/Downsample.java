package com.forto.metrics.api.metrics;

import lombok.Getter;

import java.util.regex.Matcher;

@Getter
public class Downsample extends RollupPeriod {

    private static final String DOWNSAMPLE_REGEX = "(?<periodLength>\\d+)(?<period>([smhd]|ms))\\-(?<aggregationType>(avg|min|max|sum|dev|first|last|nonzmin|count))";
    private AggregationType aggregationType;

    public Downsample(String downsampleCode) {
        super(downsampleCode);
    }

    public long getNextChunk() throws IllegalArgumentException {
        final long amt;
        switch (this.getPeriod()) {
            case Millisecond:
                amt = 1;
                break;
            case Second:
                amt = 1000;
                break;
            case Minute:
                amt = 1000 * 60;
                break;
            case Hour:
                amt = 1000 * 60 * 60;
                break;
            case Day:
                amt = 1000 * 60 * 60 * 24;
                break;
            default:
                throw new IllegalArgumentException("Invalid period supplied.");
        }
        return amt * this.getPeriodLength();
    }

    @Override
    protected int getDesiredGroupCount() {
        return 5;
    }

    @Override
    protected String getRollupRegex() {
        return DOWNSAMPLE_REGEX;
    }

    @Override
    protected void parseFurther(Matcher matcher) {
        this.aggregationType = AggregationType.get(matcher.group("aggregationType"));
    }
}
