package it.networklink.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import it.networklink.core.NetworkLinkService;

public final class VelocityMaintenanceListener {
    private final NetworkLinkService service;

    public VelocityMaintenanceListener(NetworkLinkService service) { this.service = service; }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        String target = event.getOriginalServer().getServerInfo().getName();
        Player player = event.getPlayer();
        if (!service.isTargetInMaintenance(target)) return;
        if (player.hasPermission(service.maintenanceBypassPermission())) return;
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        player.sendMessage(Component.text("[NetworkLink] " + service.maintenanceMessageFor(target)));
    }
}
