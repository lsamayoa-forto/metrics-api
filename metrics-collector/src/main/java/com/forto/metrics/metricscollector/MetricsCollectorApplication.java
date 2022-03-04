package com.forto.metrics.metricscollector;

import com.datastax.oss.driver.api.core.CqlSession;
import com.forto.metrics.metricscollector.cassandra.CassandraMetricsWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;

import java.util.function.Consumer;

@SpringBootApplication
public class MetricsCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricsCollectorApplication.class, args);
    }

    @Bean
    public MetricTagsHashCache metricHashCache() {
        return new MetricTagsHashCache();
    }

    @Bean
    public MetricsWriter metricsWriter(final CqlSession session,
                                       final MetricTagsHashCache metricTagsHashCache) {
        return new CassandraMetricsWriter(new AsyncCqlTemplate(session), metricTagsHashCache);
    }

    @Bean
    public Consumer<Metric> consumer(final MetricsWriter metricsWriter) {
        return metricsWriter::writeMetric;
    }
}
