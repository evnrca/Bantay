package com.evnrca.bantay.listeners;

import com.evnrca.bantay.Bantay;
import com.evnrca.bantay.cooldown.CooldownManager;
import org.bukkit.entity.Player;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    private final Bantay plugin;
    private final CooldownManager cooldownManager;

    public CommandListener(Bantay plugin) {
        this.plugin = plugin;
        this.cooldownManager = plugin.getCooldownManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("bantay.bypass")) {
            return;
        }

        if (!plugin.getConfigManager().isCommandCooldownEnabled()) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.isEmpty() || !message.startsWith("/")) {
            return;
        }

        String command = cooldownManager.resolveCommandKey(message);

        if (cooldownManager.isCommandExempt(command)) {
            return;
        }

        if (cooldownManager.checkCommandCooldown(uuid, command)) {
            int remaining = cooldownManager.getRemainingCommandCooldown(uuid, command);
            String msg = plugin.getConfigManager().getCommandCooldownMsg()
                    .replace("{seconds}", String.valueOf(remaining))
                    .replace("{command}", command);
            player.sendMessage(plugin.getConfigManager().getPrefix() + msg);
            event.setCancelled(true);
            return;
        }

        cooldownManager.setCommandCooldown(uuid, command);
    }
}
