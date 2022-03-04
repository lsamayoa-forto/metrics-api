package com.forto.metrics.api.metrics;

import com.forto.metrics.api.MetricsException;

public interface ITimeSeriesService {

    MetricsResponse[] getTimeSeries(MetricRequest request) throws MetricsException;

}
