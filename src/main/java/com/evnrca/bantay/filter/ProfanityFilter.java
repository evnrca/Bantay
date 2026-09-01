package com.evnrca.bantay.filter;

import com.evnrca.bantay.Bantay;
import com.evnrca.bantay.config.ConfigManager;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfanityFilter {

    private final ConfigManager config;
    private final Bantay plugin;

    private final Map<String, Pattern> compiledWordPatterns = new ConcurrentHashMap<>();
    private final List<Pattern> filipinoRegexPatterns = new ArrayList<>();
    private final List<Pattern> englishRegexPatterns = new ArrayList<>();
    private final Map<String, String> aliasMap = new ConcurrentHashMap<>();
    private final Map<String, String> profanitySoundexMap = new ConcurrentHashMap<>();

    private final HttpClient httpClient;
    private final ExecutorService mlExecutor;

    public ProfanityFilter(ConfigManager config, Bantay plugin) {
        this.config = config;
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getMlToxicityTimeoutMs()))
                .build();
        this.mlExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Bantay-ML-Toxicity");
            t.setDaemon(true);
            return t;
        });
        reload();
    }

    public void reload() {
        compiledWordPatterns.clear();
        filipinoRegexPatterns.clear();
        englishRegexPatterns.clear();
        aliasMap.clear();
        profanitySoundexMap.clear();

        if (config.isNormalizeLeetspeak()) {
            aliasMap.putAll(config.getAliases());
        }

        if (config.isFilipinoEnabled()) {
            for (String word : config.getFilipinoWords()) {
                String pattern = buildWordPattern(word);
                compiledWordPatterns.put(pattern, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                if (config.isPhoneticEnabled()) {
                    profanitySoundexMap.put(soundex(word.toLowerCase()), word.toLowerCase());
                }
            }
            for (String regex : config.getFilipinoRegexPatterns()) {
                try {
                    filipinoRegexPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid Filipino regex pattern: " + regex);
                }
            }
        }

        if (config.isEnglishEnabled()) {
            for (String word : config.getEnglishWords()) {
                String pattern = buildWordPattern(word);
                compiledWordPatterns.put(pattern, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                if (config.isPhoneticEnabled()) {
                    profanitySoundexMap.put(soundex(word.toLowerCase()), word.toLowerCase());
                }
            }
            for (String regex : config.getEnglishRegexPatterns()) {
                try {
                    englishRegexPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid English regex pattern: " + regex);
                }
            }
        }
    }

    private String buildWordPattern(String word) {
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

        matches.addAll(matchWordPatterns(workingMessage));
        matches.addAll(matchRegexPatterns(workingMessage, filipinoRegexPatterns, "filipino-regex"));
        matches.addAll(matchRegexPatterns(workingMessage, englishRegexPatterns, "english-regex"));

        if (config.isPhoneticEnabled()) {
            matches.addAll(matchPhonetic(workingMessage));
        }

        if (matches.isEmpty()) {
            if (config.isMlToxicityEnabled()) {
                return checkMlToxicityAsync(originalMessage, workingMessage, matches);
            }
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

        String censoredMessage = censored.toString();

        if (config.isMlToxicityEnabled()) {
            return checkMlToxicityAsync(originalMessage, censoredMessage, matches);
        }

        return new FilterResult(censoredMessage, true, matches);
    }

    private List<MatchInfo> matchWordPatterns(String message) {
        List<MatchInfo> matches = new ArrayList<>();
        for (Map.Entry<String, Pattern> entry : compiledWordPatterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(message);
            while (matcher.find()) {
                matches.add(new MatchInfo(matcher.start(), matcher.end(), matcher.group(), entry.getKey()));
            }
        }
        return matches;
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

    private FilterResult checkMlToxicityAsync(String originalMessage, String currentMessage, List<MatchInfo> currentMatches) {
        CompletableFuture<FilterResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return callMlToxicityApi(originalMessage, currentMessage, currentMatches);
            } catch (Exception e) {
                plugin.getLogger().warning("ML toxicity check failed: " + e.getMessage());
                return new FilterResult(currentMessage, !currentMatches.isEmpty(), currentMatches);
            }
        }, mlExecutor);

        try {
            return future.get(config.getMlToxicityTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("ML toxicity check timeout: " + e.getMessage());
            return new FilterResult(currentMessage, !currentMatches.isEmpty(), currentMatches);
        }
    }

    private FilterResult callMlToxicityApi(String originalMessage, String currentMessage, List<MatchInfo> currentMatches) throws Exception {
        String jsonBody = String.format("{\"text\":%s}", toJsonString(originalMessage));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(config.getMlToxicityEndpoint()))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(config.getMlToxicityTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        String apiKeyHeader = config.getMlApiKeyHeader();
        String apiKey = config.getMlApiKey();
        if (!apiKeyHeader.isEmpty() && !apiKey.isEmpty()) {
            requestBuilder.header(apiKeyHeader, apiKey);
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            plugin.getLogger().warning("ML toxicity API returned status: " + response.statusCode());
            return new FilterResult(currentMessage, !currentMatches.isEmpty(), currentMatches);
        }

        return parseMlResponse(response.body(), originalMessage, currentMessage, currentMatches);
    }

    private String toJsonString(String input) {
        return "\"" + input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private FilterResult parseMlResponse(String json, String originalMessage, String currentMessage, List<MatchInfo> currentMatches) {
        try {
            boolean toxic = false;
            double score = 0.0;
            List<String> labels = new ArrayList<>();

            int toxicIdx = json.indexOf("\"toxic\"");
            if (toxicIdx >= 0) {
                int colonIdx = json.indexOf(':', toxicIdx);
                String val = json.substring(colonIdx + 1).trim();
                toxic = val.startsWith("true");
            }

            int scoreIdx = json.indexOf("\"score\"");
            if (scoreIdx >= 0) {
                int colonIdx = json.indexOf(':', scoreIdx);
                int commaIdx = json.indexOf(',', colonIdx);
                if (commaIdx < 0) commaIdx = json.indexOf('}', colonIdx);
                String val = json.substring(colonIdx + 1, commaIdx).trim();
                score = Double.parseDouble(val);
            }

            int labelsIdx = json.indexOf("\"labels\"");
            if (labelsIdx >= 0) {
                int bracketIdx = json.indexOf('[', labelsIdx);
                int endBracketIdx = json.indexOf(']', bracketIdx);
                if (bracketIdx >= 0 && endBracketIdx > bracketIdx) {
                    String labelsStr = json.substring(bracketIdx + 1, endBracketIdx);
                    for (String label : labelsStr.split(",")) {
                        label = label.trim().replace("\"", "");
                        if (!label.isEmpty()) labels.add(label);
                    }
                }
            }

            boolean shouldCensor = toxic && score >= config.getMlToxicityThreshold();
            if (shouldCensor && !config.getMlToxicLabels().isEmpty()) {
                shouldCensor = labels.stream().anyMatch(config.getMlToxicLabels()::contains);
            }

            if (shouldCensor) {
                List<MatchInfo> allMatches = new ArrayList<>(currentMatches);
                allMatches.add(new MatchInfo(0, originalMessage.length(), originalMessage, "ml-toxicity:score=" + score + ",labels=" + labels));
                return new FilterResult(censorFullMessage(currentMessage), true, allMatches);
            }

            return new FilterResult(currentMessage, !currentMatches.isEmpty(), currentMatches);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse ML response: " + e.getMessage());
            return new FilterResult(currentMessage, !currentMatches.isEmpty(), currentMatches);
        }
    }

    private String censorFullMessage(String message) {
        int length = config.isFixedLengthCensor() ? 4 : message.length();
        return String.valueOf(config.getCensorChar()).repeat(length);
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