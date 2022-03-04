package com.forto.metrics.api.metrics;

import com.datastax.oss.driver.shaded.guava.common.base.Stopwatch;
import com.forto.metrics.api.MetricsSender;
import com.forto.metrics.api.tags.TagFinder;
import com.forto.metrics.api.tags.TagGroup;
import com.forto.metrics.api.MetricsException;
import com.forto.metrics.api.tags.ITagsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class TimeSeriesService implements ITimeSeriesService {
    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesService.class);

    private final ITagsRepository tagsRepository;
    private final IMetricsRepository metricsService;

    @Autowired
    public TimeSeriesService(final ITagsRepository tagsRepository,
                             final IMetricsRepository finder) {
        this.tagsRepository = tagsRepository;
        this.metricsService = finder;
    }

    /*
     * For each metric / tags (all of them) we get a query.
     * For ones where they are the same, that's where aggregated tags comes in, and we use the aggregator.
     * "Timeseries" is a metric_name / tags combination
     * So imagine if we stored records of type:
     * TS1 =        sys.cpu     { 'dc': 'dal', 'host': 'web01' }
     * TS2 =        sys.cpu     { 'dc': 'dal', 'host': 'web02' }
     * TS3 =        sys.cpu     { 'dc': 'dal', 'host': 'web03' }
     * TS4 =        sys.cpu     { 'host': 'web01' }
     * TS5 =        sys.cpu     { 'host': 'web01', 'user': 'ctxpjc' }
     * TS6 =        sys.cpu     { 'dc': 'lax', 'host': 'web01' }
     * TS7 =        sys.cpu     { 'dc': 'lax', 'host': 'web02' }
     *
     * If query is sys.cpu + host=web01. We take EVERY tag that includes web01, which is
     * 4 series aggregated into one result.
     * TS1, TS4, TS5, TS6
     *
     * If query is sys.cpu + host=*, dc=dal
     * 3 results:
     * TS1
     * TS2
     * TS3
     *
     * If query is sys.cpu + host=web01, dc=dal
     * TS1
     *
     * If query is sys.cpu + dc=dal|lax
     * 2 results,
     * TS1, TS2, TS3
     * TS6, TS7
     */
    @Override
    public MetricsResponse[] getTimeSeries(MetricRequest request) throws MetricsException {
        // TODO: Why not use Bean Validation from Spring
        TimeSeriesRequestValidator validationService = new TimeSeriesRequestValidator(request);
        if (!validationService.isValid()) {
            return new MetricsResponse[0];
        }
        List<MetricsResponse> metricsResponses = new ArrayList<>();
        List<CompletableFuture<MetricsResponse>> futures = new ArrayList<>();
        // start timing.
        Stopwatch stopwatch = Stopwatch.createStarted();
        int totalQueries = 0;
        for (MetricQuery query : request.getQueries()) {
            // We need to understand how many tags contain our query tags.-*
            // We will do basic initially. Get the full list of tags for the metric.
            TagFinder tagFinder = this.tagsRepository.getTags(query.getMetric());
            // Go through our query tags
            TagGroup[] tagGroups = tagFinder.findTagsContaining(query.getTags());

            // Then for each group we should run a series.
            for (TagGroup group : tagGroups) {
                // We need to use the array of tags to create each timeseries associated with them.
                // Each tag group is now a series, we have grouped by already.
                totalQueries++;
                futures.add(this.metricsService.getSeriesAsync(group, request, query));
            }
        }
        // Hold it...
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<MetricsResponse> future : futures) {
            try {
                MetricsResponse metricsResponse = future.get();
                metricsResponses.add(metricsResponse);
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace(System.err);
                throw new MetricsException(10, "Could not read result from future", ex);
            }
        }
        // schtop it.
        stopwatch.stop();
        // Send our metric.
        MetricsSender.send("metrics.api.endpoint_latency",
                (int) stopwatch.elapsed().toMillis(),
                "method:gettimeseries",
                "queries:" + totalQueries,
                "daybuckets:" + request.getDayBucketRange().length);
        // Add to the log.
        this.logger.info("TimeSeries completed in {}ms, with {} queries", stopwatch.elapsed().toMillis(), totalQueries);

        // Send back our list of results.
        return metricsResponses.toArray(new MetricsResponse[0]);
    }
}
