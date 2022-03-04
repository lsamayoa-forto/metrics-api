package com.forto.metrics.api.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ChunkAggregator {
    private static final Logger logger = LoggerFactory.getLogger(ChunkAggregator.class);

    private final Collection<ChunkList> sources;

    public ChunkAggregator(Collection<ChunkList> sources) {
        this.sources = sources;
    }

    public ChunkList updateTarget(ChunkList target) {
        // We go through each chunk of the target
        // Now for each of our datasources, let's work out a value.
        for (ChunkList list : this.sources) {
            for (Chunk chunk : list.getChunks()) {
                // Get the chunk for our data source.
                Chunk aggregatedAlready = list.getChunk(chunk.getStartTime());
                // if we didn't find anything don't aggregate that!
                if (aggregatedAlready.hasValue()) {
                    // Add it to our target chunk.
                    target.addSample(aggregatedAlready.getStartTime(),
                            1,
                            aggregatedAlready.getDpValue());
                }
            }
        }
        // finalize the target instance.
        target.complete();
        // Now we are aggregated.
        logger.debug("Total of {} results, over {} sources aggregated via {}",
                target.getCount(),
                this.sources.size(),
                target.getAggregator());
        return target;
    }
}
