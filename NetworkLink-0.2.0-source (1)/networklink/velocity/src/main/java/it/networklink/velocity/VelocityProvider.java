package it.networklink.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import it.networklink.core.NodeSnapshot;
import it.networklink.core.RefreshableSnapshotProvider;
import it.networklink.core.ServerSnapshot;
import it.networklink.core.PlatformType;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityProvider implements RefreshableSnapshotProvider {
    private final ProxyServer proxy;
    private final String nodeId;
    private final String nodeName;
    private final Map<String, Boolean> health = new ConcurrentHashMap<>();

    public VelocityProvider(ProxyServer proxy, String nodeId, String nodeName) {
        this.proxy = proxy;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }

    @Override public void refresh() {
        for (RegisteredServer server : proxy.getAllServers()) {
            server.ping().whenComplete((ping, error) -> health.put(server.getServerInfo().getName(), error == null && ping != null));
        }
    }

    @Override public NodeSnapshot snapshot() {
        ArrayList<ServerSnapshot> servers = new ArrayList<>();
        int players = proxy.getPlayerCount();
        for (RegisteredServer server : proxy.getAllServers()) {
            var info = server.getServerInfo();
            String address = info.getAddress().getHostString() + ":" + info.getAddress().getPort();
            servers.add(new ServerSnapshot(info.getName(), address, health.getOrDefault(info.getName(), false), server.getPlayersConnected().size()));
        }
        return new NodeSnapshot(nodeId, nodeName, PlatformType.VELOCITY, true, players, servers, System.currentTimeMillis());
    }
}
