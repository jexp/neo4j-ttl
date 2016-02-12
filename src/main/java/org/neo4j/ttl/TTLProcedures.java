package org.neo4j.ttl;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Resource;
import org.neo4j.procedure.PerformsWriteOperations;
import org.neo4j.procedure.Procedure;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 12.02.16
 */

public class TTLProcedures {

    @Resource
    private GraphDatabaseService gds;
    @Resource
    private ScheduledExecutorService executor;

    @Procedure
    @PerformsWriteOperations
    public void start() {
        gds.execute("CREATE INDEX ON :Timed(ttl)");
        executor.scheduleAtFixedRate(() -> {
            gds.execute("MATCH (n:Timed) WITH n LIMIT 10000 WHERE n.ttl <= timestamp() DETACH DELETE n");
        }, 60,60,TimeUnit.SECONDS);
    }
}
