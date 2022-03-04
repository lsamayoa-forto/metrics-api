package com.forto.metrics.api.tags;

import java.util.Map;

public record TagRecord(
        String hash,
        Map<String, String> tags
) {

    public boolean containsTags(final Map<String, String> tags) {
        for (String k : tags.keySet()) {
            if (!this.tags.containsKey(k)) {
                return false;
            }
            // Check see if has the value.
            if (!this.tags.get(k).equals(tags.get(k))) {
                return false;
            }
        }
        // we make sure we have all the tags.
        return true;
    }
}
