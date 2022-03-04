package com.forto.metrics.api.suggestions;

import com.forto.metrics.api.MetricsException;

public interface ISuggestionsService {
    String[] getSuggestions(SuggestRequest request) throws MetricsException;

    LookupResponse performLookup(String query, long maximum) throws MetricsException;
}
