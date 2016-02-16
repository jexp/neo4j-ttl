package org.neo4j.ttl.extension.extension;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.kernel.lifecycle.Lifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * @author mh
 * @since 12.02.16
 */

public class TTLExtensionCypher implements Lifecycle {
    private final static Logger logger = Logger.getLogger(TTLExtensionCypher.class.getName());
    public static final int BATCH_SIZE = 10000;
    private ScheduledExecutorService executor;

    private final GraphDatabaseService gds;

    private final Long schedule;
    private final String label;
    private final String property;

    private Runnable deleter;

    public TTLExtensionCypher(GraphDatabaseService gds, Long schedule, String label, String property) {
        this.gds = gds;
        this.schedule = schedule;
        this.label = label;
        this.property = property;
    }

    @Override
    public void init() throws Throwable {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() throws Throwable {

        executor.submit(() -> {
            gds.execute(format("CREATE INDEX ON :`%s`(`%s`)", label, property));
            gds.schema().awaitIndexesOnline(schedule, TimeUnit.MILLISECONDS);
        });

        String deleteStatement = format("MATCH (n:`%s`) WHERE n.`%s` <= timestamp() WITH n LIMIT %d DETACH DELETE n RETURN count(*) as c",
                label, property, BATCH_SIZE);

        deleter = () -> {
            ResourceIterator<Number> result = gds.execute(deleteStatement).columnAs("c");
            if (result.hasNext()) {
                int deleted = result.next().intValue();
                if (deleted > 0) logger.info("Expired " + deleted + " nodes.");
                if (deleted == BATCH_SIZE) executor.submit(deleter);
            }
        };
        executor.scheduleAtFixedRate(deleter, schedule * 5, schedule, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() throws Throwable {
        executor.shutdown();
    }

    @Override
    public void shutdown() throws Throwable {
        executor.awaitTermination(schedule, TimeUnit.MILLISECONDS);
    }
}