package com.forto.metrics.api.cql;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface IAsyncRepository {

    CompletionStage<AsyncResultSet> executeCqlAsync(SimpleStatement statement);

    CompletableFuture<Boolean> execute(SimpleStatement statement);
}
