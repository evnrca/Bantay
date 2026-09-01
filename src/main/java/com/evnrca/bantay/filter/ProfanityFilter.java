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

    private final List<Pattern> filipinoRegexPatterns = new ArrayList<>();
    private final List<Pattern> englishRegexPatterns = new ArrayList<>();
    private final Map<String, String> aliasMap = new ConcurrentHashMap<>();
    private final Map<String, String> profanitySoundexMap = new ConcurrentHashMap<>();

    public ProfanityFilter(ConfigManager config, Bantay plugin) {
        this.config = config;
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        filipinoRegexPatterns.clear();
        englishRegexPatterns.clear();
        aliasMap.clear();
        profanitySoundexMap.clear();

        if (config.isNormalizeLeetspeak()) {
            aliasMap.putAll(config.getAliases());
        }

        // Build phonetic map from regex patterns (extract base words)
        if (config.isPhoneticEnabled()) {
            buildPhoneticMapFromRegex();
        }

        if (config.isFilipinoEnabled()) {
            for (String regex : config.getFilipinoRegexPatterns()) {
                try {
                    filipinoRegexPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid Filipino regex pattern: " + regex);
                }
            }
        }

        if (config.isEnglishEnabled()) {
            for (String regex : config.getEnglishRegexPatterns()) {
                try {
                    englishRegexPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid English regex pattern: " + regex);
                }
            }
        }
    }

    private void buildPhoneticMapFromRegex() {
        // Predefined base words for phonetic matching (extracted from default regex patterns)
        String[] filipinoBaseWords = {
            "putangina", "putang ina", "tangina", "tang ina", "gago", "gaga", "ulol",
            "bobo", "tanga", "punyeta", "leche", "pakshet", "pakyu", "hayop", "hayup",
            "kupal", "tarantado", "lintik", "peste", "pucha", "puta", "inutil", "sira ulo"
        };
        String[] englishBaseWords = {
            "damn", "dammit", "hell", "shit", "shitty", "bullshit",
            "fuck", "fucker", "fucking", "motherfucker",
            "bitch", "bitches", "ass", "asshole", "asshat", "jackass", "dumbass",
            "crap", "crappy", "piss", "pissing", "pissed",
            "cock", "cunt", "twat", "pussy", "dick", "prick",
            "whore", "slut", "hoe", "skank",
            "bastard", "bollocks", "bugger", "wanker", "tosser",
            "nigger", "nigga"
        };

        for (String word : filipinoBaseWords) {
            profanitySoundexMap.put(soundex(word.toLowerCase()), word.toLowerCase());
        }
        for (String word : englishBaseWords) {
            profanitySoundexMap.put(soundex(word.toLowerCase()), word.toLowerCase());
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

        matches.addAll(matchRegexPatterns(workingMessage, filipinoRegexPatterns, "filipino-regex"));
        matches.addAll(matchRegexPatterns(workingMessage, englishRegexPatterns, "english-regex"));

        if (config.isPhoneticEnabled()) {
            matches.addAll(matchPhonetic(workingMessage));
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

    private List<MatchInfo> matchRegexPatterns(String message, List<Pattern> patterns, String source) {
        List<MatchInfo> matches = new ArrayList<>();
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                matches.add(new MatchInfo(matcher.start(), matcher.end(), matcher.group(), source + ":" + pattern.pattern()));
            }
        }
        return matches;
    }

    private List<MatchInfo> matchPhonetic(String message) {
        List<MatchInfo> matches = new ArrayList<>();
        String[] words = message.toLowerCase().split("\\s+");
        int charIndex = 0;

        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-z0-9]", "");
            if (cleanWord.length() < 3) {
                charIndex += word.length() + 1;
                continue;
            }

            String wordSoundex = soundex(cleanWord);
            for (Map.Entry<String, String> entry : profanitySoundexMap.entrySet()) {
                double similarity = soundexSimilarity(wordSoundex, entry.getKey());
                if (similarity >= config.getPhoneticThreshold()) {
                    int start = message.toLowerCase().indexOf(word, charIndex);
                    if (start >= 0) {
                        matches.add(new MatchInfo(start, start + word.length(), word, "phonetic:" + entry.getValue()));
                    }
                }
            }
            charIndex += word.length() + 1;
        }
        return matches;
    }

    private String soundex(String input) {
        if (input == null || input.isEmpty()) return "";
        String s = input.toUpperCase();
        StringBuilder res = new StringBuilder();
        res.append(s.charAt(0));

        Map<Character, Character> encodings = Map.ofEntries(
                Map.entry('B', '1'), Map.entry('F', '1'), Map.entry('P', '1'), Map.entry('V', '1'),
                Map.entry('C', '2'), Map.entry('G', '2'), Map.entry('J', '2'), Map.entry('K', '2'),
                Map.entry('Q', '2'), Map.entry('S', '2'), Map.entry('X', '2'), Map.entry('Z', '2'),
                Map.entry('D', '3'), Map.entry('T', '3'),
                Map.entry('L', '4'),
                Map.entry('M', '5'), Map.entry('N', '5'),
                Map.entry('R', '6')
        );

        char prevCode = '0';
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            char code = encodings.getOrDefault(c, '0');
            if (code != '0' && code != prevCode) {
                res.append(code);
            }
            prevCode = code;
        }

        while (res.length() < 4) res.append('0');
        return res.substring(0, 4);
    }

    private double soundexSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int matches = 0;
        for (int i = 0; i < Math.min(s1.length(), s2.length()); i++) {
            if (s1.charAt(i) == s2.charAt(i)) matches++;
        }
        return (double) matches / Math.max(s1.length(), s2.length());
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

    /**
     * Convert a word to a regex pattern that handles leetspeak variants.
     * Example: "fuck" -> "(f|F)[uU]+[cC]+[kK]+"
     * Handles: @/4->a, 3->e, 1/!->i, 0->o, $->s
     */
    public static String wordToRegex(String word) {
        if (word == null || word.trim().isEmpty()) {
            return null;
        }
        String[] parts = word.toLowerCase().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0) {
                sb.append("\\s*");
            }
            for (char c : part.toCharArray()) {
                switch (c) {
                    case 'a' -> sb.append("[@4aA]+");
                    case 'e' -> sb.append("[eE3]+");
                    case 'i' -> sb.append("[iI!1]+");
                    case 'o' -> sb.append("[oO0]+");
                    case 's' -> sb.append("[sS$]+");
                    default -> sb.append("[").append(Character.toLowerCase(c)).append(Character.toUpperCase(c)).append("]+");
                }
            }
        }

        return "\\b" + sb.toString() + "\\b";
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
