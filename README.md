# Bantay

**Bantay** (Filipino for "guard/watchman") is a lightweight Minecraft server plugin (Paper/Spigot, Java 17+) for chat moderation, **specialized in filtering Filipino (Tagalog/Taglish) swear words and profanity**, with English filtering supported as a secondary/optional list.

## Requirements

- **Server**: Paper 1.20.6+ (or Spigot 1.20+)
- **Java**: 17 or higher
- **Build**: Maven 3.8+

## Installation

1. Download the latest `Bantay-1.0.0.jar` from the [Releases](https://github.com/evnrca/Bantay/releases) page (or build from source).
2. Drop the JAR into your server's `plugins/` folder.
3. Restart the server (or use `/bantay reload` after a reload-capable plugin manager).
4. Configure `plugins/Bantay/config.yml` to your liking.

## Configuration Reference

All options are documented in the generated `config.yml`. Below is a summary table:

### Filter Section

| Option | Default | Description |
|--------|---------|-------------|
| `filter.filipino-enabled` | `true` | Enable/disable Filipino profanity filtering |
| `filter.english-enabled` | `true` | Enable/disable English profanity filtering |
| `filter.normalize-leetspeak` | `true` | Convert @/4→a, 3→e, 1/!→i, 0→o, $→s before matching |
| `filter.aliases` | *(see config)* | Custom shorthand→root mappings (e.g., `tgna`→`tangina`) |
| `filter.strip-repeated-chars` | `true` | Collapse 3+ repeated chars (e.g., `tanginaaaa`→`tangina`) |
| `filter.regex-patterns.filipino` | *(24 patterns)* | Regex patterns for Filipino profanity (replaces word list) |
| `filter.regex-patterns.english` | *(31 patterns)* | Regex patterns for English profanity (replaces word list) |
| `filter.phonetic-matching.enabled` | `false` | Enable Soundex phonetic matching for misspellings |
| `filter.phonetic-matching.threshold` | `0.85` | Minimum phonetic similarity (0.0-1.0) |
| `filter.ml-toxicity.enabled` | `false` | Enable ML toxicity detection (external API) |
| `filter.ml-toxicity.endpoint` | `http://localhost:8000/toxicity` | API endpoint URL |
| `filter.ml-toxicity.threshold` | `0.8` | Minimum toxicity score (0.0-1.0) to censor |
| `filter.ml-toxicity.toxic-labels` | `["profanity","hate","harassment"]` | Labels that trigger censorship |
| `filter.ml-toxicity.timeout-ms` | `2000` | API request timeout in milliseconds |
| `filter.ml-toxicity.api-key-header` | `""` | Optional API key header name |
| `filter.ml-toxicity.api-key` | `""` | Optional API key value |
| `filter.censor-char` | `*` | Character used for censoring |
| `filter.fixed-length-censor` | `false` | If true, always output exactly 4 censor chars |
| `filter.notify-staff` | `false` | Silently notify online `bantay.admin` players when censored |

### Chat Cooldown Section

| Option | Default | Description |
|--------|---------|-------------|
| `cooldown.chat.enabled` | `true` | Enable chat message cooldown |
| `cooldown.chat.seconds` | `3` | Cooldown duration in seconds |

### Command Cooldown Section

| Option | Default | Description |
|--------|---------|-------------|
| `cooldown.command.enabled` | `true` | Enable command cooldown |
| `cooldown.command.seconds` | `2` | Global command cooldown in seconds |
| `cooldown.command.per-command-overrides` | `{"/spawn": 10, "/tpa": 15, "/home": 10, "/warp": 5}` | Per-command cooldown overrides (seconds) |
| `cooldown.command.exempt-commands` | `[]` | Commands never throttled (e.g., `/msg`, `/help`) |

### Messages Section

All messages support `&`-style color codes (`&c`=red, `&a`=green, `&e`=yellow, etc.).

**Configurable messages:**

| Option | Default |
|--------|---------|
| `messages.prefix` | `&8[&cBantay&8] &r` |
| `messages.chat-cooldown` | `&cPlease wait {seconds} second(s) before chatting again.` |
| `messages.command-cooldown` | `&cCommand &e{command} &cis on cooldown. Wait {seconds} second(s).` |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `bantay.bypass` | `op` | Bypasses all filters and cooldowns |
| `bantay.admin` | `op` | Access to `/bantay reload` and config commands |

## Commands

| Command | Aliases | Permission | Description |
|---------|---------|------------|-------------|
| `/bantay reload` | `/bt reload` | `bantay.admin` | Reload config.yml without restart |
| `/bantay bypass <player>` | `/bt bypass <player>` | `bantay.admin` | Toggle runtime bypass for a player |
| `/bantay english <word>` | `/bt english <word>` | `bantay.admin` | Add English profanity word (auto-regex) |
| `/bantay filipino <word>` | `/bt filipino <word>` | `bantay.admin` | Add Filipino profanity word (auto-regex) |
| `/bantay version` | `/bt version` | `bantay.admin` | Show plugin version and author |
| `/bantay help` | `/bt help` | `bantay.admin` | Show help |

> **Note**: The `/bantay bypass` command currently only instructs you to use a permission plugin (LuckPerms, PermissionsEx) to manage the `bantay.bypass` permission for persistent bypass. Runtime-only toggling without a permission plugin is not implemented.

## Example Use Cases

### Add a profanity word via command (auto-converts to regex)

```bash
/bantay english shithead
# Adds: (s|S)[hH]+[iI!1]+[tT]+[hH]+[eE3]+[@4aA]+[dD]+

/bantay filipino gago
# Adds: (g|G)[@4aA]+[gG]+[@4aA]+
```

### Add a custom regex pattern for Filipino profanity

```yaml
filter:
  regex-patterns:
    filipino:
      - "(p|P)[@4a]+[tT]+[@4a]+[nN]+[gG]+[@4a]+[iI!1]+[nN]+[@4a]+"
      - "your-custom-regex-pattern"
```

### Add a custom English regex pattern

```yaml
filter:
  regex-patterns:
    english:
      - "(s|S)[hH]+[iI!1]+[tT]+"
      - "your-custom-regex-pattern"
```

### Enable phonetic matching (Soundex)

```yaml
filter:
  phonetic-matching:
    enabled: true
    threshold: 0.85
```

### Enable ML toxicity detection

```yaml
filter:
  ml-toxicity:
    enabled: true
    endpoint: "https://your-api.example.com/toxicity"
    threshold: 0.8
    toxic-labels:
      - "profanity"
      - "hate"
      - "harassment"
    api-key-header: "Authorization"
    api-key: "Bearer YOUR_API_KEY"
```

### Stricter cooldown on `/tpa`

```yaml
cooldown:
  command:
    per-command-overrides:
      "/tpa": 30  # 30-second cooldown for /tpa
```

### Disable English filter, keep only Filipino

```yaml
filter:
  english-enabled: false
```

### Fixed-length censoring (hide word lengths)

```yaml
filter:
  fixed-length-censor: true
  censor-char: "#"
```

### Exempt essential commands from cooldown

```yaml
cooldown:
  command:
    exempt-commands:
      - "/msg"
      - "/r"
      - "/help"
      - "/rules"
```

## Building from Source

```bash
git clone https://github.com/evnrca/Bantay.git
cd Bantay
mvn clean package
```

The compiled JAR will be in `target/Bantay-1.0.0.jar`.

## How It Works

### Profanity Matching

- **Regex-based matching**: Uses comprehensive regex patterns (not simple word lists) that handle leetspeak, character repetition, and optional spacing automatically.
- **Case-insensitive** matching.
- **Leetspeak normalization** (when enabled): converts common substitutions (`@/4→a`, `3→e`, `1/!→i`, `0→o`, `$→s`) before matching.
- **Alias mapping**: custom shorthand→root mappings for Filipino texting abbreviations (e.g., `tgna`→`tangina`).
- **Repeated character stripping**: collapses 3+ repeated characters before matching (e.g., `tanginaaaa`→`tangina`).
- **Phonetic matching** (optional, Soundex): compares phonetic codes of words against profanity list; catches misspellings like `tgnina`→`tangina`.
- **ML toxicity detection** (optional, external API): async HTTP POST to configured endpoint; censors entire message if toxic.

### Default Regex Patterns

**Filipino (24 patterns):** Covers `putangina`, `putang ina`, `tangina`, `tang ina`, `gago`, `gaga`, `ulol`, `bobo`, `tanga`, `punyeta`, `leche`, `pakshet`, `pakyu`, `hayop`, `hayup`, `kupal`, `tarantado`, `lintik`, `peste`, `pucha`, `puta`, `inutil`, `sira ulo` — with leetspeak and spacing variants built-in.

**English (31 patterns):** Covers `damn`, `dammit`, `hell`, `shit`, `shitty`, `bullshit`, `fuck`, `fucker`, `fucking`, `motherfucker`, `bitch`, `bitches`, `ass`, `asshole`, `asshat`, `jackass`, `dumbass`, `crap`, `crappy`, `piss`, `pissing`, `pissed`, `cock`, `cunt`, `twat`, `pussy`, `dick`, `prick`, `whore`, `slut`, `hoe`, `skank`, `bastard`, `bollocks`, `bugger`, `wanker`, `tosser`, `nigger`, `nigga`, `N1GGA` — with leetspeak, suffixes, and phrase variants built-in.

### Cooldowns

- **Chat**: Per-player timestamp stored in `ConcurrentHashMap<UUID, Long>`.
- **Commands**: Global per-player + optional per-command overrides. Exempt commands list bypasses cooldown entirely.

## License

MIT License — see [LICENSE](LICENSE) for details.

---

*Bantay — keeping your server chat clean, the Filipino way.*