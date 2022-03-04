package com.forto.metrics.metricscollector.cassandra;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.forto.metrics.metricscollector.MetricTagsHashCache;
import com.forto.metrics.metricscollector.MetricsWriter;
import com.forto.metrics.metricscollector.TimeFunctions;
import com.forto.metrics.metricscollector.Metric;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CassandraMetricsWriter implements MetricsWriter {

    private static final int TTL_TIME = 1 * 60 * 60 * 24 * 90;
    private static final String INSERT_METRIC = "INSERT INTO ts.metrics(metric_name, day_bucket, time, tag_hash, value) " +
            "VALUES(?, ?, ?, ?, ?) USING TTL " + TTL_TIME;

    private static final String INSERT_METRIC_METADATA = "INSERT INTO ts.tags2(metric_name, tag_hash, tags) " +
            "VALUES(?, ?, ?)";

    private static final String INSERT_METRIC_DIMENSIONS = "INSERT INTO ts.dimensions(key, metric_name, value) " +
            "VALUES(?, ?, ?)";

    private final AsyncCqlTemplate cqlTemplate;
    private final MetricTagsHashCache metricTagsHashCache;
    private final Counter errorCounter = Metrics.counter("metrics-collector.insert-failed");
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CassandraMetricsWriter(final AsyncCqlTemplate cqlTemplate,
                                  final MetricTagsHashCache metricTagsHashCache) {
        this.cqlTemplate = cqlTemplate;
        this.metricTagsHashCache = metricTagsHashCache;
    }

    @Override
    public void writeMetric(final Metric metric) {
        final String tagsHash = generateTagHash(metric);
        final SimpleStatement insertMetricStatement = SimpleStatement.newInstance(INSERT_METRIC,
                metric.getMetricName(),
                metric.getDayBucket(),
                TimeFunctions.getTimeUuid(metric.getTimestamp()),
                tagsHash,
                metric.getValue()
        );
        cqlTemplate.execute(insertMetricStatement).completable()
                .exceptionally(throwable -> {
                    if(throwable != null) {
                        // increment the counter.
                        this.errorCounter.increment();
                        // report on it.
                        this.logger.error("Could not save metric " + metric.getMetricName(), throwable);
                    }
                    return null;
                })
                .join();
    }

    private CompletableFuture<Void> saveTags(final String metricName,
                                             final Map<String, String> tags,
                                             final String hash) {
        final SimpleStatement insertMetadataStatement = SimpleStatement.newInstance(
                INSERT_METRIC_METADATA,
                metricName,
                hash,
                tags
        );

        final CompletableFuture<Boolean> metricSave = cqlTemplate.execute(insertMetadataStatement).completable();

        final CompletableFuture<?>[] tagSaves = tags.keySet().stream()
                .map(key -> SimpleStatement.newInstance(
                        INSERT_METRIC_DIMENSIONS,
                        key,
                        metricName,
                        tags.get(key)
                ))
                .map(cqlTemplate::execute)
                .map(ListenableFuture::completable)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(CompletableFuture.allOf(tagSaves), metricSave);
    }

    private String generateTagHash(final Metric metric) {
        // Only insert Dimensions if they have not been cached locally
        return metricTagsHashCache.getHash(metric.getMetricName(), metric.getTags(), (newHash) -> {
            saveTags(metric.getMetricName(), metric.getTags(), newHash).join();
        });
    }
}
