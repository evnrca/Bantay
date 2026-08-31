package com.evnrca.bantay;

import com.evnrca.bantay.commands.BantayCommand;
import com.evnrca.bantay.config.ConfigManager;
import com.evnrca.bantay.cooldown.CooldownManager;
import com.evnrca.bantay.filter.ProfanityFilter;
import com.evnrca.bantay.listeners.ChatListener;
import com.evnrca.bantay.listeners.CommandListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Bantay extends JavaPlugin {

    private ConfigManager configManager;
    private ProfanityFilter profanityFilter;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.profanityFilter = new ProfanityFilter(configManager, this);
        this.cooldownManager = new CooldownManager(configManager);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);

        getCommand("bantay").setExecutor(new BantayCommand(this));

        getLogger().info("Bantay v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Bantay disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProfanityFilter getProfanityFilter() {
        return profanityFilter;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public void reload() {
        reloadConfig();
        configManager.load();
        profanityFilter.reload();
        cooldownManager.reload();
        getLogger().info("Configuration reloaded!");
    }
}