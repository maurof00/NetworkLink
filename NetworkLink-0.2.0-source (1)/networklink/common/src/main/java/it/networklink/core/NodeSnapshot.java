package it.networklink.core;

import java.util.List;

public record NodeSnapshot(
        String id,
        String name,
        PlatformType platform,
        boolean online,
        int players,
        List<ServerSnapshot> servers,
        long timestamp,
        boolean maintenance,
        String maintenanceReason
) {
    public NodeSnapshot(String id, String name, PlatformType platform, boolean online, int players, List<ServerSnapshot> servers, long timestamp) {
        this(id, name, platform, online, players, servers, timestamp, false, "");
    }

    public NodeSnapshot withMaintenance(boolean enabled, String reason) {
        return new NodeSnapshot(id, name, platform, online, players, servers, timestamp, enabled, reason == null ? "" : reason);
    }
}
