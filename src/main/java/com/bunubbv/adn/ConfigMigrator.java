package com.bunubbv.adn;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public final class ConfigMigrator {
    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final int CURRENT_MESSAGE_VERSION = 1;

    private final JavaPlugin plugin;

    public ConfigMigrator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public enum Result {
        OK,
        INVALID,
        MIGRATED,
        UNSUPPORTED
    }

    public Result migrate() {
        File dataFolder = plugin.getDataFolder();
        File configFile = new File(dataFolder, "config.yml");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // check texts.yml exists
        if (new File(dataFolder, "texts.yml").exists()) {
            return Result.UNSUPPORTED;
        }

        int configVersion = config.getInt("version", 0);
        boolean migrated = false;

        if (configVersion == 0) {
            // check 2.3.x config.yml
            if (!config.isConfigurationSection("locales")
                    || !config.isConfigurationSection("options")) {
                return Result.INVALID;
            }

            migrateConfigV0toV1();
            configVersion = 1;
            migrated = true;
        }

//        while (configVersion < CURRENT_CONFIG_VERSION) {
//            switch (configVersion) {
//                case 1 -> {
//                    migrateConfigV1toV2(configFile);
//                    configVersion = 2;
//                    migrated = true;
//                }
//                default -> throw new IllegalStateException("Unknown config.yml version: " + configVersion);
//            }
//        }

//        File messagesFile = new File(dataFolder, "messages.yml");
//
//        if (messagesFile.exists()) {
//            YamlConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
//            int messageVersion = messages.getInt("version", 1);
//
//            while (messageVersion < CURRENT_MESSAGE_VERSION) {
//                switch (messageVersion) {
//                    case 1 -> {
//                        migrateMessageV1toV2(messagesFile);
//                        messageVersion = 2;
//                        migrated = true;
//                    }
//                    default -> throw new IllegalStateException("Unknown messages.yml version: " + messageVersion);
//                }
//            }
//        }

        return migrated ? Result.MIGRATED : Result.OK;
    }

//    public void migrateConfigV1toV2(File configFile) {
//        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
//
//        try {
//            config.set("version", 2);
//            config.save(configFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void migrateMessageV1toV2(File messagesFile) {
//        YamlConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
//
//        try {
//            messages.set("version", 2);
//            messages.save(messagesFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void migrateConfigV0toV1() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);

        try {
            YamlConfiguration newConfig = new YamlConfiguration();
            YamlConfiguration messages = new YamlConfiguration();

            newConfig.set("debug", false);
            newConfig.set("nickname-prefix", oldConfig.getString("options.nick-prefix", ""));
            newConfig.set("nickname-pattern", oldConfig.getString("options.nick-pattern", "[A-Za-z0-9ㄱ-ㅎㅏ-ㅣ가-힣]+"));
            newConfig.set("nickname-max-length", oldConfig.getInt("options.nick-length", 30));
            newConfig.set("tablist-nickname", oldConfig.getBoolean("options.tablist-nick", true));
            newConfig.set("version", 1);

            messages.set("prefix", oldConfig.getString("locales.info.prefix", ""));

            messages.set("info.help", oldConfig.getString("locales.info.help-message", ""));
            messages.set("info.reload", oldConfig.getString("locales.info.config-reloaded", ""));

            messages.set("error.no-permission", oldConfig.getString("locales.error.no-permission", ""));
            messages.set("error.no-format-permission", oldConfig.getString("locales.error.invalid.tags", ""));
            messages.set("error.must-be-player", oldConfig.getString("locales.error.must-be-player", ""));
            messages.set("error.invalid-player", oldConfig.getString("locales.error.invalid.player", ""));
            messages.set("error.nickname-null", oldConfig.getString("locales.error.nick.is-null", ""));
            messages.set("error.nickname-invalid", oldConfig.getString("locales.error.invalid.nick", ""));
            messages.set("error.nickname-too-long", oldConfig.getString("locales.error.invalid.nick-length", ""));
            messages.set("error.invalid-pattern", oldConfig.getString("locales.error.invalid.nick-pattern", ""));

            messages.set("nickname.set.self", oldConfig.getString("locales.nick.set.self", ""));
            messages.set("nickname.set.other-sender", oldConfig.getString("locales.nick.set.user", ""));
            messages.set("nickname.set.other-target", oldConfig.getString("locales.nick.set.by-user", ""));

            messages.set("nickname.reset.self", oldConfig.getString("locales.nick.reset.self", ""));
            messages.set("nickname.reset.other-sender", oldConfig.getString("locales.nick.reset.user", ""));
            messages.set("nickname.reset.other-target", oldConfig.getString("locales.nick.reset.by-user", ""));

            messages.set("version", 1);

            newConfig.save(configFile);
            messages.save(messagesFile);

            plugin.getLogger().info("Config migration completed.");
        } catch (Exception e) {
            plugin.getLogger().severe("Config migration failed.");
            e.printStackTrace();
        }
    }
}
