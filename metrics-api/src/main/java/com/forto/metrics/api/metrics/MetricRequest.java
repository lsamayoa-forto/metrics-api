package com.forto.metrics.api.metrics;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@NoArgsConstructor
public class MetricRequest {

    private long start;
    private long end;
    private List<MetricQuery> queries = new ArrayList<>();
    private boolean noAnnotations;
    private boolean globalAnnotations;
    private boolean msResolution;
    private boolean showTSUIDs;
    private boolean showSummary;
    private boolean showStats;
    private boolean showQuery;
    private boolean delete;
    private String timezone;
    private boolean useCalendar;

    public MetricRequest(long start, long end) {
        this();
        this.start = start;
        this.end = end;
    }

    public MetricRequest(long start) {
        this(start, 0);
    }

    public long getEnd() {
        if (this.end == 0) {
            // TODO: Implement calendar information passed through.
            this.end = new Date().getTime();
        }

        return this.end;
    }

    public int[] getDayBucketRange() {
        // Get UTC.
        LocalDateTime startUtc = LocalDateTime.ofInstant(
                new Date(this.start).toInstant(),
                ZoneOffset.UTC
        );
        LocalDateTime endUtc = LocalDateTime.ofInstant(
                new Date(this.getEnd()).toInstant(),
                ZoneOffset.UTC
        );
        // Work out the raw difference between them in hours.
        int hours = (int) ChronoUnit.HOURS.between(startUtc, endUtc);
        // Full days...
        int days = hours / 24;
        // Now do we have something that is less than a day but takes us over bounday?
        int remainingHours = hours % 24;
        if (remainingHours > 0) {
            // so take the hour we started at.
            int hour = startUtc.getHour();
            if (hour + remainingHours > 24) {
                days++;
            }
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        int[] buckets = new int[days + 1];

        for (int i = 0; i <= days; i++) {
            Date date = new Date(startUtc.plusDays(i).toInstant(ZoneOffset.UTC).toEpochMilli());
            String representation = dateFormat.format(date);
            // through the int representation.
            buckets[i] = Integer.parseInt(representation);
        }

        return buckets;
    }
}