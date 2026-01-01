package com.bunubbv.adn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LocaleManager {
    private final JavaPlugin plugin;
    private final MiniMessage miniMsg = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    private FileConfiguration locale;
    private String prefix;

    public LocaleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File langFile = new File(plugin.getDataFolder(), "config.yml");
        locale = YamlConfiguration.loadConfiguration(langFile);
        prefix = locale.getString("locales.info.prefix", "");
    }

    private String applyTemplates(
            String template,
            String value,
            String target,
            String initiator) {
        if (template == null) return "";
        template = template.replace("<prefix>", prefix);

        if (value != null) template = template.replace("<value>", value);
        if (target != null) template = template.replace("<target>", target);
        if (initiator != null) template = template.replace("<initiator>", initiator);
        return template;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null, null, null);
    }

    public void send(CommandSender sender, String path, String value, String target, String initiator) {
        String template = locale.getString("locales." + path, "");
        if (template.isEmpty()) return;

        template = applyTemplates(template, value, target, initiator);
        Component component = miniMsg.deserialize(template);
        sender.sendMessage(legacy.serialize(component));
    }

    public String miniMsgToLegacy(String miniMessage) {
        Component component = miniMsg.deserialize(miniMessage);
        return legacy.serialize(component);
    }
}
