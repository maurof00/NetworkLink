package it.networklink.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;
import it.networklink.core.LocalNodeConfig;
import it.networklink.core.NetworkLinkService;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Plugin(id = "networklink", name = "NetworkLink", version = "0.2.0", description = "Cross-proxy network linking and web dashboard.", authors = {"NetworkLink"})
public final class VelocityMain {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private NetworkLinkService service;

    @Inject
    public VelocityMain(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve("networklink.properties");
            ensureConfig(configPath);
            startService(configPath);
            CommandManager commands = proxy.getCommandManager();
            var meta = commands.metaBuilder("networklink").aliases("nl").plugin(this).build();
            commands.register(meta, new VelocityCommand(this, service));
            proxy.getEventManager().register(this, new VelocityMaintenanceListener(service));
            logger.info("NetworkLink abilitato. Dashboard: http://<proxy-host>:{}", service.dashboardPort());
        } catch (Exception ex) {
            logger.error("NetworkLink non avviato", ex);
        }
    }

    private void startService(Path configPath) throws Exception {
        LocalNodeConfig config = LocalNodeConfig.load(configPath);
        VelocityProvider provider = new VelocityProvider(proxy, config.nodeId(), config.nodeName());
        service = new NetworkLinkService(configPath, config, it.networklink.core.PlatformType.VELOCITY, provider);
        service.start();
    }

    public NetworkLinkService service() {
        if (service == null) throw new IllegalStateException("NetworkLink non inizializzato");
        return service;
    }

    public synchronized void reloadNetworkLink() throws Exception {
        if (service != null) service.close();
        startService(dataDirectory.resolve("networklink.properties"));
    }

    private void ensureConfig(Path file) throws Exception {
        if (Files.exists(file)) return;
        try (InputStream in = VelocityMain.class.getResourceAsStream("/networklink.properties")) {
            if (in == null) throw new IllegalStateException("resource networklink.properties mancante");
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
