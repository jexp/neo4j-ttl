package org.neo4j.ttl;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.PerformsWriteOperations;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Resource;

import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 12.02.16
 */

public class TTLProceduresAnnotated {

    @Resource
    private GraphDatabaseService gds;

    @Procedure
    @RunAt(RunAt.Stage.Start)
    @PerformsWriteOperations
    public void init() {
        gds.execute("CREATE INDEX ON :Timed(ttl)");
    }

    @Procedure
    @RunAt(delay = 60,  repeat = 60, unit = TimeUnit.SECONDS)
    @PerformsWriteOperations
    public void expire() {
        gds.execute("MATCH (n:Timed) WITH n LIMIT 10000 WHERE n.ttl <= timestamp() DETACH DELETE n");
    }

}
