package it.networklink.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NetworkLinkService implements AutoCloseable {
    private final Path configFile;
    private final LocalNodeConfig config;
    private final PlatformType platform;
    private final PlatformSnapshotProvider provider;
    private final SnapshotCache cache = new SnapshotCache();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3, r -> {
        Thread t = new Thread(r, "networklink-task");
        t.setDaemon(true);
        return t;
    });
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final RemoteNodePoller poller;
    private HttpServer server;

    public NetworkLinkService(Path configFile, LocalNodeConfig config, PlatformType platform, PlatformSnapshotProvider provider) {
        this.configFile = configFile;
        this.config = config;
        this.platform = platform;
        this.provider = provider;
        this.poller = new RemoteNodePoller(scheduler, cache, config::links);
    }

    public void start() throws IOException {
        refreshLocal();
        long interval = config.pollSeconds();
        scheduler.scheduleAtFixedRate(this::refreshLocal, 0, interval, TimeUnit.SECONDS);
        poller.start(interval);
        if (config.dashboardEnabled()) startDashboard();
    }

    public void refreshLocal() {
        if (provider instanceof RefreshableSnapshotProvider refreshable) refreshable.refresh();
        NodeSnapshot raw = provider.snapshot();
        List<ServerSnapshot> servers = new ArrayList<>();
        for (ServerSnapshot server : raw.servers()) {
            NodeSnapshot linked = findRemoteNode(server.name()).orElse(null);
            if (linked != null) {
                servers.add(new ServerSnapshot(server.name(), server.address(), server.online(), server.players(),
                        linked.maintenance(), linked.maintenanceReason()));
            } else {
                servers.add(server);
            }
        }
        cache.setLocal(new NodeSnapshot(raw.id(), raw.name(), raw.platform(), raw.online(), raw.players(), servers,
                raw.timestamp(), config.maintenanceEnabled(), config.maintenanceReason()));
    }

    private void startDashboard() throws IOException {
        server = HttpServer.create(new InetSocketAddress(config.dashboardHost(), config.dashboardPort()), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/api/v1/status", this::handleStatus);
        server.createContext("/api/v1/dashboard", this::handleDashboard);
        server.createContext("/api/v1/links", this::handleLinks);
        server.createContext("/api/v1/maintenance", this::handleMaintenance);
        server.createContext("/api/v1/remote-maintenance", this::handleRemoteMaintenance);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "networklink-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
    }

    public int dashboardPort() { return config.dashboardPort(); }
    public String maintenanceBypassPermission() { return config.maintenanceBypassPermission(); }
    public boolean localMaintenance() { return config.maintenanceEnabled(); }
    public String localMaintenanceReason() { return config.maintenanceReason(); }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        var stream = NetworkLinkService.class.getResourceAsStream("/web/index.html");
        if (stream == null) {
            send(exchange, 500, "text/plain; charset=utf-8", "Dashboard resource missing");
            return;
        }
        String resource = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        send(exchange, 200, "text/html; charset=utf-8", resource);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!authorized(exchange, config.dashboardToken())) {
            send(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        NodeSnapshot local = cache.local();
        if (local == null) {
            send(exchange, 503, "application/json; charset=utf-8", "{\"error\":\"snapshot-unavailable\"}");
            return;
        }
        send(exchange, 200, "application/json; charset=utf-8", JsonCodec.snapshot(local));
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        if (!authorized(exchange, config.dashboardToken())) {
            send(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        StringBuilder json = new StringBuilder();
        NodeSnapshot local = cache.local();
        json.append("{\"local\":").append(local == null ? "null" : JsonCodec.snapshot(local));
        json.append(",\"links\":[");
        boolean first = true;
        for (RemoteNode link : config.links()) {
            if (!first) json.append(',');
            first = false;
            NodeSnapshot remote = cache.remote().get(link.id());
            json.append("{\"id\":\"").append(escape(link.id())).append("\",")
                    .append("\"url\":\"").append(escape(link.url())).append("\",")
                    .append("\"online\":").append(remote != null && remote.online()).append(',')
                    .append("\"snapshot\":").append(remote == null ? "null" : JsonCodec.snapshot(remote))
                    .append('}');
        }
        json.append("]}");
        send(exchange, 200, "application/json; charset=utf-8", json.toString());
    }

    private void handleLinks(HttpExchange exchange) throws IOException {
        if (!authorized(exchange, config.dashboardToken())) {
            send(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET" -> sendLinks(exchange);
            case "POST" -> addLink(exchange);
            case "DELETE" -> deleteLink(exchange);
            default -> send(exchange, 405, "application/json; charset=utf-8", "{\"error\":\"method-not-allowed\"}");
        }
    }

    private void handleMaintenance(HttpExchange exchange) throws IOException {
        if (!authorized(exchange, config.dashboardToken())) {
            send(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json; charset=utf-8", "{\"error\":\"method-not-allowed\"}");
            return;
        }
        Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        boolean enabled = Boolean.parseBoolean(form.getOrDefault("enabled", "false"));
        String reason = clean(form.get("reason"));
        setMaintenance(enabled, reason);
        send(exchange, 200, "application/json; charset=utf-8", "{\"ok\":true,\"maintenance\":" + enabled + "}");
    }

    private void handleRemoteMaintenance(HttpExchange exchange) throws IOException {
        if (!authorized(exchange, config.dashboardToken())) {
            send(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json; charset=utf-8", "{\"error\":\"method-not-allowed\"}");
            return;
        }
        Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String id = clean(form.get("id"));
        if (id.isBlank()) {
            send(exchange, 400, "application/json; charset=utf-8", "{\"error\":\"id-required\"}");
            return;
        }
        boolean enabled = Boolean.parseBoolean(form.getOrDefault("enabled", "false"));
        String reason = clean(form.get("reason"));
        try {
            setRemoteMaintenance(id, enabled, reason);
            send(exchange, 200, "application/json; charset=utf-8", "{\"ok\":true}");
        } catch (Exception ex) {
            send(exchange, 502, "application/json; charset=utf-8", "{\"error\":\"" + escape(ex.getMessage() == null ? "remote-request-failed" : ex.getMessage()) + "\"}");
        }
    }

    private void sendLinks(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (RemoteNode link : config.links()) {
            if (!first) json.append(',');
            first = false;
            json.append("{\"id\":\"").append(escape(link.id())).append("\",\"url\":\"").append(escape(link.url())).append("\"}");
        }
        send(exchange, 200, "application/json; charset=utf-8", json.append(']').toString());
    }

    private void addLink(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String id = clean(form.get("id"));
        String url = clean(form.get("url"));
        String token = form.getOrDefault("token", "");
        if (id.isBlank() || url.isBlank()) {
            send(exchange, 400, "application/json; charset=utf-8", "{\"error\":\"id-and-url-required\"}");
            return;
        }
        try { URI.create(LocalNodeConfig.normalizeUrl(url)); }
        catch (IllegalArgumentException ex) {
            send(exchange, 400, "application/json; charset=utf-8", "{\"error\":\"invalid-url\"}");
            return;
        }
        config.upsertLink(id, url, token);
        config.save(configFile);
        send(exchange, 200, "application/json; charset=utf-8", "{\"ok\":true}");
    }

    private void deleteLink(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseForm(exchange.getRequestURI().getRawQuery());
        String id = clean(query.get("id"));
        if (id.isBlank()) {
            send(exchange, 400, "application/json; charset=utf-8", "{\"error\":\"id-required\"}");
            return;
        }
        config.removeLink(id);
        config.save(configFile);
        send(exchange, 200, "application/json; charset=utf-8", "{\"ok\":true}");
    }

    private boolean authorized(HttpExchange exchange, String expected) {
        String header = exchange.getRequestHeaders().getFirst("X-NetworkLink-Token");
        return expected != null && !expected.isBlank() && expected.equals(header);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private static Map<String, String> parseForm(String raw) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split("&")) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=", 2);
            result.put(urlDecode(kv[0]), kv.length > 1 ? urlDecode(kv[1]) : "");
        }
        return result;
    }

    private static String urlDecode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    public NodeSnapshot snapshot() {
        NodeSnapshot local = cache.local();
        return local != null ? local : provider.snapshot().withMaintenance(config.maintenanceEnabled(), config.maintenanceReason());
    }

    public int remoteCount() { return cache.remote().size(); }

    public synchronized void link(String id, String url, String token) throws IOException {
        config.upsertLink(id, url, token);
        config.save(configFile);
    }

    public synchronized void unlink(String id) throws IOException {
        config.removeLink(id);
        config.save(configFile);
    }

    public synchronized void setMaintenance(boolean enabled, String reason) throws IOException {
        config.setMaintenance(enabled, reason);
        config.save(configFile);
        refreshLocal();
    }

    public synchronized void setRemoteMaintenance(String id, boolean enabled, String reason) throws IOException, InterruptedException {
        RemoteNode link = config.findLink(id);
        if (link == null) throw new IllegalArgumentException("Nodo remoto non trovato: " + id);
        String body = "enabled=" + java.net.URLEncoder.encode(String.valueOf(enabled), StandardCharsets.UTF_8)
                + "&reason=" + java.net.URLEncoder.encode(reason == null ? "" : reason, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(LocalNodeConfig.normalizeUrl(link.url()) + "/api/v1/maintenance"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-NetworkLink-Token", link.token())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Remote maintenance HTTP " + response.statusCode());
    }

    public boolean isTargetInMaintenance(String targetId) {
        return findRemoteNode(targetId).map(NodeSnapshot::maintenance).orElse(false);
    }

    public String maintenanceMessageFor(String targetId) {
        return findRemoteNode(targetId)
                .map(NodeSnapshot::maintenanceReason)
                .filter(v -> !v.isBlank())
                .orElse("Server in manutenzione. Riprova più tardi.");
    }

    public Optional<NodeSnapshot> findRemoteNode(String targetId) {
        if (targetId == null) return Optional.empty();
        for (RemoteNode link : config.links()) {
            NodeSnapshot snapshot = cache.remote().get(link.id());
            if (snapshot != null && (link.id().equalsIgnoreCase(targetId) || snapshot.id().equalsIgnoreCase(targetId) || snapshot.name().equalsIgnoreCase(targetId))) {
                return Optional.of(snapshot);
            }
        }
        return Optional.empty();
    }

    @Override public void close() {
        poller.close();
        if (server != null) server.stop(0);
    }
}
