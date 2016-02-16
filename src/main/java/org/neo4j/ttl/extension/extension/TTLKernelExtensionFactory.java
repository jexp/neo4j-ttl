package org.neo4j.ttl.extension.extension;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.Description;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;

import static org.neo4j.kernel.configuration.Settings.*;

/**
 * @author mh
 * @since 06.02.13
 */
public class TTLKernelExtensionFactory extends KernelExtensionFactory<TTLKernelExtensionFactory.Dependencies> {

    public static final String SERVICE_NAME = "TIME_TO_LIVE";

    @Description("Settings for the TTL Extension")
    public static abstract class TTLSettings {
        public static Setting<Long> schedule = setting("ttl.schedule", DURATION, "60s");
        public static Setting<String> ttlLabel = setting("ttl.label", STRING, (String) "Timed");
        public static Setting<String> ttlProperty = setting("ttl.property", STRING, (String) "ttl");
    }

    public TTLKernelExtensionFactory() {
        super(SERVICE_NAME);
    }

    @Override
    public Lifecycle newInstance(KernelContext kernelContext, Dependencies dependencies) throws Throwable {
        Config config = dependencies.getConfig();
        return new TTLExtensionCypher(dependencies.getGraphDatabaseService(),
                config.get(TTLSettings.schedule),
                config.get(TTLSettings.ttlLabel),
                config.get(TTLSettings.ttlProperty));
    }

    public interface Dependencies {
        GraphDatabaseService getGraphDatabaseService();

        Config getConfig();
    }
}