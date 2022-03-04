package com.forto.metrics.api.tags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TagFinder {
    private static final Logger logger = LoggerFactory.getLogger(TagFinder.class);

    private final List<TagRecord> tagRecords;

    public TagFinder(final List<TagRecord> tagRecords) {
        this.tagRecords = tagRecords;
    }

    public TagGroup[] findTagsContaining(Map<String, String> tags) {
        // Grab the different groups with filter applied - could make several groups.
        // or not. 
        List<Map<String, String>> rawGroups = this.applyLiteralOr(tags);
        logger.debug("Found {} different groups from literalOr filter.", rawGroups.size());
        rawGroups.addAll(this.applyWildcard(tags));
        logger.debug("Found {} total groups after applying wildcard filter.", rawGroups.size());
        List<String> tagsFromFilter = new ArrayList<>();
        // If it's nothing just add the supplied.
        if (rawGroups.size() == 0) { // means we didn't have any filters.
            rawGroups.add(tags);
        }
        List<TagGroup> tagGroups = new ArrayList<>();
        // We need a distinction of tags, those that should be aggregated together
        // and those that should not.
        // If we are using host=(web01|web02), we find two tags host=web01, and host=web02 and they are grouped.
        int i = 1;
        for (Map<String, String> group : rawGroups) {
            List<TagRecord> tagHashes = new ArrayList<>();
            for (TagRecord record : this.tagRecords) {
                if (record.containsTags(group)) {
                    tagHashes.add(record);
                }
            }
            // Add the keys as filter excludes
            for (String k : group.keySet()) {
                if (!tagsFromFilter.contains(k)) {
                    tagsFromFilter.add(k);
                }
            }

            // Let's add the group.
            final TagGroup tagGroup = new TagGroup(i, tagHashes.toArray(new TagRecord[0]), tagsFromFilter);
            if(tagHashes.size() > 0) {
                tagGroups.add(tagGroup);
                i++;
            }
        }
        logger.info("Identified {} separate groupbys from supplied tags", tagGroups.size());
        // Send it back as an array.
        return tagGroups.toArray(new TagGroup[0]);
    }

    private List<Map<String, String>> applyWildcard(Map<String, String> tags) {
        List<Map<String, String>> groupedTags = new ArrayList<>();
        // We check all the tag records.
        for(TagRecord tagRecord : this.tagRecords) {
            // Now check wildcarded tags
            boolean canAdd = true;
            boolean hasWildcards = false;
            // Now get our none prefiltered.
            Map<String, String> preFiltered = this.toFilteredTags(tags);
            // Now check the list of tags.
            for(String k : tags.keySet()) {
                String v = tags.get(k);
                if(v.contains("*")) {
                    hasWildcards = true;
                    // Create the regex.
                    String recordValue = tagRecord.tags().get(k);
                    Pattern wildcard = Pattern.compile(v.replace("*", "(\\w|\\d|\\-|\\.|\\/|\\s)+"));
                    canAdd = canAdd && tagRecord.tags().containsKey(k) && wildcard.matcher(recordValue).matches();
                    // put the prefiltered on.
                    preFiltered.put(k, recordValue);
                }
            }
            if(hasWildcards && canAdd && !groupedTags.contains(preFiltered)) {
                groupedTags.add(preFiltered);
            }
        }
        /*
        for (String k : tags.keySet()) {
            String v = tags.get(k);
            if (v.contains("*")) {
                // Replace with regex pattern.
                Pattern wildcard = Pattern.compile(v.replace("*", "(\\w|\\d|\\-|\\.|\\/|\\s)+"));
                // Now I need to look for keys that match the pattern.
                for (TagRecord r : this.tagRecords) {
                    String recordValue = r.tags().get(k);
                    if (r.tags().containsKey(k) && wildcard.matcher(recordValue).matches()) {
                        // then we add this guy.
                        Map<String, String> filtered = this.toFilteredTags(tags);
                        filtered.put(k, recordValue);
                        if (!groupedTags.contains(filtered)) {
                            groupedTags.add(filtered);
                        }
                    }
                }
            }
        }
        // How do we then combine * tags?
        // For each tag record, i need to find all the wildcarded tags.
        // and see if each tag record applies.
         */
        return groupedTags;
    }

    private List<Map<String, String>> applyLiteralOr(Map<String, String> tags) {
        List<Map<String, String>> groupedTags = new ArrayList<>();
        for (String k : tags.keySet()) {
            // If the value contains a pipe
            String v = tags.get(k);
            if (v.contains("|")) {
                // Split it
                v = v.substring(v.indexOf('(') + 1, v.indexOf(')'));
                String[] parts = v.split("\\|");
                // Each part then is a valid tag
                for (String part : parts) {
                    // We need a filtered list.
                    Map<String, String> filtered = this.toFilteredTags(tags);
                    filtered.put(k, part);
                    groupedTags.add(filtered);
                }
            }
        }
        return groupedTags;
    }

    /*
     * e.g: we have a tag list of
     * dc=lax&host=(web01|web02)
     * we would expect to filter it to:
     * Two groups:
     * dc=lax,host=web01
     * dc=lax,host=web02
     */
    private Map<String, String> toFilteredTags(Map<String, String> tags) {
        Map<String, String> filtered = new HashMap<>();
        for (String k : tags.keySet()) {
            // if it is a filtered tag.
            String v = tags.get(k);
            if (!v.contains("|") && !v.contains("*")) {
                // Add the tags in.
                filtered.put(k, v);
            }
        }

        return filtered;
    }
}
