package com.forto.metrics.api.tags;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.shaded.guava.common.base.Stopwatch;
import com.forto.metrics.api.MetricsSender;
import com.forto.metrics.api.MetricsException;
import com.forto.metrics.api.cql.IAsyncRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@Repository
public class TagsRepository implements ITagsRepository {

    private static final String TAGS_FETCH = "SELECT metric_name, tag_hash, tags FROM ts.tags2 WHERE metric_name = ?";
    private static final Logger logger = LoggerFactory.getLogger(TagsRepository.class);

    private final IAsyncRepository asyncRepository;

    @Autowired
    public TagsRepository(final IAsyncRepository asyncRepository) {
        this.asyncRepository = asyncRepository;
    }

    @Override
    public TagFinder getTags(String metric) throws MetricsException {
        // TODO: should we be returning tags using the index and the dimension kvp from the query?
        Stopwatch watch = Stopwatch.createStarted();

        SimpleStatement statement = SimpleStatement.newInstance(
                TAGS_FETCH,
                metric
        );

        final CompletionStage<AsyncResultSet> queryStage = this.asyncRepository.executeCqlAsync(statement);
        final CompletionStage<TagFinder> mapStage = queryStage.thenApply((rs) -> {
            int count = 0;
            // So go through the result set.
            Row row ;
            final List<TagRecord> tagRecords = new ArrayList<>();
            while ((row = rs.one()) != null) {
                count++;
                // Grab the hash.
                tagRecords.add(new TagRecord(
                        row.getString(1),
                        row.getMap(2, String.class, String.class)));
            }
            // Send our count of records.
            MetricsSender.send("metrics.api.query.rows", count, "query:tags2", "metric:" + metric);
            logger.info("Found {} tag records for the metric {}.", count, metric);

            return new TagFinder(tagRecords);
        });

        try {
            return mapStage.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new MetricsException(1, "Asynchronous operation failed.", ex);
        } finally {
            MetricsSender.send("metrics.api.query.latency", (int)watch.elapsed().toMillis(), "query:ts.tags2", "metric:" + metric);
            logger.info("Tags query completed in {}ms for metric {}.", (int)watch.elapsed().toMillis(), metric);
        }
    }
}
