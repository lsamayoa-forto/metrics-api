package com.forto.metrics.api.metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparingLong;

public class ChunkList {

    private final long start;
    private final long increment;
    private final boolean isDownsampling;
    private final AggregationType aggregationType;

    private List<Chunk> chunks = new ArrayList<>();
    private Map<Long, Chunk> mappedChunks = new HashMap<>();

    public ChunkList(long start, long end, Downsample downsample) {
        this(start, end, downsample.getNextChunk(), downsample.getAggregationType());
    }

    public ChunkList(long start, long end, AggregationType type) {
        this(start, end, -1, type);
    }

    public ChunkList(long start, long end, long increment, AggregationType type) {
        this.start = start;
        this.increment = increment;
        this.aggregationType = type;
        this.isDownsampling = !(increment == -1);
        if (isDownsampling) {
            this.preallocate(start, end, increment, type);
        }
    }

    public int getCount() {
        return this.chunks.size();
    }

    public Chunk[] getChunks() {
        return this.chunks.toArray(new Chunk[0]);
    }

    public Chunk getChunk(long timestamp) {
        if (this.isDownsampling) {
            return this.downsampledLookup(timestamp);
        }
        return this.mappedChunks.get(timestamp);
    }

    private void preallocate(long start, long end, long increment, AggregationType aggregationType) {
        // get the millisecond increments.
        for (long i = start; i <= end; i += increment) {
            // Create the chunk.
            Chunk chunk = new Chunk(Instant.ofEpochMilli(i), increment, aggregationType);
            this.chunks.add(chunk);
        }
    }

    public void addSample(long ts, long count, double value) {
        // We find or create the respective chunk the timestamp belongs to...
        final Chunk chunk = findOrCreateChunk(this.isDownsampling, ts);
        // ...then increment the chunk.
        // if there is more than one value at a particular point in time then we simply aggregate those two
        // together using the supplied aggregation method through the chunk.
        chunk.increment(value, count);
    }

    public void complete() {
        this.chunks.sort(comparingLong(Chunk::getStartTime));
        // Apply post aggregation after sort.
        if (this.aggregationType == AggregationType.CumulativeSum) {
            int total = 0;
            for (Chunk c : this.chunks) {
                if (c.hasValue()) {
                    // Apply to total.
                    total += c.getDpValue();
                }
                // Apply it.
                // horrific hack for cumulative sum
                c.setDpValue(total);
            }
        }
    }

    public Map<String, Double> getMappedChunks() {
        final Map<String, Double> result = new HashMap<>();
        for (Chunk chunk : this.chunks) {
            if (chunk.hasValue()) {
                result.put(
                        chunk.getDpLabel(),
                        chunk.getDpValue()
                );
            }
        }
        return result;
    }

    /**
     * Looks up in the chunk list the chunk correspoding for an specific timestamp.
     * It has the ability to
     *
     * @param isDownsampling should
     * @param timestamp
     * @return
     */
    private Chunk findOrCreateChunk(boolean isDownsampling, long timestamp) {
        // When downsampling we preallocate the chunks so no need to create new ones
        if (isDownsampling) {
            // We need to find the appropriate chunk, so we normalize the value.
            // i.e. we have a chunks 0 - 4999, 5000 - 9999, value of 6000 sits in the 2nd chunk.
            // 6000 / 5000 = 1, chunks[1].
            // Above works if we are downsampling.
            return this.downsampledLookup(timestamp);
        }

        // Create a new chunk if we cannot find it
        return this.mappedChunks.computeIfAbsent(timestamp, key -> {
            final Chunk chunk = new Chunk(Instant.ofEpochMilli(timestamp), 0, this.aggregationType);
            this.chunks.add(chunk);
            return chunk;
        });
    }

    /**
     * Performs a lookup by normalizing the supplied timestamp and cutting off the remainder.
     * e.g.: start = 1623182400000 (8th June 2021, 20:00:00)
     * supplied = 1623182533000 (8th June 2021, 20:02:13)
     * <p>
     * normalized = 1623182533000 - 1623182400000 = 133000.
     * normalized / increment = 26.6, or 26th index, chunks[26]
     * retrives chunk: 1623182533000
     * <p>
     * ts = 1623182442000 - 1623182400000 = 42000 / 5000 = 8.4, chunks[8]
     * 0,1623182400000
     * 1,1623182405000
     * 2,1623182410000
     * 3,1623182415000
     * 4,1623182420000
     * 5,1623182425000
     * 6,1623182430000
     * 7,1623182435000
     * 8,1623182440000
     */
    private Chunk downsampledLookup(long ts) {
        // we normalize it by removing the start
        long chunked = ts - this.start;
        long index = chunked / this.increment; // will cut off the remainder.

        if (index >= this.chunks.size()) {
            // Out of range. should never happen.
            throw new ArrayIndexOutOfBoundsException("Invalid calculation - index " + Long.valueOf(index).toString());
        }
        // retrieve it.
        return this.chunks.get((int) index);
    }

    public AggregationType getAggregator() {
        return this.aggregationType;
    }

}
