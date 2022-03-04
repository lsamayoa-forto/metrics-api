package com.forto.metrics.api.suggestions;

import com.forto.metrics.api.MetricsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SuggestionsService implements ISuggestionsService {
    private final SuggestionsRepository suggestionsRepository;

    @Autowired
    public SuggestionsService(final SuggestionsRepository suggestionsRepository) {
        this.suggestionsRepository = suggestionsRepository;
    }

    @Override
    public String[] getSuggestions(SuggestRequest request) throws MetricsException {
        if (request.getQuery() == null || request.getQuery().length() == 0) {
            // TODO: Add actual request validation
            // this ain't gonna work.
            return new String[0];
        }
        return suggestionsRepository.getSuggestions(request.getType(), request.getQuery(), request.getMaximum());
    }

    @Override
    public LookupResponse performLookup(String query, long maximum) throws MetricsException {
        return this.suggestionsRepository.performLookup(query, maximum);
    }
}
