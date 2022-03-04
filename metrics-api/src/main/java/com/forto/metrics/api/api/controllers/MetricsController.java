package com.forto.metrics.api.api.controllers;

import com.forto.metrics.api.MetricsException;
import com.forto.metrics.api.metrics.AggregationType;
import com.forto.metrics.api.metrics.ITimeSeriesService;
import com.forto.metrics.api.metrics.MetricRequest;
import com.forto.metrics.api.metrics.MetricsResponse;
import com.forto.metrics.api.suggestions.ISuggestionsService;
import com.forto.metrics.api.suggestions.LookupResponse;
import com.forto.metrics.api.suggestions.SuggestQueryType;
import com.forto.metrics.api.suggestions.SuggestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class MetricsController {
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    private final ITimeSeriesService service;
    private final ISuggestionsService suggestionsService;

    @Autowired
    public MetricsController(final ITimeSeriesService service,
                             final ISuggestionsService suggestionsService) {
        this.service = service;
        this.suggestionsService = suggestionsService;
    }

    @PostMapping("api/query")
    public MetricsResponse[] executeQuery(@RequestBody MetricRequest request) throws MetricsException {
        // Delegate to the service.
        return this.service.getTimeSeries(request);
    }

    @GetMapping("api/suggest")
    public String[] getSuggestions(@RequestParam String type, @RequestParam String q, @RequestParam int max) throws MetricsException {
        SuggestRequest request = new SuggestRequest(SuggestQueryType.get(type), q, max);
        return this.suggestionsService.getSuggestions(request);
    }

    @GetMapping("api/search/lookup")
    public LookupResponse getSuggestions(@RequestParam String m, @RequestParam int limit) throws MetricsException {
        return this.suggestionsService.performLookup(m, limit);
    }

    @GetMapping("api/aggregators")
    public String[] getAggregators() {
        return AggregationType.getAggregators();
    }

}
