package com.evnrca.bantay.cooldown;

import com.evnrca.bantay.config.ConfigManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final ConfigManager config;

    private final Map<UUID, Long> chatCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> commandCooldowns = new ConcurrentHashMap<>();

    public CooldownManager(ConfigManager config) {
        this.config = config;
    }

    public void reload() {
        // No config-dependent state to clear, but we could clear cooldowns if desired
    }

    public boolean checkChatCooldown(UUID playerId) {
        if (!config.isChatCooldownEnabled()) {
            return false;
        }

        Long lastChat = chatCooldowns.get(playerId);
        if (lastChat == null) {
            return false;
        }

        long elapsed = (System.currentTimeMillis() - lastChat) / 1000;
        return elapsed < config.getChatCooldownSeconds();
    }

    public int getRemainingChatCooldown(UUID playerId) {
        Long lastChat = chatCooldowns.get(playerId);
        if (lastChat == null) {
            return 0;
        }

        long elapsed = (System.currentTimeMillis() - lastChat) / 1000;
        int remaining = config.getChatCooldownSeconds() - (int) elapsed;
        return Math.max(0, remaining);
    }

    public void setChatCooldown(UUID playerId) {
        chatCooldowns.put(playerId, System.currentTimeMillis());
    }

    public boolean checkCommandCooldown(UUID playerId, String command) {
        if (!config.isCommandCooldownEnabled()) {
            return false;
        }

        String lowerCommand = command.toLowerCase();

        if (config.getExemptCommands().contains(lowerCommand)) {
            return false;
        }

        int cooldownSeconds = config.getCommandOverrides().getOrDefault(lowerCommand, config.getCommandCooldownSeconds());

        Map<String, Long> playerCommands = commandCooldowns.get(playerId);
        if (playerCommands == null) {
            return false;
        }

        Long lastCommand = playerCommands.get(lowerCommand);
        if (lastCommand == null) {
            Long globalLast = playerCommands.get("*");
            if (globalLast == null) {
                return false;
            }
            long elapsed = (System.currentTimeMillis() - globalLast) / 1000;
            return elapsed < config.getCommandCooldownSeconds();
        }

        long elapsed = (System.currentTimeMillis() - lastCommand) / 1000;
        return elapsed < cooldownSeconds;
    }

    public int getRemainingCommandCooldown(UUID playerId, String command) {
        String lowerCommand = command.toLowerCase();

        int cooldownSeconds = config.getCommandOverrides().getOrDefault(lowerCommand, config.getCommandCooldownSeconds());

        Map<String, Long> playerCommands = commandCooldowns.get(playerId);
        if (playerCommands == null) {
            return 0;
        }

        Long lastCommand = playerCommands.get(lowerCommand);
        if (lastCommand == null) {
            Long globalLast = playerCommands.get("*");
            if (globalLast == null) {
                return 0;
            }
            long elapsed = (System.currentTimeMillis() - globalLast) / 1000;
            return Math.max(0, config.getCommandCooldownSeconds() - (int) elapsed);
        }

        long elapsed = (System.currentTimeMillis() - lastCommand) / 1000;
        return Math.max(0, cooldownSeconds - (int) elapsed);
    }

    public void setCommandCooldown(UUID playerId, String command) {
        long now = System.currentTimeMillis();
        String lowerCommand = command.toLowerCase();

        commandCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(lowerCommand, now);
        commandCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put("*", now);
    }
}