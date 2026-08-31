package com.evnrca.bantay.filter;

import com.evnrca.bantay.Bantay;
import com.evnrca.bantay.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfanityFilter {

    private final ConfigManager config;
    private final Bantay plugin;

    private Set<String> filipinoPatterns;
    private Set<String> englishPatterns;
    private Map<String, String> aliasMap;
    private Map<String, Pattern> compiledPatterns;

    public ProfanityFilter(ConfigManager config, Bantay plugin) {
        this.config = config;
        this.plugin = plugin;
        this.aliasMap = new ConcurrentHashMap<>();
        this.compiledPatterns = new ConcurrentHashMap<>();
        reload();
    }

    public void reload() {
        filipinoPatterns = ConcurrentHashMap.newKeySet();
        englishPatterns = ConcurrentHashMap.newKeySet();
        compiledPatterns.clear();
        aliasMap.clear();

        if (config.isNormalizeLeetspeak()) {
            aliasMap.putAll(config.getAliases());
        }

        if (config.isFilipinoEnabled()) {
            for (String word : config.getFilipinoWords()) {
                String pattern = buildPattern(word);
                filipinoPatterns.add(pattern);
            }
        }

        if (config.isEnglishEnabled()) {
            for (String word : config.getEnglishWords()) {
                String pattern = buildPattern(word);
                englishPatterns.add(pattern);
            }
        }

        compilePatterns();
    }

    private String buildPattern(String word) {
        StringBuilder sb = new StringBuilder();
        String[] parts = word.toLowerCase().split("\\s+");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0) {
                sb.append("\\s*");
            }
            sb.append(Pattern.quote(part));
        }

        return "\\b" + sb.toString() + "\\b";
    }

    private void compilePatterns() {
        Set<String> allPatterns = new HashSet<>();
        allPatterns.addAll(filipinoPatterns);
        allPatterns.addAll(englishPatterns);

        for (String patternStr : allPatterns) {
            try {
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                compiledPatterns.put(patternStr, pattern);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to compile pattern: " + patternStr);
            }
        }
    }

    public FilterResult filter(String message, Player player) {
        if (player != null && player.hasPermission("bantay.bypass")) {
            return new FilterResult(message, false, Collections.emptyList());
        }

        String originalMessage = message;
        String workingMessage = message;

        if (config.isNormalizeLeetspeak()) {
            workingMessage = normalizeLeetspeak(workingMessage);
        }

        if (config.isStripRepeatedChars()) {
            workingMessage = stripRepeatedChars(workingMessage);
        }

        List<MatchInfo> matches = new ArrayList<>();

        for (Map.Entry<String, Pattern> entry : compiledPatterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(workingMessage);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String matched = matcher.group();
                matches.add(new MatchInfo(start, end, matched, entry.getKey()));
            }
        }

        if (matches.isEmpty()) {
            return new FilterResult(originalMessage, false, Collections.emptyList());
        }

        matches.sort(Comparator.comparingInt(m -> m.start));

        StringBuilder censored = new StringBuilder(workingMessage);
        int offset = 0;

        for (MatchInfo match : matches) {
            int censoredLength = config.isFixedLengthCensor() ? 4 : match.matched.length();
            String replacement = String.valueOf(config.getCensorChar()).repeat(censoredLength);

            int actualStart = match.start + offset;
            int actualEnd = match.end + offset;

            censored.replace(actualStart, actualEnd, replacement);
            offset += replacement.length() - (match.end - match.start);
        }

        return new FilterResult(censored.toString(), true, matches);
    }

    private String normalizeLeetspeak(String input) {
        StringBuilder sb = new StringBuilder(input.toLowerCase());
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            switch (c) {
                case '@', '4' -> sb.setCharAt(i, 'a');
                case '3' -> sb.setCharAt(i, 'e');
                case '1', '!' -> sb.setCharAt(i, 'i');
                case '0' -> sb.setCharAt(i, 'o');
                case '$' -> sb.setCharAt(i, 's');
            }
        }

        String normalized = sb.toString();
        for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
            normalized = normalized.replaceAll("(?i)\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        return normalized;
    }

    private String stripRepeatedChars(String input) {
        return input.replaceAll("(.)\\1{2,}", "$1$1");
    }

    public static class FilterResult {
        public final String filteredMessage;
        public final boolean wasCensored;
        public final List<MatchInfo> matches;

        public FilterResult(String filteredMessage, boolean wasCensored, List<MatchInfo> matches) {
            this.filteredMessage = filteredMessage;
            this.wasCensored = wasCensored;
            this.matches = matches;
        }
    }

    public static class MatchInfo {
        public final int start;
        public final int end;
        public final String matched;
        public final String pattern;

        public MatchInfo(int start, int end, String matched, String pattern) {
            this.start = start;
            this.end = end;
            this.matched = matched;
            this.pattern = pattern;
        }
    }
}