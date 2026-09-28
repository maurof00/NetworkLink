package it.networklink.bungee;

import it.networklink.core.NodeSnapshot;
import it.networklink.core.RefreshableSnapshotProvider;
import it.networklink.core.PlatformType;
import it.networklink.core.ServerSnapshot;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ServerPing;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BungeeProvider implements RefreshableSnapshotProvider {
    private final ProxyServer proxy;
    private final String nodeId;
    private final String nodeName;
    private final Map<String, Boolean> health = new ConcurrentHashMap<>();

    public BungeeProvider(ProxyServer proxy, String nodeId, String nodeName) {
        this.proxy = proxy;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }

    public void refresh() {
        for (ServerInfo info : proxy.getServers().values()) {
            try {
                info.ping(new Callback<ServerPing>() {
                    @Override public void done(ServerPing result, Throwable error) {
                        health.put(info.getName(), error == null && result != null);
                    }
                });
            } catch (Throwable error) {
                health.put(info.getName(), false);
            }
        }
    }

    @Override public NodeSnapshot snapshot() {
        ArrayList<ServerSnapshot> servers = new ArrayList<>();
        int players = 0;
        for (ServerInfo info : proxy.getServers().values()) {
            int count = info.getPlayers().size();
            players += count;
            SocketAddress socket = info.getSocketAddress();
            String address = socket instanceof InetSocketAddress inet
                    ? inet.getHostString() + ":" + inet.getPort() : socket.toString();
            servers.add(new ServerSnapshot(info.getName(), address, health.getOrDefault(info.getName(), false), count));
        }
        return new NodeSnapshot(nodeId, nodeName, PlatformType.BUNGEE, true, players, servers, System.currentTimeMillis());
    }
}
