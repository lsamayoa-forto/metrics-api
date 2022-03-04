package com.forto.metrics.metricscollector;

import com.datastax.oss.driver.api.core.uuid.Uuids;

import java.util.Random;
import java.util.UUID;

public class TimeFunctions {
    public static UUID getTimeUuid(long epochMillis) {
        long mostSignificantBits = Uuids.startOf(epochMillis).getMostSignificantBits();
        Random r = new Random();
        long leastSigBits = r.nextLong();
        return new UUID(mostSignificantBits, leastSigBits);
    }
}
