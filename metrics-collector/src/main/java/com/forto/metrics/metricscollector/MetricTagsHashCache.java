package com.forto.metrics.metricscollector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MetricTagsHashCache {
    private final static String TAG_DELIMITER = ",";
    private final static String TAG_FORMAT = "'%s':'%s'";
    private final static String TAG_STRING_FORMAT = "{%s}";
    private final Map<CacheEntry, String> cachedContent = new ConcurrentHashMap<>();

    public String getHash(String metricName, Map<String, String> tags, Consumer<String> onNewHash) {
        final String tagString = generateTagString(tags);
        final CacheEntry entry = new CacheEntry(metricName, tagString);
        return this.cachedContent.computeIfAbsent(entry, (key) -> {
           final String hashString = this.hashTagString(key.getTagString());
           onNewHash.accept(hashString);
           return hashString;
        });
    }
    private String hashTagString(String tagString) {
        byte[] bytes = null;
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            bytes = md5.digest(tagString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            //TODO: DO NOT SWALLOW!
        }
        // Make it pretty.
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            String hexString = Integer.toHexString(b & 0xff);
            hexString = hexString.toUpperCase();
            builder.append(hexString.length() == 1 ? "0" + hexString : hexString);
        }
        return builder.toString();
    }

    public static String generateTagString(Map<String, String> tags) {
        return String.format(TAG_STRING_FORMAT,
                tags.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(kvp -> String.format(TAG_FORMAT, kvp.getKey(), kvp.getValue()))
                        .collect(Collectors.joining(TAG_DELIMITER)));
    }
}
