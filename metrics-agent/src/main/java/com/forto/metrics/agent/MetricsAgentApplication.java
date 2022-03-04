package com.forto.metrics.agent;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.PublishSubscribeSpec;
import org.springframework.integration.ip.dsl.Udp;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SpringBootApplication
public class MetricsAgentApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsAgentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MetricsAgentApplication.class, args);
    }

    @Value("${server.udp.port}")
    private int udpPort;

    @Value("${udp.proxy.enabled}")
    private boolean proxyEnabled;

    @Value("${udp.proxy.host}")
    private String proxyHostname;

    @Value("${udp.proxy.port}")
    private int proxyPort;

    @Value("${kinesis.stream.name}")
    private String kinesisStreamName;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AmazonKinesis amazonKinesis() {
        return AmazonKinesisClientBuilder
                .standard()
                .build();
    }

    @Bean
    public IntegrationFlow processIncomingUdpMetricMessage(final Clock clock,
                                                           final AmazonKinesis kinesis) {

        return IntegrationFlows
                .from(Udp.inboundAdapter(udpPort))
                .publishSubscribeChannel(getPublishSubscribeSpecConsumer(clock, kinesis))
                .get();
    }

    private Consumer<PublishSubscribeSpec> getPublishSubscribeSpecConsumer(
            final Clock clock,
            final AmazonKinesis kinesis
    ) {
        return subscription ->
                subscription
                        .subscribe(subflow -> subflow
                                .filter((message) -> proxyEnabled)
                                .handle(Udp.outboundAdapter(proxyHostname, proxyPort)))
                        .subscribe(subflow -> subflow
                                .transform(payload -> new String((byte[]) payload))
                                .transform((String payload) -> payload.split("\\r?\\n"))
                                .split()
                                .transform((String payload) -> Metric.parseString(payload, clock.instant().toEpochMilli()))
                                .resequence()
                                .aggregate()
                                .transform((List<Metric> metrics) -> {
                                    final List<PutRecordsRequestEntry> recordsEntries = metrics
                                            .stream()
                                            .map(metric -> {
                                                try {
                                                    final PutRecordsRequestEntry entry = new PutRecordsRequestEntry();
                                                    entry.setPartitionKey(String.valueOf(metric.getTimestamp()));
                                                    entry.setData(ByteBuffer.wrap(OBJECT_MAPPER.writeValueAsBytes(metric)));
                                                    return entry;
                                                } catch (JsonProcessingException e) {
                                                    LOGGER.error("There was an error trying to write a metric {} as json", metric.getMetricName(), e);
                                                }
                                                return null;
                                            })
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
                                    final PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
                                    putRecordsRequest.setRecords(recordsEntries);
                                    putRecordsRequest.setStreamName(kinesisStreamName);
                                    return putRecordsRequest;
                                })
                                .filter((PutRecordsRequest message) -> !message.getRecords().isEmpty())
                                .handle((message) -> kinesis.putRecords((PutRecordsRequest) message.getPayload())));
    }

}
