package it.networklink.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import it.networklink.core.NetworkLinkService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class VelocityCommand implements SimpleCommand {
    private final VelocityMain plugin;

    public VelocityCommand(VelocityMain plugin, NetworkLinkService service) {
        this.plugin = plugin;
    }

    @Override public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        var service = plugin.service();
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            var local = service.snapshot();
            sender.sendPlainMessage("[NetworkLink] " + local.name() + " " + local.platform() + " ONLINE players=" + local.players() + " maintenance=" + local.maintenance());
            for (var s : local.servers()) {
                sender.sendPlainMessage(" - " + s.name() + " " + s.address() + " " + (s.online() ? "ONLINE" : "OFFLINE") + " players=" + s.players() + (s.maintenance() ? " MAINTENANCE" : ""));
            }
            sender.sendPlainMessage("Remote links online: " + service.remoteCount());
            return;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "link" -> {
                    if (args.length < 4) { sender.sendPlainMessage("/networklink link <id> <url> <token>"); return; }
                    service.link(args[1], args[2], args[3]);
                    sender.sendPlainMessage("[NetworkLink] link salvato.");
                }
                case "unlink" -> {
                    if (args.length < 2) { sender.sendPlainMessage("/networklink unlink <id>"); return; }
                    service.unlink(args[1]);
                    sender.sendPlainMessage("[NetworkLink] link rimosso.");
                }
                case "reload" -> { plugin.reloadNetworkLink(); sender.sendPlainMessage("[NetworkLink] configurazione ricaricata."); }
                case "dashboard" -> sender.sendPlainMessage("[NetworkLink] Dashboard: http://<proxy-host>:" + service.dashboardPort());
                case "maintenance" -> handleMaintenance(sender, args);
                default -> sender.sendPlainMessage("/networklink status | link | unlink | reload | dashboard | maintenance");
            }
        } catch (Exception ex) {
            sender.sendPlainMessage("[NetworkLink] Errore: " + ex.getMessage());
        }
    }

    private void handleMaintenance(CommandSource sender, String[] args) {
        if (args.length < 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            sender.sendPlainMessage("/networklink maintenance <on|off> [node-id] [reason]");
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        String target = args.length >= 3 ? args[2] : null;
        int reasonStart = target == null ? 2 : 3;
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, reasonStart, args.length)).trim();
        try {
            if (target == null || target.isBlank()) {
                service.setMaintenance(enabled, reason);
                sender.sendPlainMessage("[NetworkLink] Manutenzione locale " + (enabled ? "ATTIVATA" : "DISATTIVATA") + ".");
            } else {
                service.setRemoteMaintenance(target, enabled, reason);
                sender.sendPlainMessage("[NetworkLink] Manutenzione remota per " + target + " " + (enabled ? "ATTIVATA" : "DISATTIVATA") + ".");
            }
        } catch (Exception ex) {
            sender.sendPlainMessage("[NetworkLink] Errore: " + ex.getMessage());
        }
    }

    @Override public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("networklink.admin");
    }

    @Override public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) return Arrays.asList("status", "link", "unlink", "reload", "dashboard", "maintenance");
        return Collections.emptyList();
    }
}
