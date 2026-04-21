package com.bunubbv.adn;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ConfigMigrator {
    private final JavaPlugin plugin;

    public ConfigMigrator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void migrateIfNeeded() {
        File dataFolder = plugin.getDataFolder();
        File configFile = new File(dataFolder, "config.yml");
        File messagesFile = new File(dataFolder, "messages.yml");

        YamlConfiguration oldCfg = YamlConfiguration.loadConfiguration(configFile);

        // Migrate if version is missing
        if (oldCfg.contains("version")) return;

        try {
            backup(configFile);

            YamlConfiguration newConfig = new YamlConfiguration();
            YamlConfiguration messages = new YamlConfiguration();

            newConfig.set("debug", false);
            newConfig.set("nickname-prefix", oldCfg.getString("options.nick-prefix", ""));
            newConfig.set("nickname-pattern", oldCfg.getString("options.nick-pattern", "[A-Za-z0-9ㄱ-ㅎㅏ-ㅣ가-힣]+"));
            newConfig.set("nickname-max-length", oldCfg.getInt("options.nick-length", 30));
            newConfig.set("tablist-nickname", oldCfg.getBoolean("options.tablist-nick", true));
            newConfig.set("version", 1);

            messages.set("prefix", oldCfg.getString("locales.info.prefix", ""));

            messages.set("info.help", oldCfg.getString("locales.info.help-message", ""));
            messages.set("info.reload", oldCfg.getString("locales.info.config-reloaded", ""));

            messages.set("error.no-permission", oldCfg.getString("locales.error.no-permission", ""));
            messages.set("error.no-format-permission", oldCfg.getString("locales.error.invalid.tags", ""));
            messages.set("error.must-be-player", oldCfg.getString("locales.error.must-be-player", ""));
            messages.set("error.invalid-player", oldCfg.getString("locales.error.invalid.player", ""));
            messages.set("error.nickname-null", oldCfg.getString("locales.error.nick.is-null", ""));
            messages.set("error.nickname-invalid", oldCfg.getString("locales.error.invalid.nick", ""));
            messages.set("error.nickname-too-long", oldCfg.getString("locales.error.invalid.nick-length", ""));
            messages.set("error.invalid-pattern", oldCfg.getString("locales.error.invalid.nick-pattern", ""));

            messages.set("nickname.set.self", oldCfg.getString("locales.nick.set.self", ""));
            messages.set("nickname.set.other-sender", oldCfg.getString("locales.nick.set.user", ""));
            messages.set("nickname.set.other-target", oldCfg.getString("locales.nick.set.by-user", ""));

            messages.set("nickname.reset.self", oldCfg.getString("locales.nick.reset.self", ""));
            messages.set("nickname.reset.other-sender", oldCfg.getString("locales.nick.reset.user", ""));
            messages.set("nickname.reset.other-target", oldCfg.getString("locales.nick.reset.by-user", ""));

            messages.set("version", 1);

            newConfig.save(configFile);
            messages.save(messagesFile);

            plugin.getLogger().info("Config migration completed.");

        } catch (Exception e) {
            plugin.getLogger().severe("Config migration failed.");
            e.printStackTrace();
        }
    }

    private void backup(File file) throws Exception {
        File backup = new File(file.getParentFile(), file.getName() + ".bak");

        Files.copy(
                file.toPath(),
                backup.toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );

        plugin.getLogger().info("Backup created: " + backup.getName());
    }
}
