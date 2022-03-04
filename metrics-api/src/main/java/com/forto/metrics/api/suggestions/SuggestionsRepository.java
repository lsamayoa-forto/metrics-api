package com.forto.metrics.api.suggestions;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.shaded.guava.common.base.Stopwatch;
import com.forto.metrics.api.MetricsException;
import com.forto.metrics.api.cql.AsyncRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class SuggestionsRepository {
    private static final String METRICS_SUGGEST = "SELECT metric_name FROM ts.dimensions " +
            "WHERE metric_name LIKE ?";

    private static final String KEYS_SUGGEST = "SELECT DISTINCT key FROM ts.dimensions";

    private static final String VALUES_SUGGEST = "SELECT value FROM ts.dimensions " +
            "WHERE value LIKE ?";

    private static final String TAGS_BY_METRIC = "SELECT key FROM ts.dimensions WHERE metric_name = ?";

    private static final String VALUES_BY_METRIC_KEY = "SELECT metric_name, key, value FROM ts.dimensions " +
            "WHERE key = ? AND metric_name = ?";

    private static final String METRIC_SEARCH = "SELECT metric_name, key, value FROM ts.dimensions " +
            "WHERE metric_name = ?";

    private final AsyncRepository asyncRepository;

    @Autowired
    public SuggestionsRepository(final AsyncRepository asyncRepository) {
        this.asyncRepository = asyncRepository;
    }

    public LookupResponse performLookup(String query, long maximum) throws MetricsException {
        final Stopwatch watch = Stopwatch.createStarted();
        final String[] terms = splitTerms(query);
        final String metric = terms[1];
        final String key = terms[0];
        final SimpleStatement statement = key != null
                ? SimpleStatement.newInstance(VALUES_BY_METRIC_KEY, key, metric)
                : SimpleStatement.newInstance(METRIC_SEARCH, metric);
        final CompletionStage<AsyncResultSet> queryStage = this.asyncRepository.executeCqlAsync(statement);
        final CompletionStage<List<LookupResult>> readStage = queryStage.thenApply((resultSet -> {
            final List<LookupResult> results = new ArrayList<>();
            Row row;
            while((row = resultSet.one()) != null) {
                final String value = row.getString(2);
                final String m = row.getString(0);
                final String k = row.getString(1);
                final LookupResult result = new LookupResult(m, k, value);
                results.add(result);
                if(results.size() == maximum) {
                    break;
                }
            }
            return results;
        }));
        try {
            watch.stop();
            final List<LookupResult> lookups = readStage.toCompletableFuture().get();
            final LookupResponse response = new LookupResponse(
                    metric,
                    (int)maximum,
                    (int)watch.elapsed().toMillis(),
                    key == null ? new ArrayList<>() : List.of(new TagInfo(key, "*")),
                    lookups
            );
            return response;
        }
        catch (InterruptedException | ExecutionException ex) {
            throw new MetricsException(2, "Could not perform lookup query.", ex);
        }
    }

    public String[] getSuggestions(SuggestQueryType suggestQueryType, String query, long maximum) throws MetricsException {
        final SimpleStatement statement = getSuggestionCql(suggestQueryType, query);

        final CompletionStage<AsyncResultSet> queryStage = this.asyncRepository.executeCqlAsync(statement);
        final CompletionStage<String[]> readStage = queryStage.thenApply((resultSet -> {
            final Set<String> suggestions = new HashSet<>();
            Row row;
            while ((row = resultSet.one()) != null) {
                String item = row.getString(0);
                if (!suggestions.contains(item)) {
                    if(suggestQueryType == SuggestQueryType.TagsByMetric ||
                            suggestQueryType == SuggestQueryType.ValuesByMetricTag || Pattern.matches(
                                    query.replace("*", "\\w*((\\.|\\_|\\/|\\-|\\,)\\w+)*"), item)) {
                        suggestions.add(item);
                        // when do we break out?
                        if (suggestions.size() == maximum) {
                            break;
                        }
                    }
                }
            }
            return suggestions.toArray(String[]::new);
        }));

        try {
            return readStage.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new MetricsException(2, "Asynchronous operation failed", ex);
        }
    }

    private SimpleStatement getSuggestionCql(SuggestQueryType queryType, String query) throws MetricsException {
        final String baseQuery = getBaseQuery(queryType);
        String term = query.replace('*', '%');
        if ((queryType == SuggestQueryType.TagValue || queryType == SuggestQueryType.Metrics) && term.indexOf('%') == -1) {
            term = term.concat("%");
        }
        switch(queryType) {
            case TagKey:
                return SimpleStatement.newInstance(baseQuery);
            case ValuesByMetricTag:
                return SimpleStatement.newInstance(baseQuery, splitTerms(term));
            default:
                return SimpleStatement.newInstance(baseQuery, term);
        }
    }

    private static String[] splitTerms(String term) throws MetricsException {
        final Pattern p = Pattern.compile(SuggestQueryType.REGEX_TEXT);
        final Matcher m = p.matcher(term);
        if (m.matches()) {
            final String metric = m.group("metricName");
            final String key = m.group("tagKey");

            return new String[] { key, metric };
        }
        throw new MetricsException(14, "Could not read metric/tag query");
    }

    private String getBaseQuery(SuggestQueryType queryType) {
        return switch (queryType) {
            case Metrics -> METRICS_SUGGEST;
            case TagKey -> KEYS_SUGGEST;
            case TagValue -> VALUES_SUGGEST;
            case TagsByMetric -> TAGS_BY_METRIC;
            case ValuesByMetricTag -> VALUES_BY_METRIC_KEY;
        };
    }
}
