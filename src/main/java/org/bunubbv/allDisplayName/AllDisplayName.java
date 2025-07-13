package org.bunubbv.allDisplayName;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class AllDisplayName extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        String deceasedName = deceased.getName();
        Component deceasedDisplayName = deceased.displayName();

        Player killer = deceased.getKiller();
        Component killerDisplayName = null;

        if (killer != null) {
            killerDisplayName = killer.displayName();
        }

        Component originalMessage = event.deathMessage();

        if (originalMessage == null) {
            return;
        }

        if (originalMessage instanceof TranslatableComponent translatable) {
            String key = translatable.key();
            List<Component> args = translatable.args();

            List<Component> modifiedArgs = new ArrayList<>();
            for (Component arg : args) {
                if (arg instanceof TextComponent textComponent) {
                    String content = textComponent.content();
                    if (content.equals(deceasedName)) {
                        modifiedArgs.add(deceasedDisplayName);
                    } else if (killer != null && content.equals(killer.getName())) {
                        modifiedArgs.add(killerDisplayName);
                    } else {
                        modifiedArgs.add(arg);
                    }
                } else {
                    modifiedArgs.add(arg);
                }
            }

            TranslatableComponent modifiedMessage = Component.translatable(key, modifiedArgs);
            event.deathMessage(modifiedMessage);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        Component playerDisplayName = player.displayName();
        Component originalMessage = event.joinMessage();

        if (originalMessage == null) {
            return;
        }

        if (originalMessage instanceof TranslatableComponent translatable) {
            String key = translatable.key();
            List<Component> args = translatable.args();

            List<Component> modifiedArgs = new ArrayList<>();
            for (Component arg : args) {
                if (arg instanceof TextComponent textComponent && textComponent.content().equals(playerName)) {
                    modifiedArgs.add(playerDisplayName);
                } else {
                    modifiedArgs.add(arg);
                }
            }

            TranslatableComponent modifiedMessage = Component.translatable(key, modifiedArgs);
            modifiedMessage = modifiedMessage.style(Style.style(NamedTextColor.YELLOW));
            event.joinMessage(modifiedMessage);
        }
    }
}
