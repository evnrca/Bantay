package com.evnrca.bantay.config;

import com.evnrca.bantay.Bantay;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private final Bantay plugin;
    private FileConfiguration config;

    // Filter settings
    private boolean filipinoEnabled;
    private boolean englishEnabled;
    private boolean normalizeLeetspeak;
    private Map<String, String> aliases;
    private boolean stripRepeatedChars;
    private char censorChar;
    private boolean fixedLengthCensor;
    private boolean notifyStaff;

    // Regex patterns
    private List<String> filipinoRegexPatterns;
    private List<String> englishRegexPatterns;

    // Phonetic matching
    private boolean phoneticEnabled;
    private double phoneticThreshold;

    // ML Toxicity detection
    private boolean mlToxicityEnabled;
    private String mlToxicityEndpoint;
    private int mlToxicityTimeoutMs;
    private double mlToxicityThreshold;
    private List<String> mlToxicLabels;
    private String mlApiKeyHeader;
    private String mlApiKey;

    // Chat cooldown
    private boolean chatCooldownEnabled;
    private int chatCooldownSeconds;

    // Command cooldown
    private boolean commandCooldownEnabled;
    private int commandCooldownSeconds;
    private Map<String, Integer> commandOverrides;
    private Set<String> exemptCommands;

    // Messages (hardcoded)
    private String prefix;
    private String chatCooldownMsg;
    private String commandCooldownMsg;
    private String reloadSuccessMsg;
    private String bypassEnabledMsg;
    private String bypassDisabledMsg;
    private String playerNotFoundMsg;
    private String helpHeader;
    private String helpReload;
    private String helpBypass;
    private String helpVersion;
    private String helpHelp;
    private String helpFooter;
    private String versionMsg;

    public ConfigManager(Bantay plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.config = plugin.getConfig();

        // Filter
        filipinoEnabled = config.getBoolean("filter.filipino-enabled", true);
        englishEnabled = config.getBoolean("filter.english-enabled", true);
        normalizeLeetspeak = config.getBoolean("filter.normalize-leetspeak", true);
        stripRepeatedChars = config.getBoolean("filter.strip-repeated-chars", true);
        censorChar = config.getString("filter.censor-char", "*").charAt(0);
        fixedLengthCensor = config.getBoolean("filter.fixed-length-censor", false);
        notifyStaff = config.getBoolean("filter.notify-staff", false);

        // Regex patterns
        filipinoRegexPatterns = config.getStringList("filter.regex-patterns.filipino");
        englishRegexPatterns = config.getStringList("filter.regex-patterns.english");

        // Phonetic matching
        phoneticEnabled = config.getBoolean("filter.phonetic-matching.enabled", false);
        phoneticThreshold = config.getDouble("filter.phonetic-matching.threshold", 0.85);

        // ML Toxicity detection
        mlToxicityEnabled = config.getBoolean("filter.ml-toxicity.enabled", false);
        mlToxicityEndpoint = config.getString("filter.ml-toxicity.endpoint", "http://localhost:8000/toxicity");
        mlToxicityTimeoutMs = config.getInt("filter.ml-toxicity.timeout-ms", 2000);
        mlToxicityThreshold = config.getDouble("filter.ml-toxicity.threshold", 0.8);
        mlToxicLabels = config.getStringList("filter.ml-toxicity.toxic-labels");
        mlApiKeyHeader = config.getString("filter.ml-toxicity.api-key-header", "");
        mlApiKey = config.getString("filter.ml-toxicity.api-key", "");

        // Aliases
        aliases = new ConcurrentHashMap<>();
        if (config.isConfigurationSection("filter.aliases")) {
            for (String key : config.getConfigurationSection("filter.aliases").getKeys(false)) {
                aliases.put(key.toLowerCase(), config.getString("filter.aliases." + key).toLowerCase());
            }
        }

        // Chat cooldown
        chatCooldownEnabled = config.getBoolean("cooldown.chat.enabled", true);
        chatCooldownSeconds = config.getInt("cooldown.chat.seconds", 3);

        // Command cooldown
        commandCooldownEnabled = config.getBoolean("cooldown.command.enabled", true);
        commandCooldownSeconds = config.getInt("cooldown.command.seconds", 2);
        commandOverrides = new ConcurrentHashMap<>();
        if (config.isConfigurationSection("cooldown.command.per-command-overrides")) {
            for (String key : config.getConfigurationSection("cooldown.command.per-command-overrides").getKeys(false)) {
                commandOverrides.put(key.toLowerCase(), config.getInt("cooldown.command.per-command-overrides." + key));
            }
        }
        exemptCommands = ConcurrentHashMap.newKeySet();
        if (config.isList("cooldown.command.exempt-commands")) {
            exemptCommands.addAll(config.getStringList("cooldown.command.exempt-commands"));
        }

        // Messages (hardcoded)
        prefix = colorize("&8[&cBantay&8] &r");
        chatCooldownMsg = colorize("&cPlease wait {seconds} second(s) before chatting again.");
        commandCooldownMsg = colorize("&cCommand &e{command} &cis on cooldown. Wait {seconds} second(s).");
        reloadSuccessMsg = colorize("&aConfiguration reloaded successfully!");
        bypassEnabledMsg = colorize("&aBypass enabled for &e{player}&a.");
        bypassDisabledMsg = colorize("&cBypass disabled for &e{player}&c.");
        playerNotFoundMsg = colorize("&cPlayer '&e{player}&c' not found.");
        helpHeader = colorize("&8&m----------- &cBantay &8&m-----------");
        helpReload = colorize("&e/bantay reload &7- Reload configuration");
        helpBypass = colorize("&e/bantay bypass <player> &7- Toggle bypass for a player");
        helpVersion = colorize("&e/bantay version &7- Show plugin version");
        helpHelp = colorize("&e/bantay help &7- Show this help message");
        helpFooter = colorize("&8&m--------------------------------");
        versionMsg = colorize("&eBantay &fv{version} &7by &fevnrca");
    }

    private String colorize(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
    }

    // Getters
    public boolean isFilipinoEnabled() { return filipinoEnabled; }
    public boolean isEnglishEnabled() { return englishEnabled; }
    public boolean isNormalizeLeetspeak() { return normalizeLeetspeak; }
    public Map<String, String> getAliases() { return aliases; }
    public boolean isStripRepeatedChars() { return stripRepeatedChars; }
    public char getCensorChar() { return censorChar; }
    public boolean isFixedLengthCensor() { return fixedLengthCensor; }
    public boolean isNotifyStaff() { return notifyStaff; }

    // Regex patterns
    public List<String> getFilipinoRegexPatterns() { return filipinoRegexPatterns; }
    public List<String> getEnglishRegexPatterns() { return englishRegexPatterns; }

    // Phonetic matching
    public boolean isPhoneticEnabled() { return phoneticEnabled; }
    public double getPhoneticThreshold() { return phoneticThreshold; }

    // ML Toxicity detection
    public boolean isMlToxicityEnabled() { return mlToxicityEnabled; }
    public String getMlToxicityEndpoint() { return mlToxicityEndpoint; }
    public int getMlToxicityTimeoutMs() { return mlToxicityTimeoutMs; }
    public double getMlToxicityThreshold() { return mlToxicityThreshold; }
    public List<String> getMlToxicLabels() { return mlToxicLabels; }
    public String getMlApiKeyHeader() { return mlApiKeyHeader; }
    public String getMlApiKey() { return mlApiKey; }

    public boolean isChatCooldownEnabled() { return chatCooldownEnabled; }
    public int getChatCooldownSeconds() { return chatCooldownSeconds; }

    public boolean isCommandCooldownEnabled() { return commandCooldownEnabled; }
    public int getCommandCooldownSeconds() { return commandCooldownSeconds; }
    public Map<String, Integer> getCommandOverrides() { return commandOverrides; }
    public Set<String> getExemptCommands() { return exemptCommands; }

    public String getPrefix() { return prefix; }
    public String getChatCooldownMsg() { return chatCooldownMsg; }
    public String getCommandCooldownMsg() { return commandCooldownMsg; }
    public String getReloadSuccessMsg() { return reloadSuccessMsg; }
    public String getBypassEnabledMsg() { return bypassEnabledMsg; }
    public String getBypassDisabledMsg() { return bypassDisabledMsg; }
    public String getPlayerNotFoundMsg() { return playerNotFoundMsg; }
    public String getHelpHeader() { return helpHeader; }
    public String getHelpReload() { return helpReload; }
    public String getHelpBypass() { return helpBypass; }
    public String getHelpHelp() { return helpHelp; }
    public String getHelpVersion() { return helpVersion; }
    public String getHelpFooter() { return helpFooter; }
    public String getVersionMsg() { return versionMsg; }
}