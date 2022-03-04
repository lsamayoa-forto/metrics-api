package com.forto.metrics.metricscollector;

import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;

@Configuration
public class CommonTagsProvider {

    @Value("${management.metrics.tags.env}")
    private String environment;

    @PostConstruct
    public void onInit() {
        Metrics.globalRegistry
                .config()
                .commonTags("env", this.environment);
    }
}
