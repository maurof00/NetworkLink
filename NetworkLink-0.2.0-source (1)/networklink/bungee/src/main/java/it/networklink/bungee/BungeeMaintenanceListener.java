package it.networklink.bungee;

import it.networklink.core.NetworkLinkService;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeeMaintenanceListener implements Listener {
    private final NetworkLinkService service;

    public BungeeMaintenanceListener(NetworkLinkService service) { this.service = service; }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        String target = event.getTarget().getName();
        if (!service.isTargetInMaintenance(target)) return;
        if (event.getPlayer().hasPermission(service.maintenanceBypassPermission())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(new TextComponent("§c[NetworkLink] §f" + service.maintenanceMessageFor(target)));
    }
}
