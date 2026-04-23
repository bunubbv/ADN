package com.bunubbv.adn;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NickManager {
    private FileConfiguration cfg;
    private final MessageManager locale;
    private final SqlNickStore store;

    private int nickMaxLength;
    private Pattern nickPattern;
    private boolean tablistNick;

    private final Pattern tagPattern = Pattern.compile("<[^>]+>");

    public enum NickValidationResult {
        OK,
        ERROR_INVALID,
        ERROR_NICK_TOO_LONG,
        ERROR_NICK_INVALID,
        ERROR_NO_FORMAT_PERMISSION,
    }

    public NickManager(FileConfiguration cfg, MessageManager locale, SqlNickStore store) {
        this.cfg = cfg;
        this.locale = locale;
        this.store = store;

        reload(cfg);
    }

    public void reload(FileConfiguration cfg) {
        this.cfg = cfg;

        nickMaxLength = cfg.getInt("nickname-max-length", 30);

        String regex = cfg.getString("nickname-pattern", "[A-Za-z0-9ㄱ-ㅎㅏ-ㅣ가-힣]+");
        try {
            nickPattern = Pattern.compile(regex);
        } catch (Exception e) {
            nickPattern = Pattern.compile("[A-Za-z0-9ㄱ-ㅎㅏ-ㅣ가-힣]+");
        }

        tablistNick = cfg.getBoolean("tablist-nickname", true);
    }

    public void applyNickname(Player player, String rawNickname) {
        String prefix = cfg.getString("nickname-prefix", "");
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
        try {
            return store.getNick(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    public void setCurrent(UUID uuid, String raw) {
        try {
            store.setNick(uuid, raw);
        } catch (Exception ignored) {

        }
    }

    public void removeCurrent(UUID uuid) {
        try {
            store.removeNick(uuid);
        } catch (Exception ignored) {

        }
    }

    public NickValidationResult validateNickname(Player requester, String rawNickname) {
        if (!requester.hasPermission("adn.format")) {
            String tmp = rawNickname.replace("\\<", "").replace("\\>", "");
            if (tmp.contains("<") || tmp.contains(">")) {
                return NickValidationResult.ERROR_NO_FORMAT_PERMISSION;
            }
        }

        String safe = rawNickname
                .replace("\\<", "\u0001")
                .replace("\\>", "\u0002");

        String stripped = stripTags(safe);

        stripped = stripped
                .replace("\u0001", "<")
                .replace("\u0002", ">");

        if (stripped.isEmpty()) return NickValidationResult.ERROR_INVALID;
        if (stripped.length() > nickMaxLength) return NickValidationResult.ERROR_NICK_TOO_LONG;
        if (!nickPattern.matcher(stripped).matches()) return NickValidationResult.ERROR_NICK_INVALID;

        return NickValidationResult.OK;
    }

    private String stripTags(String input) {
        Matcher matcher = tagPattern.matcher(input);

        return matcher.replaceAll("");
    }

    public boolean handleValidationError(org.bukkit.command.CommandSender sender,
                                         NickValidationResult result) {
        return !switch (result) {
            case OK -> true;
            case ERROR_NICK_TOO_LONG -> {
                locale.send(sender, "error.nickname-too-long",
                        String.valueOf(nickMaxLength),
                        null, null);
                yield false;
            }
            case ERROR_NICK_INVALID -> {
                locale.send(sender, "error.nickname-invalid",
                        cfg.getString("nickname-pattern", "[A-Za-z0-9ㄱ-ㅎㅏ-ㅣ가-힣]+"),
                        null, null);
                yield false;
            }
            case ERROR_NO_FORMAT_PERMISSION -> {
                locale.send(sender, "error.no-format-permission");
                yield false;
            }
            default -> {
                locale.send(sender, "error.nickname-null");
                yield false;
            }
        };
    }
}
