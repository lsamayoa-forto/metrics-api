package com.forto.metrics.api.cql;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Repository
public class AsyncRepository implements IAsyncRepository {

    private final CqlSession session;
    private final AsyncCqlTemplate template;

    @Autowired
    public AsyncRepository(final CqlSession session) {
        this.session = session;
        this.template = new AsyncCqlTemplate(session);
    }

    @Override
    public CompletionStage<AsyncResultSet> executeCqlAsync(SimpleStatement statement) {
        CompletionStage<AsyncResultSet> resultSetStage = session.executeAsync(statement);
        return resultSetStage;
    }

    @Override
    public CompletableFuture<Boolean> execute(SimpleStatement statement) {
        return this.template.execute(statement).completable();
    }
}
