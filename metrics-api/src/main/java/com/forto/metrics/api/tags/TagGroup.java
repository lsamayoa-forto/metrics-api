package com.forto.metrics.api.tags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagGroup {
    private static final Logger logger = LoggerFactory.getLogger(TagGroup.class);

    private final int id;
    private final TagRecord[] tagHashes;
    private final List<String> filterTags;

    public TagGroup(final int id,
                    final TagRecord[] tagHashes,
                    final List<String> filterTags) {
        this.id = id;
        this.tagHashes = tagHashes;
        this.filterTags = filterTags;
    }

    public int getId() {
        return this.id;
    }

    public String[] getTagHashes() {
        String[] hashes = new String[this.tagHashes.length];
        for (int i = 0; i < this.tagHashes.length; i++) {
            // Send back the hash.
            hashes[i] = this.tagHashes[i].hash();
        }
        return hashes;
    }

    public Map<String, String> getTags() {
        List<String> aggregated = List.of(this.getAggregatedTags());
        Map<String, String> tags = new HashMap<>();
        for (TagRecord pairing : this.tagHashes) {
            // Get our tags
            Map<String, String> pairingTags = pairing.tags();
            for (String k : pairingTags.keySet()) {
                // check to see it's not already part of our aggregated tags.
                if (!aggregated.contains(k)) {
                    // and now compare it to all of the other tag records, if the value is the same
                    // add it to the list of tags.
                    boolean ubiquitious = true;
                    for (TagRecord compare : this.tagHashes) {
                        Map<String, String> comparedTags = compare.tags();
                        ubiquitious = ubiquitious && comparedTags.containsKey(k) && comparedTags.get(k).equals(pairingTags.get(k));

                        if (!ubiquitious) {
                            break;
                        }
                    }
                    if (ubiquitious) {
                        // we can add it.
                        tags.put(k, pairingTags.get(k));
                    }
                }
            }
        }

        return tags;
    }

    public String[] getAggregatedTags() {
        // Go through our tags and see they exist every where.
        final List<String> presentKeys = Arrays.stream(this.tagHashes)
                .map(TagRecord::tags)
                .flatMap(tags -> tags.keySet().stream())
                .distinct()
                .filter(this::isEverPresent)
                .filter(tag -> !this.filterTags.contains(tag))
                .collect(Collectors.toList());

        logger.info("Identified {} aggregated tags in group {}.", presentKeys.size(), this.id);
        return presentKeys.toArray(new String[0]);
    }

    public boolean isEverPresent(String k) {
        // Go through the children and see if it is everywhere.
        for (TagRecord record : this.tagHashes) {
            if (!record.tags().containsKey(k)) {
                return false;
            }
        }
        return true;
    }

}
