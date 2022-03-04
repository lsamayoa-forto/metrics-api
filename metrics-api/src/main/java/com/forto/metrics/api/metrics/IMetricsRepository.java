package com.forto.metrics.api.metrics;

import com.forto.metrics.api.MetricsException;
import com.forto.metrics.api.tags.TagGroup;

import java.util.concurrent.CompletableFuture;

public interface IMetricsRepository {

    CompletableFuture<MetricsResponse> getSeriesAsync(TagGroup group, MetricRequest request, MetricQuery query) throws MetricsException;

}
