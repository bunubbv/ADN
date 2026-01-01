package com.bunubbv.adn;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public final class FileMigrator {
    private final JavaPlugin plugin;

    public FileMigrator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void migrateTextsFile() {
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();

        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) return;

        File textsFile = new File(dataFolder, "texts.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);

        boolean changed = false;

        if (cfg.isConfigurationSection("options") || cfg.isConfigurationSection("locales")) {
            return;
        }

        if (cfg.contains("nick-prefix")) {
            cfg.set("options.nick-prefix", cfg.get("nick-prefix"));
            cfg.set("nick-prefix", null);
            changed = true;
        }
        if (cfg.contains("nick-length")) {
            cfg.set("options.nick-length", cfg.get("nick-length"));
            cfg.set("nick-length", null);
            changed = true;
        }
        if (cfg.contains("nick-protection")) {
            cfg.set("options.nick-protection", cfg.get("nick-protection"));
            cfg.set("nick-protection", null);
            changed = true;
        }
        if (cfg.contains("nick-pattern")) {
            cfg.set("options.nick-pattern", cfg.get("nick-pattern"));
            cfg.set("nick-pattern", null);
            changed = true;
        }
        if (cfg.contains("tablist-nick")) {
            cfg.set("options.tablist-nick", cfg.get("tablist-nick"));
            cfg.set("tablist-nick", null);
            changed = true;
        }

        if (cfg.contains("save-type")) {
            cfg.set("save-type", null);
            changed = true;
        }
        if (cfg.contains("max-saves")) {
            cfg.set("max-saves", null);
            changed = true;
        }

        if (textsFile.exists()) {
            YamlConfiguration texts = YamlConfiguration.loadConfiguration(textsFile);

            if (texts.isConfigurationSection("plugin")) {
                for (String k : texts.getConfigurationSection("plugin").getKeys(true)) {
                    String full = "plugin." + k;
                    if (texts.isConfigurationSection(full)) continue;
                    cfg.set("locales.info." + k, texts.get(full));
                    changed = true;
                }
            }

            if (texts.isConfigurationSection("error")) {
                for (String k : texts.getConfigurationSection("error").getKeys(true)) {
                    String full = "error." + k;
                    if (texts.isConfigurationSection(full)) continue;
                    cfg.set("locales.error." + k, texts.get(full));
                    changed = true;
                }
            }

            if (texts.isConfigurationSection("nick")) {
                for (String k : texts.getConfigurationSection("nick").getKeys(true)) {
                    String full = "nick." + k;
                    if (texts.isConfigurationSection(full)) continue;
                    cfg.set("locales.nick." + k, texts.get(full));
                    changed = true;
                }
            }
        }

        if (!changed) return;

        try {
            cfg.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save migrated config.yml");
            e.printStackTrace();
            return;
        }

        if (textsFile.exists()) {
            File backupFile = new File(dataFolder, "texts.yml.bak");
            if (backupFile.exists()) backupFile.delete();
            if (!textsFile.renameTo(backupFile)) {
                plugin.getLogger().warning("Failed to rename texts.yml to texts.yml.bak");
            }
        }

        plugin.reloadConfig();
    }

    public int migrateNicksFile(SqliteNickStore store) throws Exception {
        File dataFolder = plugin.getDataFolder();
        File nickFile = new File(dataFolder, "nicks.yml");
        if (!nickFile.exists()) return 0;

        YamlConfiguration nickData = YamlConfiguration.loadConfiguration(nickFile);

        int migrated = 0;
        for (String key : nickData.getKeys(false)) {
            String current = nickData.getString(key + ".current", null);
            if (current == null || current.isEmpty()) continue;

            try {
                UUID uuid = UUID.fromString(key);
                store.setNick(uuid, current);
                migrated++;
            } catch (IllegalArgumentException ignored) {}
        }

        File backupFile = new File(dataFolder, "nicks.yml.bak");
        if (backupFile.exists()) backupFile.delete();
        if (!nickFile.renameTo(backupFile)) {
            plugin.getLogger().warning("Failed to rename nicks.yml to nicks.yml.bak");
        }

        return migrated;
    }
}
