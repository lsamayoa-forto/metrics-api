package com.forto.metrics.api.metrics;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.forto.metrics.api.cql.IAsyncRepository;
import com.forto.metrics.api.tags.TagGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service
public class MetricsRepository implements IMetricsRepository {
    private static final String FETCH_WITH_IN = "SELECT metric_name, day_bucket, tag_hash, dateOf(time), value " +
            "FROM ts.metrics " +
            "WHERE metric_name = ? " +
            "AND day_bucket = ? " +
            "AND tag_hash IN ? " +
            "AND time >= minTimeUuid(?) " +
            "AND time <= maxTimeUuid(?)";

    private static final String MULTI_DAY_FETCH = "SELECT metric_name, day_bucket, tag_hash, dateOf(time), value " +
            "FROM ts.metrics " +
            "WHERE metric_name = ? " +
            "AND day_bucket IN ? " +
            "AND tag_hash IN ? " +
            "AND time >= minTimeUuid(?) " +
            "AND time <= maxTimeUuid(?) ";

    private final IAsyncRepository asyncMetricsRepository;

    @Autowired
    public MetricsRepository(final IAsyncRepository asyncMetricsRepository) {
        this.asyncMetricsRepository = asyncMetricsRepository;
    }

    @Override
    public CompletableFuture<MetricsResponse> getSeriesAsync(final TagGroup group,
                                                             final MetricRequest request,
                                                             final MetricQuery query) {
        final SimpleStatement fetchStatement = buildFetchStatement(request, query, group);
        final CompletionStage<AsyncResultSet> queryStage = this.asyncMetricsRepository.executeCqlAsync(fetchStatement);
        return queryStage.thenCompose(rs -> readTimeSeriesFromResultSet(rs, group, request, query)).toCompletableFuture();
    }

    private CompletionStage<MetricsResponse> readTimeSeriesFromResultSet(
            final AsyncResultSet resultSet,
            final TagGroup tagGroup,
            final MetricRequest request,
            final MetricQuery query
    ) {
        final CompletionStage<Map<String, ChunkList>> readStage = readChunkMapFromResultSet(
                resultSet,
                Collections.emptyMap(),
                request.getStart(),
                request.getEnd(),
                query
        );
        final CompletionStage<Collection<ChunkList>> sourcesStage = readStage.thenApply(Map::values);
        final CompletionStage<ChunkList> aggregationStage = sourcesStage
                .thenApply(sources -> aggregateLists(request.getStart(), request.getEnd(), sources, query.getAggregator()));
        final CompletableFuture<MetricsResponse> responseMappingStage = aggregationStage
                .thenApply(chunkList -> new MetricsResponse(query.getMetric(), tagGroup, chunkList.getMappedChunks()))
                .toCompletableFuture();

        return responseMappingStage;
    }

    private ChunkList aggregateLists(final long start,
                                     final long end,
                                     final Collection<ChunkList> chunkList,
                                     final AggregationType aggregationType) {
        final ChunkList target = new ChunkList(start, end, aggregationType);
        final ChunkAggregator aggregator = new ChunkAggregator(chunkList);
        aggregator.updateTarget(target);
        return target;
    }

    private CompletionStage<Map<String, ChunkList>> readChunkMapFromResultSet(
            final AsyncResultSet resultSet,
            final Map<String, ChunkList> previousChunkMap,
            final long start,
            final long end,
            final MetricQuery query
    ) {
        final Map<String, ChunkList> currentPageChunkMap =
                readChunkListMapFromRows(resultSet.currentPage(), start, end, query);
        final Map<String, ChunkList> combinedChunkMap = combineChunkMaps(
                start,
                end,
                currentPageChunkMap,
                previousChunkMap,
                query
        );
        if (resultSet.hasMorePages()) {
            return resultSet.fetchNextPage().thenCompose(rs ->
                    readChunkMapFromResultSet(rs, combinedChunkMap, start, end, query));
        }
        return CompletableFuture.completedFuture(combinedChunkMap);
    }

    private Map<String, ChunkList> combineChunkMaps(final long start,
                                                    final long end,
                                                    final Map<String, ChunkList> currentChunkMap,
                                                    final Map<String, ChunkList> previousChunkMap,
                                                    final MetricQuery query) {

        final Set<String> keys = new HashSet<>();
        keys.addAll(currentChunkMap.keySet());
        keys.addAll(previousChunkMap.keySet());

        final Map<String, ChunkList> result = new HashMap<>();
        for (final String key : keys) {
            final ChunkList currentChunkList = currentChunkMap.get(key);
            final ChunkList previousChunkList = previousChunkMap.get(key);

            final List<ChunkList> sources = new ArrayList<>();
            if (currentChunkList != null) sources.add(currentChunkList);
            if (previousChunkList != null) sources.add(previousChunkList);

            result.put(key, aggregateLists(start, end, sources, query.getAggregator()));
        }

        return result;
    }

    private Map<String, ChunkList> readChunkListMapFromRows(final Iterable<Row> rows,
                                                            final long start,
                                                            final long end,
                                                            final MetricQuery query) {
        final Map<String, ChunkList> chunkListMap = new HashMap<>();
        for (final Row row : rows) {
            // Extract chunk data from row
            final String hash = row.getString(2);
            final Instant timestamp = row.getInstant(3);
            final double value = row.getFloat(4);

            // Create or find the correct chunk list this chunk belongs to
            final ChunkList groupChunkList = chunkListMap
                    .computeIfAbsent(hash, key -> query.hasDownsample() ?
                            new ChunkList(start, end, query.getTypedDownsample()) :
                            new ChunkList(start, end, query.getAggregator()));
            // Add sample to chunk list
            groupChunkList.addSample(timestamp.toEpochMilli(), 1, value);
        }
        return chunkListMap;
    }

    private SimpleStatement buildFetchStatement(MetricRequest request, MetricQuery query, TagGroup group) {
        String[] hashes = group.getTagHashes();
        int[] buckets = request.getDayBucketRange();
        List<Integer> bucketList = new ArrayList<>();
        for (int b : buckets) {
            bucketList.add(b);
        }
        return buckets.length == 1 ?
                // Build it... and will they come?
                SimpleStatement.newInstance(FETCH_WITH_IN,
                        query.getMetric(),
                        buckets[0],
                        Arrays.asList(hashes),
                        request.getStart(),
                        request.getEnd()
                ) : SimpleStatement.newInstance(MULTI_DAY_FETCH,
                query.getMetric(),
                bucketList,
                Arrays.asList(hashes),
                request.getStart(),
                request.getEnd()
        );
    }
}
