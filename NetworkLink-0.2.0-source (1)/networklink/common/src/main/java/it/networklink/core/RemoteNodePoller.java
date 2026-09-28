package it.networklink.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RemoteNodePoller implements AutoCloseable {
    private static final Pattern ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern NAME = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern PLATFORM = Pattern.compile("\\\"platform\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern ONLINE = Pattern.compile("\\\"online\\\"\\s*:\\s*(true|false)");
    private static final Pattern PLAYERS = Pattern.compile("\\\"players\\\"\\s*:\\s*(\\d+)");
    private static final Pattern MAINTENANCE = Pattern.compile("\\\"maintenance\\\"\\s*:\\s*(true|false)");
    private static final Pattern MAINTENANCE_REASON = Pattern.compile("\\\"maintenanceReason\\\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern SERVER = Pattern.compile("\\{\\\"name\\\":\\\"([^\"]*)\\\",\\\"address\\\":\\\"([^\"]*)\\\",\\\"online\\\":(true|false),\\\"players\\\":(\\d+)(?:,\\\"maintenance\\\":(true|false),\\\"maintenanceReason\\\":\\\"([^\"]*)\\\")?\\}");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ScheduledExecutorService scheduler;
    private final SnapshotCache cache;
    private final java.util.function.Supplier<List<RemoteNode>> linksSupplier;

    public RemoteNodePoller(ScheduledExecutorService scheduler, SnapshotCache cache,
                            java.util.function.Supplier<List<RemoteNode>> linksSupplier) {
        this.scheduler = scheduler;
        this.cache = cache;
        this.linksSupplier = linksSupplier;
    }

    public void start(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::pollAll, 0, Math.max(1, intervalSeconds), TimeUnit.SECONDS);
    }

    private void pollAll() {
        Map<String, NodeSnapshot> next = new LinkedHashMap<>();
        for (RemoteNode link : linksSupplier.get()) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(link.url() + "/api/v1/status"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Accept", "application/json")
                        .header("X-NetworkLink-Token", link.token())
                        .GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    NodeSnapshot snapshot = parseSnapshot(response.body());
                    if (snapshot != null) next.put(link.id(), snapshot);
                }
            } catch (Exception ignored) {
                // Offline/invalid links are simply absent from the current remote cache.
            }
        }
        cache.replaceRemote(next);
    }

    private static NodeSnapshot parseSnapshot(String json) {
        String id = match(ID, json);
        String name = match(NAME, json);
        String platformRaw = match(PLATFORM, json);
        String onlineRaw = match(ONLINE, json);
        String playersRaw = match(PLAYERS, json);
        String maintenanceRaw = match(MAINTENANCE, json);
        String maintenanceReason = match(MAINTENANCE_REASON, json);
        if (id == null || name == null || platformRaw == null || onlineRaw == null || playersRaw == null) return null;
        if (maintenanceRaw == null) maintenanceRaw = "false";
        if (maintenanceReason == null) maintenanceReason = "";
        List<ServerSnapshot> servers = new java.util.ArrayList<>();
        Matcher matcher = SERVER.matcher(json);
        while (matcher.find()) {
            servers.add(new ServerSnapshot(
                    matcher.group(1), matcher.group(2), Boolean.parseBoolean(matcher.group(3)), Integer.parseInt(matcher.group(4)),
                    matcher.group(5) != null && Boolean.parseBoolean(matcher.group(5)), matcher.group(6) == null ? "" : matcher.group(6)));
        }
        PlatformType type;
        try { type = PlatformType.valueOf(platformRaw); } catch (IllegalArgumentException ex) { type = PlatformType.BUNGEE; }
        return new NodeSnapshot(id, name, type, Boolean.parseBoolean(onlineRaw), Integer.parseInt(playersRaw), servers,
                System.currentTimeMillis(), Boolean.parseBoolean(maintenanceRaw), maintenanceReason);
    }

    private static String match(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Override public void close() { scheduler.shutdownNow(); }
}
