package org.neo4j.ttl;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.kernel.api.index.IndexDescriptor;

/**
 * @author mh
 * @since 12.02.16
 */
public class TTLEventHandler implements TransactionEventHandler{

    public TTLEventHandler(GraphDatabaseService gds, IndexDescriptor index) {
    }

    @Override
    public Object beforeCommit(TransactionData transactionData) throws Exception {
        return null;
    }

    @Override
    public void afterCommit(TransactionData transactionData, Object o) {

    }

    @Override
    public void afterRollback(TransactionData transactionData, Object o) {

    }
}
