package it.networklink.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class LocalNodeConfig {
    private String nodeId = "proxy-1";
    private String nodeName = "Proxy 1";
    private boolean dashboardEnabled = true;
    private String dashboardHost = "0.0.0.0";
    private int dashboardPort = 8088;
    private String dashboardToken = "change-me";
    private int pollSeconds = 5;
    private boolean maintenanceEnabled = false;
    private String maintenanceReason = "Server in manutenzione. Riprova più tardi.";
    private String maintenanceBypassPermission = "networklink.maintenance.bypass";
    private final Map<String, RemoteNode> links = new LinkedHashMap<>();

    public static LocalNodeConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            LocalNodeConfig config = new LocalNodeConfig();
            config.save(file);
            return config;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
        }
        LocalNodeConfig c = new LocalNodeConfig();
        c.nodeId = p.getProperty("node.id", c.nodeId).trim();
        c.nodeName = p.getProperty("node.name", c.nodeName).trim();
        c.dashboardEnabled = Boolean.parseBoolean(p.getProperty("dashboard.enabled", String.valueOf(c.dashboardEnabled)));
        c.dashboardHost = p.getProperty("dashboard.host", c.dashboardHost).trim();
        c.dashboardPort = parseInt(p.getProperty("dashboard.port"), c.dashboardPort, 1, 65535);
        c.dashboardToken = p.getProperty("dashboard.token", c.dashboardToken);
        c.pollSeconds = parseInt(p.getProperty("poll.seconds"), c.pollSeconds, 1, 300);
        c.maintenanceEnabled = Boolean.parseBoolean(p.getProperty("maintenance.enabled", String.valueOf(c.maintenanceEnabled)));
        c.maintenanceReason = p.getProperty("maintenance.reason", c.maintenanceReason).trim();
        c.maintenanceBypassPermission = p.getProperty("maintenance.bypass-permission", c.maintenanceBypassPermission).trim();
        if (c.maintenanceReason.isBlank()) c.maintenanceReason = "Server in manutenzione. Riprova più tardi.";
        if (c.maintenanceBypassPermission.isBlank()) c.maintenanceBypassPermission = "networklink.maintenance.bypass";
        for (String key : p.stringPropertyNames()) {
            if (!key.startsWith("links.") || !key.endsWith(".url")) continue;
            String id = key.substring("links.".length(), key.length() - ".url".length()).trim();
            String url = p.getProperty(key, "").trim();
            String token = p.getProperty("links." + id + ".token", "");
            if (!id.isBlank() && !url.isBlank()) {
                c.links.put(id, new RemoteNode(id, url, token));
            }
        }
        return c;
    }

    public synchronized void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Properties p = new Properties();
        p.setProperty("node.id", nodeId);
        p.setProperty("node.name", nodeName);
        p.setProperty("dashboard.enabled", String.valueOf(dashboardEnabled));
        p.setProperty("dashboard.host", dashboardHost);
        p.setProperty("dashboard.port", String.valueOf(dashboardPort));
        p.setProperty("dashboard.token", dashboardToken);
        p.setProperty("poll.seconds", String.valueOf(pollSeconds));
        p.setProperty("maintenance.enabled", String.valueOf(maintenanceEnabled));
        p.setProperty("maintenance.reason", maintenanceReason);
        p.setProperty("maintenance.bypass-permission", maintenanceBypassPermission);
        for (RemoteNode link : links.values()) {
            p.setProperty("links." + link.id() + ".url", link.url());
            p.setProperty("links." + link.id() + ".token", link.token());
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            p.store(w, "NetworkLink configuration");
        }
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        if (value == null) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public synchronized void upsertLink(String id, String url, String token) {
        links.put(id, new RemoteNode(id, normalizeUrl(url), token == null ? "" : token));
    }

    public synchronized void removeLink(String id) { links.remove(id); }
    public synchronized void setMaintenance(boolean enabled, String reason) {
        maintenanceEnabled = enabled;
        if (reason != null && !reason.trim().isBlank()) maintenanceReason = reason.trim();
    }
    public synchronized RemoteNode findLink(String id) {
        if (id == null) return null;
        RemoteNode direct = links.get(id);
        if (direct != null) return direct;
        for (RemoteNode link : links.values()) {
            if (link.id().equalsIgnoreCase(id)) return link;
        }
        return null;
    }
    public synchronized List<RemoteNode> links() { return new ArrayList<>(links.values()); }
    public String nodeId() { return nodeId; }
    public String nodeName() { return nodeName; }
    public boolean dashboardEnabled() { return dashboardEnabled; }
    public String dashboardHost() { return dashboardHost; }
    public int dashboardPort() { return dashboardPort; }
    public String dashboardToken() { return dashboardToken; }
    public int pollSeconds() { return pollSeconds; }
    public boolean maintenanceEnabled() { return maintenanceEnabled; }
    public String maintenanceReason() { return maintenanceReason; }
    public String maintenanceBypassPermission() { return maintenanceBypassPermission; }

    public static String normalizeUrl(String url) {
        String value = url.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
