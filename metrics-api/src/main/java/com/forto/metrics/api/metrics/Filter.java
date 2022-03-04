package com.forto.metrics.api.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Filter {

    private FilterType type;
    private String tagk;
    private String filter;
    private boolean groupBy;
}
