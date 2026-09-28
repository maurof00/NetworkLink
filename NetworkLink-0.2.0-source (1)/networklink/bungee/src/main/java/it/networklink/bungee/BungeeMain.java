package it.networklink.bungee;

import it.networklink.core.LocalNodeConfig;
import it.networklink.core.NetworkLinkService;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BungeeMain extends Plugin {
    private NetworkLinkService service;
    private BungeeProvider provider;
    private Path configPath;

    @Override public void onEnable() {
        try {
            getDataFolder().mkdirs();
            configPath = getDataFolder().toPath().resolve("networklink.properties");
            ensureConfig(configPath);
            startService();
            ProxyServer.getInstance().getPluginManager().registerCommand(this, new BungeeCommand(this, service));
            ProxyServer.getInstance().getPluginManager().registerListener(this, new BungeeMaintenanceListener(service));
            getLogger().info("NetworkLink abilitato. Dashboard: http://<proxy-host>:" + service.dashboardPort());
        } catch (Exception ex) {
            getLogger().severe("NetworkLink non avviato: " + ex.getMessage());
        }
    }

    private void startService() throws Exception {
        LocalNodeConfig config = LocalNodeConfig.load(configPath);
        provider = new BungeeProvider(ProxyServer.getInstance(), config.nodeId(), config.nodeName());
        service = new NetworkLinkService(configPath, config, it.networklink.core.PlatformType.BUNGEE, provider);
        service.start();
    }

    public NetworkLinkService service() {
        if (service == null) throw new IllegalStateException("NetworkLink non inizializzato");
        return service;
    }

    public synchronized void reloadNetworkLink() throws Exception {
        if (service != null) service.close();
        startService();
    }

    private void ensureConfig(Path file) throws Exception {
        if (Files.exists(file)) return;
        try (InputStream in = getResourceAsStream("networklink.properties")) {
            if (in == null) throw new IllegalStateException("resource networklink.properties mancante");
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override public void onDisable() { if (service != null) service.close(); }
}
