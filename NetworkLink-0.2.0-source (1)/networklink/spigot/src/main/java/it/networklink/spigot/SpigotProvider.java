package it.networklink.spigot;

import it.networklink.core.NodeSnapshot;
import it.networklink.core.PlatformSnapshotProvider;
import it.networklink.core.PlatformType;
import it.networklink.core.ServerSnapshot;
import it.networklink.core.RefreshableSnapshotProvider;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class SpigotProvider implements RefreshableSnapshotProvider {
    private final JavaPlugin plugin;
    private final String nodeId;
    private final String nodeName;

    public SpigotProvider(JavaPlugin plugin, String nodeId, String nodeName) {
        this.plugin = plugin;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }

    @Override
    public void refresh() {
        // The Bukkit API is local: if the plugin is active, this backend is online.
    }

    @Override
    public NodeSnapshot snapshot() {
        Server server = plugin.getServer();
        String address = server.getIp();
        if (address == null || address.isBlank()) address = "0.0.0.0";
        int port = server.getPort();
        List<ServerSnapshot> servers = List.of(new ServerSnapshot(
                nodeName,
                address + ":" + port,
                true,
                Bukkit.getOnlinePlayers().size()
        ));
        return new NodeSnapshot(
                nodeId,
                nodeName,
                PlatformType.SPIGOT,
                true,
                Bukkit.getOnlinePlayers().size(),
                servers,
                System.currentTimeMillis()
        );
    }
}
