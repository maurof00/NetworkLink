package it.networklink.spigot;

import it.networklink.core.NetworkLinkService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import net.kyori.adventure.text.Component;

public final class SpigotMaintenanceListener implements Listener {
    private final NetworkLinkService service;

    public SpigotMaintenanceListener(NetworkLinkService service) { this.service = service; }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!service.localMaintenance()) return;
        if (event.getPlayer().hasPermission(service.maintenanceBypassPermission())) return;
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(service.localMaintenanceReason()));
    }
}
