package com.evnrca.bantay.cooldown;

import com.evnrca.bantay.config.ConfigManager;

import java.util.Map;
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

    public String resolveCommandKey(String commandLine) {
        String normalized = normalizeCommand(commandLine);
        String matched = null;

        for (String command : config.getExemptCommands()) {
            matched = longerMatch(normalized, command, matched);
        }
        for (String command : config.getCommandOverrides().keySet()) {
            matched = longerMatch(normalized, command, matched);
        }

        if (matched != null) {
            return matched;
        }

        int spaceIndex = normalized.indexOf(' ');
        return spaceIndex >= 0 ? normalized.substring(0, spaceIndex) : normalized;
    }

    private String longerMatch(String commandLine, String configuredCommand, String currentMatch) {
        String command = normalizeCommand(configuredCommand);
        if (commandLine.equals(command) || commandLine.startsWith(command + " ")) {
            if (currentMatch == null || command.length() > currentMatch.length()) {
                return command;
            }
        }
        return currentMatch;
    }

    private String normalizeCommand(String command) {
        return command == null ? "" : command.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    public boolean isCommandExempt(String commandKey) {
        return config.getExemptCommands().contains(normalizeCommand(commandKey));
    }

    public boolean checkCommandCooldown(UUID playerId, String commandKey) {
        if (!config.isCommandCooldownEnabled()) {
            return false;
        }

        String lowerCommand = normalizeCommand(commandKey);

        if (isCommandExempt(lowerCommand)) {
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

    public int getRemainingCommandCooldown(UUID playerId, String commandKey) {
        String lowerCommand = normalizeCommand(commandKey);

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

    public void setCommandCooldown(UUID playerId, String commandKey) {
        long now = System.currentTimeMillis();
        String lowerCommand = normalizeCommand(commandKey);

        commandCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(lowerCommand, now);
        commandCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put("*", now);
    }
}
