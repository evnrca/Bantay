package com.evnrca.bantay.commands;

import com.evnrca.bantay.Bantay;
import com.evnrca.bantay.filter.ProfanityFilter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BantayCommand implements CommandExecutor {

    private final Bantay plugin;

    public BantayCommand(Bantay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("bantay.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission.");
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getReloadSuccessMsg());
                return true;

            case "bypass":
                if (!sender.hasPermission("bantay.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cUsage: /bantay bypass <player>");
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    String msg = plugin.getConfigManager().getPlayerNotFoundMsg().replace("{player}", args[1]);
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + msg);
                    return true;
                }
                sender.sendMessage(plugin.getConfigManager().getPrefix() +
                        "&eUse a permission plugin (LuckPerms/PermissionsEx) to manage bantay.bypass for " + target.getName());
                return true;

            case "english":
                if (!sender.hasPermission("bantay.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cUsage: /bantay english <word>");
                    return true;
                }
                return handleAddWord(sender, args[1], "english");

            case "filipino":
                if (!sender.hasPermission("bantay.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cUsage: /bantay filipino <word>");
                    return true;
                }
                return handleAddWord(sender, args[1], "filipino");

            case "version":
                String versionMsg = plugin.getConfigManager().getVersionMsg()
                        .replace("{version}", plugin.getDescription().getVersion());
                sender.sendMessage(plugin.getConfigManager().getPrefix() + versionMsg);
                return true;

            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleAddWord(CommandSender sender, String word, String language) {
        String regex = ProfanityFilter.wordToRegex(word);
        if (regex == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cInvalid word.");
            return true;
        }

        List<String> patterns = language.equals("english")
                ? plugin.getConfigManager().getEnglishRegexPatterns()
                : plugin.getConfigManager().getFilipinoRegexPatterns();

        // Check if pattern already exists (case-insensitive)
        for (String existing : patterns) {
            if (existing.equalsIgnoreCase(regex)) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "&eWord already exists in " + language + " patterns.");
                return true;
            }
        }

        // Add to runtime list
        patterns.add(regex);

        // Also add to config.yml for persistence
        plugin.getConfig().set("filter.regex-patterns." + language, patterns);
        plugin.saveConfig();

        // Reload filter to pick up new pattern
        plugin.getProfanityFilter().reload();

        sender.sendMessage(plugin.getConfigManager().getPrefix() + "&aAdded &e" + word + "&a as regex pattern to &e" + language + "&a: &f" + regex);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getHelpHeader());
        sender.sendMessage(plugin.getConfigManager().getHelpReload());
        sender.sendMessage(plugin.getConfigManager().getHelpBypass());
        sender.sendMessage(plugin.getConfigManager().getHelpEnglish());
        sender.sendMessage(plugin.getConfigManager().getHelpFilipino());
        sender.sendMessage(plugin.getConfigManager().getHelpVersion());
        sender.sendMessage(plugin.getConfigManager().getHelpHelp());
        sender.sendMessage(plugin.getConfigManager().getHelpFooter());
    }
}