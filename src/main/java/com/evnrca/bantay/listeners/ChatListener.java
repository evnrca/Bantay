package com.evnrca.bantay.listeners;

import com.evnrca.bantay.Bantay;
import com.evnrca.bantay.cooldown.CooldownManager;
import com.evnrca.bantay.filter.ProfanityFilter;
import org.bukkit.entity.Player;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final Bantay plugin;
    private final ProfanityFilter profanityFilter;
    private final CooldownManager cooldownManager;

    public ChatListener(Bantay plugin) {
        this.plugin = plugin;
        this.profanityFilter = plugin.getProfanityFilter();
        this.cooldownManager = plugin.getCooldownManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("bantay.bypass")) {
            return;
        }

        if (plugin.getConfigManager().isChatCooldownEnabled()) {
            if (cooldownManager.checkChatCooldown(uuid)) {
                int remaining = cooldownManager.getRemainingChatCooldown(uuid);
                String msg = plugin.getConfigManager().getChatCooldownMsg()
                        .replace("{seconds}", String.valueOf(remaining));
                player.sendMessage(plugin.getConfigManager().getPrefix() + msg);
                event.setCancelled(true);
                return;
            }
        }

        String message = event.getMessage();
        ProfanityFilter.FilterResult result = profanityFilter.filter(message, player);

        if (result.wasCensored) {
            event.setMessage(result.filteredMessage);

            if (plugin.getConfigManager().isNotifyStaff()) {
                notifyStaff(player, message, result.filteredMessage);
            }
        }

        cooldownManager.setChatCooldown(uuid);
    }

    private void notifyStaff(Player sender, String original, String filtered) {
        String notifyMsg = plugin.getConfigManager().getPrefix() +
                "&eChat censored from &f" + sender.getName() + "&e: &f" + original;

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("bantay.admin") && !p.equals(sender)) {
                p.sendMessage(notifyMsg);
            }
        }
    }
}