package it.networklink.core;

public record ServerSnapshot(
        String name,
        String address,
        boolean online,
        int players,
        boolean maintenance,
        String maintenanceReason
) {
    public ServerSnapshot(String name, String address, boolean online, int players) {
        this(name, address, online, players, false, "");
    }
}
