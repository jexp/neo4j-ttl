package org.neo4j.ttl;

import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.api.DataWriteOperations;
import org.neo4j.kernel.api.SchemaWriteOperations;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.InvalidTransactionTypeKernelException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.legacyindex.AutoIndexingKernelException;
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException;
import org.neo4j.kernel.api.exceptions.schema.AlreadyIndexedException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.impl.api.store.RelationshipIterator;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.lifecycle.Lifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author mh
 * @since 12.02.16
 */

public class TTLExtensionMonster implements Lifecycle {
    private final static Logger logger = Logger.getLogger(TTLExtensionMonster.class.getName());
    private ScheduledExecutorService executor;

    private final GraphDatabaseService gds;
    private final ThreadToStatementContextBridge ctx;
    private final IndexDescriptor index;

    private final Long schedule;
    private TTLEventHandler handler;

    public TTLExtensionMonster(GraphDatabaseService gds, Long schedule, String label, String property) {
        this.gds = gds;
        ctx = ((GraphDatabaseAPI) gds).getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);

        this.schedule = schedule;
        this.index = getOrCreateIndex(gds, label, property);
        logger.info("TTL Extension: Running for " + label + " - " + property);
    }

    private IndexDescriptor getOrCreateIndex(GraphDatabaseService gds, String label, String property) {
        try (Transaction tx = gds.beginTx()) {
            SchemaWriteOperations ops = ctx.get().schemaWriteOperations();
            IndexDescriptor index = ops.indexCreate(ops.labelGetForName(label), ops.propertyKeyGetForName(property));
            InternalIndexState state = ops.indexGetState(index);
            while (state == InternalIndexState.POPULATING) {
                Thread.sleep(schedule);
            }
            if (ops.indexGetState(index) == InternalIndexState.FAILED) {
                String msg = ops.indexGetFailure(index);
                throw new RuntimeException("Error creating index on :" + label + "(" + property + ") " + msg);
            }
            tx.success();
            return index;
        } catch (IndexNotFoundKernelException | InterruptedException | AlreadyConstrainedException | AlreadyIndexedException | InvalidTransactionTypeKernelException e) {
            throw new RuntimeException("Error creating index on :" + label + "(" + property + ") ", e);
        }
    }

    @Override
    public void init() throws Throwable {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Deleter(), 5 * schedule, schedule, TimeUnit.MILLISECONDS);

        handler = new TTLEventHandler(gds, index);
        gds.registerTransactionEventHandler(handler);
        logger.info("Connecting to TTL");
    }

    @Override
    public void start() throws Throwable {
    }

    @Override
    public void stop() throws Throwable {
        executor.shutdown();
    }

    @Override
    public void shutdown() throws Throwable {
        executor.awaitTermination(schedule, TimeUnit.MILLISECONDS);
        gds.unregisterTransactionEventHandler(handler);
        logger.info("TTL Extension shut down");
    }

    private class Deleter implements Runnable {
        @Override
        public void run() {
            try (Transaction tx = gds.beginTx()) {
                DataWriteOperations ops = ctx.get().dataWriteOperations();
                PrimitiveLongIterator nodeIds = ops.nodesGetFromIndexRangeSeekByNumber(index, null, false, System.currentTimeMillis(), true);
                while (nodeIds.hasNext()) deleteNode(ops, nodeIds.next());
                tx.success();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting node");
            }
        }

        private void deleteNode(DataWriteOperations ops, long nodeId) throws Exception {
            RelationshipIterator rels = ops.nodeGetRelationships(nodeId, Direction.BOTH);
            while (rels.hasNext()) {
                ops.relationshipDelete(rels.next());
            }
            ops.nodeDelete(nodeId);
        }
    }
}
