package com.forto.metrics.api.metrics;

import com.forto.metrics.api.MetricsException;

public interface IMetricsService {

    MetricsResponse[] getMetrics(MetricRequest request) throws MetricsException;

}
