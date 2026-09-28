package it.networklink.bungee;

import it.networklink.core.NetworkLinkService;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public final class BungeeCommand extends Command {
    private final BungeeMain plugin;

    public BungeeCommand(BungeeMain plugin, NetworkLinkService service) {
        super("networklink", "networklink.admin", "nl");
        this.plugin = plugin;
    }

    @Override public void execute(CommandSender sender, String[] args) {
        var service = plugin.service();
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            var local = service.snapshot();
            sender.sendMessage("§8[§bNetworkLink§8] §7" + local.name() + " §f" + local.platform() + " §aONLINE §7players=" + local.players() + " §7maintenance=" + local.maintenance());
            for (var server : local.servers()) {
                sender.sendMessage(" §8- §f" + server.name() + " §7" + server.address() + " " + (server.online() ? "§aONLINE" : "§cOFFLINE") + " §7players=" + server.players() + (server.maintenance() ? " §eMAINTENANCE" : ""));
            }
            sender.sendMessage("§7Remote links: §f" + service.remoteCount() + " §7online");
            return;
        }
        try {
            switch (args[0].toLowerCase()) {
            case "link" -> {
                if (args.length < 4) { sender.sendMessage("§c/networklink link <id> <url> <token>"); return; }
                service.link(args[1], args[2], args[3]);
                sender.sendMessage("§aNetworkLink: link salvato.");
            }
            case "unlink" -> {
                if (args.length < 2) { sender.sendMessage("§c/networklink unlink <id>"); return; }
                service.unlink(args[1]);
                sender.sendMessage("§aNetworkLink: link rimosso.");
            }
            case "reload" -> {
                try { plugin.reloadNetworkLink(); sender.sendMessage("§aNetworkLink: configurazione ricaricata."); }
                catch (Exception ex) { sender.sendMessage("§cNetworkLink: " + ex.getMessage()); }
            }
            case "dashboard" -> sender.sendMessage("§bDashboard: §fhttp://<proxy-host>:" + service.dashboardPort());
            case "maintenance" -> handleMaintenance(sender, args);
            default -> sender.sendMessage("§e/networklink status | link | unlink | reload | dashboard | maintenance");
            }
        } catch (Exception ex) {
            sender.sendMessage("§cNetworkLink: " + ex.getMessage());
        }
    }
    private void handleMaintenance(CommandSender sender, String[] args) {
        if (args.length < 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage("§e/networklink maintenance <on|off> [node-id] [reason]");
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        int reasonStart = 2;
        String target = null;
        if (args.length >= 3) {
            target = args[2];
            reasonStart = 3;
        }
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
            sender.sendMessage("§cNetworkLink: " + ex.getMessage());
        }
    }
}
