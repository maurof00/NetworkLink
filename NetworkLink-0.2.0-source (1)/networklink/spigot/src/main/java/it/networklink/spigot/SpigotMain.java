package it.networklink.spigot;

import it.networklink.core.LocalNodeConfig;
import it.networklink.core.NetworkLinkService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class SpigotMain extends JavaPlugin implements TabExecutor {
    private NetworkLinkService service;
    private Path configPath;

    @Override
    public void onEnable() {
        try {
            configPath = getDataFolder().toPath().resolve("networklink.properties");
            ensureConfig(configPath);
            startService();
            if (getCommand("networklink") != null) getCommand("networklink").setExecutor(this);
            Bukkit.getPluginManager().registerEvents(new SpigotMaintenanceListener(service), this);
            getLogger().info("NetworkLink backend/node abilitato. Dashboard: http://<server-host>:" + service.dashboardPort());
        } catch (Exception ex) {
            getLogger().severe("NetworkLink non avviato: " + ex.getMessage());
        }
    }

    private void startService() throws Exception {
        LocalNodeConfig config = LocalNodeConfig.load(configPath);
        SpigotProvider provider = new SpigotProvider(this, config.nodeId(), config.nodeName());
        service = new NetworkLinkService(configPath, config, it.networklink.core.PlatformType.SPIGOT, provider);
        service.start();
    }

    private void ensureConfig(Path file) throws Exception {
        if (Files.exists(file)) return;
        Files.createDirectories(getDataFolder().toPath());
        try (InputStream in = SpigotMain.class.getResourceAsStream("/networklink.properties")) {
            if (in == null) throw new IllegalStateException("resource networklink.properties mancante");
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void onDisable() {
        if (service != null) service.close();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (service == null) {
            sender.sendMessage("§c[NetworkLink] servizio non disponibile.");
            return true;
        }
        if (!sender.hasPermission("networklink.admin")) {
            sender.sendMessage("§c[NetworkLink] Nessun permesso.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            var local = service.snapshot();
            sender.sendMessage("§8[§bNetworkLink§8] §aONLINE §7node=" + local.name() + " §7platform=" + local.platform() + " §7players=" + local.players() + " §7maintenance=" + local.maintenance());
            sender.sendMessage("§7Link remoti online: " + service.remoteCount());
            return true;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "link" -> {
                    if (args.length < 4) { sender.sendMessage("§e/networklink link <id> <url> <token>"); return true; }
                    service.link(args[1], args[2], args[3]);
                    sender.sendMessage("§a[NetworkLink] Nodo collegato e salvato.");
                }
                case "unlink" -> {
                    if (args.length < 2) { sender.sendMessage("§e/networklink unlink <id>"); return true; }
                    service.unlink(args[1]);
                    sender.sendMessage("§a[NetworkLink] Collegamento rimosso.");
                }
                case "reload" -> {
                    service.close();
                    startService();
                    sender.sendMessage("§a[NetworkLink] Configurazione ricaricata.");
                }
                case "dashboard" -> sender.sendMessage("§b[NetworkLink] Dashboard: http://<server-host>:" + service.dashboardPort());
                case "maintenance" -> handleMaintenance(sender, args);
                default -> sender.sendMessage("§e/networklink status | link | unlink | reload | dashboard | maintenance");
            }
        } catch (Exception ex) {
            sender.sendMessage("§c[NetworkLink] Errore: " + ex.getMessage());
        }
        return true;
    }

    private void handleMaintenance(CommandSender sender, String[] args) {
        if (args.length < 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage("§e/networklink maintenance <on|off> [node-id] [reason]");
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        String target = args.length >= 3 ? args[2] : null;
        int reasonStart = target == null ? 2 : 3;
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, reasonStart, args.length)).trim();
        try {
            if (target == null || target.isBlank()) {
                service.setMaintenance(enabled, reason);
                sender.sendMessage("§a[NetworkLink] Manutenzione locale " + (enabled ? "ATTIVATA" : "DISATTIVATA") + ".");
            } else {
                service.setRemoteMaintenance(target, enabled, reason);
                sender.sendMessage("§a[NetworkLink] Manutenzione remota per " + target + " " + (enabled ? "ATTIVATA" : "DISATTIVATA") + ".");
            }
        } catch (Exception ex) {
            sender.sendMessage("§c[NetworkLink] Errore: " + ex.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("status", "link", "unlink", "reload", "dashboard", "maintenance");
        return List.of();
    }
}
