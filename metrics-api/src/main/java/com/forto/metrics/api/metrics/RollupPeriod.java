package com.forto.metrics.api.metrics;

import com.forto.metrics.api.metrics.Period;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class RollupPeriod {

    private static final String ROLLUP_REGEX = "(?<periodLength>\\d+)(?<period>([smhd]|ms))";
    private Period period;
    private int periodLength;
    private String code;

    public RollupPeriod(String rollup) {
        final Pattern pattern = Pattern.compile(this.getRollupRegex(), Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(rollup);

        if(!matcher.matches() || matcher.groupCount() < this.getDesiredGroupCount()) {
            throw new IllegalArgumentException("Invalid rollup period supplied " + rollup);
        }

        // Stealy steal.
        this.periodLength = Integer.parseInt(matcher.group("periodLength"));
        this.period = Period.get(matcher.group("period"));
        this.code = rollup;

        this.parseFurther(matcher);
    }

    protected String getRollupRegex() {
        return ROLLUP_REGEX;
    }

    protected int getDesiredGroupCount() {
        return 3;
    }

    protected void parseFurther(Matcher matcher) {
    }
}
