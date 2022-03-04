package com.forto.metrics.api.tags;

import com.forto.metrics.api.MetricsException;

public interface ITagsRepository {

    TagFinder getTags(String metric) throws MetricsException;

}
