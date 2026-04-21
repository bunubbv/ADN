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

    private FileConfiguration messages;
    private String prefix;

    private static final LegacyComponentSerializer legacy =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .character('§')
                    .build();

    public LocaleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");

        messages = YamlConfiguration.loadConfiguration(file);
        prefix = messages.getString("prefix", "");
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
        String template = messages.getString(path, "");
        if (template.isEmpty()) return;

        template = applyTemplates(template, value, target, initiator);

        Component component = miniMsg.deserialize(template);
        sender.sendMessage(legacy.serialize(component));
    }

    public String miniMsgToLegacy(String input) {
        Component component = miniMsg.deserialize(input);
        return legacy.serialize(component) + "§r";
    }
}
