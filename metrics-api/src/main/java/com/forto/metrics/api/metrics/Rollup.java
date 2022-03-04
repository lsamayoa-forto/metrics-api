package com.forto.metrics.api.metrics;

import lombok.Getter;

public enum Rollup {

    Raw("ROLLUP_RAW"),
    NoFallback("ROLLUP_NOFALLBACK"),
    Fallback("ROLLUP_FALLBACK"),
    FallbackRaw("ROLLUP_FALLBACK_RAW");

    Rollup(String name) {
        this.name = name;
    }

    @Getter
    private final String name;
}
