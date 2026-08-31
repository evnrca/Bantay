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
| `filter.filipino-words` | *(seed list)* | List of Filipino profanity words/phrases |
| `filter.english-words` | `[damn, hell, shit, fuck, bitch, ass, crap]` | List of English profanity words |
| `filter.normalize-leetspeak` | `true` | Convert @/4→a, 3→e, 1/!→i, 0→o, $→s before matching |
| `filter.aliases` | *(see config)* | Custom shorthand→root mappings (e.g., `tgna`→`tangina`) |
| `filter.strip-repeated-chars` | `true` | Collapse 3+ repeated chars (e.g., `tanginaaaa`→`tangina`) |
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

| Option | Default |
|--------|---------|
| `messages.prefix` | `&8[&cBantay&8] &r` |
| `messages.chat-cooldown` | `&cPlease wait {seconds} second(s) before chatting again.` |
| `messages.command-cooldown` | `&cCommand &e{command} &cis on cooldown. Wait {seconds} second(s).` |
| `messages.reload-success` | `&aConfiguration reloaded successfully!` |
| `messages.bypass-enabled` | `&aBypass enabled for &e{player}&a.` |
| `messages.bypass-disabled` | `&cBypass disabled for &e{player}&c.` |
| `messages.player-not-found` | `&cPlayer '&e{player}&c' not found.` |
| `messages.help-header` | `&8&m----------- &cBantay &8&m-----------` |
| `messages.help-reload` | `&e/bantay reload &7- Reload configuration` |
| `messages.help-bypass` | `&e/bantay bypass <player> &7- Toggle bypass for a player` |
| `messages.help-help` | `&e/bantay help &7- Show this help message` |
| `messages.help-footer` | `&8&m--------------------------------` |

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
| `/bantay help` | `/bt help` | `bantay.admin` | Show help |

> **Note**: The `/bantay bypass` command currently only instructs you to use a permission plugin (LuckPerms, PermissionsEx) to manage the `bantay.bypass` permission for persistent bypass. Runtime-only toggling without a permission plugin is not implemented.

## Example Use Cases

### Adding a custom Filipino word to the filter

```yaml
filter:
  filipino-words:
    - "putangina"
    - "yournewword"  # Add your word here
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

- **Case-insensitive** matching using regex word boundaries (`\b`).
- **Multi-word phrases** (e.g., `putang ina`) automatically match spaced variants (`putangina`, `putang  ina`).
- **Leetspeak normalization** (when enabled): converts common substitutions before matching.
- **Alias mapping**: custom shorthand→root mappings for Filipino texting abbreviations.
- **Repeated character stripping**: collapses 3+ repeated characters before matching.

### Cooldowns

- **Chat**: Per-player timestamp stored in `ConcurrentHashMap<UUID, Long>`.
- **Commands**: Global per-player + optional per-command overrides. Exempt commands list bypasses cooldown entirely.

## License

MIT License — see [LICENSE](LICENSE) for details.

---

*Bantay — keeping your server chat clean, the Filipino way.*