package com.bunubbv.adn;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NickManager {
    private final FileConfiguration cfg;
    private final LocaleManager locale;
    private final SqlNickStore store;

    private int nickLength;
    private Pattern nickPattern;
    private boolean tablistNick;
    private int nickProtection;

    private final Pattern tagPattern = Pattern.compile("<[^>]+>");

    public enum NickValidationResult {
        OK,
        ERROR_INVALID,
        ERROR_LENGTH,
        ERROR_REGEX,
        ERROR_TAG_PERMISSION,
        ERROR_USERNAME_TAKEN
    }

    public NickManager(FileConfiguration cfg, LocaleManager locale, SqlNickStore store) {
        this.cfg = cfg;
        this.locale = locale;
        this.store = store;
        reload();
    }

    public void reload() {
        nickLength = cfg.getInt("options.nick-length", 30);

        String regex = cfg.getString("options.nick-pattern", "[가-힣a-zA-Z0-9]+");
        try { nickPattern = Pattern.compile(regex); }
        catch (Exception e) { nickPattern = Pattern.compile("[가-힣a-zA-Z0-9]+"); }

        tablistNick = cfg.getBoolean("options.tablist-nick", true);
        nickProtection = cfg.getInt("options.nick-protection", -1);
    }

    public void applyNickname(Player player, String rawNickname) {
        String prefix = cfg.getString("options.nick-prefix", "");
        String fullMini = prefix + rawNickname;
        String legacy = locale.miniMsgToLegacy(fullMini);

        player.setDisplayName(legacy);
        if (tablistNick) player.setPlayerListName(legacy);
    }

    public void applyReset(Player player) {
        player.setDisplayName(player.getName());
        if (tablistNick) player.setPlayerListName(player.getName());
    }

    public String getCurrent(UUID uuid) {
        try { return store.getNick(uuid); }
        catch (Exception e) { return null; }
    }

    public void setCurrent(UUID uuid, String raw) {
        try { store.setNick(uuid, raw); }
        catch (Exception ignored) {}
    }

    public void removeCurrent(UUID uuid) {
        try { store.removeNick(uuid); }
        catch (Exception ignored) {}
    }

    public NickValidationResult validateNickname(Player requester, String rawNickname) {
        if (!requester.hasPermission("adn.format")) {
            if (rawNickname.contains("<") || rawNickname.contains(">")) {
                return NickValidationResult.ERROR_TAG_PERMISSION;
            }
        }

        String stripped = stripTags(rawNickname);
        if (stripped == null || stripped.isEmpty()) return NickValidationResult.ERROR_INVALID;
        if (stripped.length() > nickLength) return NickValidationResult.ERROR_LENGTH;
        if (!nickPattern.matcher(stripped).matches()) return NickValidationResult.ERROR_REGEX;

        if (nickProtection >= 0) {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() == null) continue;
                if (op.getName().equalsIgnoreCase(stripped)) {
                    long lastPlayed = op.getLastPlayed();
                    if (lastPlayed == 0L) return NickValidationResult.ERROR_USERNAME_TAKEN;

                    long now = System.currentTimeMillis();
                    long diff = now - lastPlayed;
                    long days = diff / (1000L * 60 * 60 * 24);
                    if (days <= nickProtection) return NickValidationResult.ERROR_USERNAME_TAKEN;
                }
            }
        }

        return NickValidationResult.OK;
    }

    private String stripTags(String input) {
        Matcher matcher = tagPattern.matcher(input);
        return matcher.replaceAll("");
    }

    public boolean handleValidationError(org.bukkit.command.CommandSender sender,
                                         NickValidationResult result,
                                         String raw) {
        return !switch (result) {
            case OK -> true;
            case ERROR_LENGTH -> {
                locale.send(sender, "error.invalid.nick-length",
                        String.valueOf(cfg.getInt("options.nick-length", 30)),
                        null, null);
                yield false;
            }
            case ERROR_REGEX -> {
                locale.send(sender, "error.invalid.nick",
                        cfg.getString("options.nick-pattern", "[가-힣a-zA-Z0-9]+"),
                        null, null);
                yield false;
            }
            case ERROR_TAG_PERMISSION -> {
                locale.send(sender, "error.invalid.tags");
                yield false;
            }
            case ERROR_USERNAME_TAKEN -> {
                locale.send(sender, "error.nick.other-players-nick", raw, null, null);
                yield false;
            }
            default -> {
                locale.send(sender, "error.nick.is-null");
                yield false;
            }
        };
    }
}
