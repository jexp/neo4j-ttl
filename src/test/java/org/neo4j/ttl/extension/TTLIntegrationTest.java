package org.neo4j.ttl.extension;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

public class TTLIntegrationTest {

    public static final int SCHEDULE_SECONDS = 1;
    private GraphDatabaseService db;

    @Before
    public void setUp() throws Exception {
        db = new TestGraphDatabaseFactory()
                .newImpermanentDatabaseBuilder()
                .setConfig(config())
                .newGraphDatabase();
    }

    private Map<String, String> config() {
        return stringMap(
                "ttl.label", "Decay","ttl.property","zombie", "ttl.schedule", SCHEDULE_SECONDS + "s");
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
    }

    @Test
    public void testDecayNode() throws Exception {
        long ttl = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(SCHEDULE_SECONDS);
        try (Transaction tx = db.beginTx()) {
            Node node = db.createNode(Label.label("Decay"));
            System.out.println("ttl = " + ttl);
            node.setProperty("zombie", ttl);
            tx.success();
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(5*SCHEDULE_SECONDS+1));
        long now = System.currentTimeMillis();
        System.out.println("current = "+ now+" expired "+ (ttl <= now));
        try (Transaction tx = db.beginTx()) {
            System.out.println(db.execute("MATCH (n:Decay) WHERE n.zombie <= timestamp() RETURN n").resultAsString());
            ResourceIterator<Node> nodes = db.findNodes(Label.label("Decay"));
            assertEquals(false,nodes.hasNext());
        }

    }
}
