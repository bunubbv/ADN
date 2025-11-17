package org.bunubbv.allDisplayName;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class AllDisplayName extends JavaPlugin implements Listener {

    private static final boolean DEBUG = false;
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    private static void debugPrintComponent(Component component, int depth) {
        String indent = "  ".repeat(depth);
        Bukkit.getLogger().info(indent + "Component: " + component.getClass().getSimpleName());

        if (component instanceof TextComponent text) {
            Bukkit.getLogger().info(indent + "  content = \"" + text.content() + "\"");
        }

        if (component instanceof TranslatableComponent tr) {
            Bukkit.getLogger().info(indent + "  key = " + tr.key());
            Bukkit.getLogger().info(indent + "  args size = " + tr.args().size());

            List<Component> args = tr.args();
            for (int i = 0; i < args.size(); i++) {
                Bukkit.getLogger().info(indent + "  arg[" + i + "]:");
                debugPrintComponent(args.get(i), depth + 2);
            }
        }

        Style st = component.style();
        if (st != Style.empty()) {
            Bukkit.getLogger().info(indent + "  style:");
            if (st.color() != null) {
                Bukkit.getLogger().info(indent + "    color = " + st.color());
            }
            if (st.clickEvent() != null) {
                Bukkit.getLogger().info(indent + "    click = " +
                        st.clickEvent().action() + " -> " + st.clickEvent().value());
            }
            if (st.hoverEvent() != null) {
                Bukkit.getLogger().info(indent + "    hover = " +
                        st.hoverEvent().action());
            }
        }

        if (!component.children().isEmpty()) {
            Bukkit.getLogger().info(indent + "  children:");
            for (Component child : component.children()) {
                debugPrintComponent(child, depth + 1);
            }
        }
    }

    private Component replacePlayerNameComponent(Component originalArg, String targetName, Component displayName) {
        String plain = PLAIN.serialize(originalArg);

        if (!plain.equals(targetName)) {
            return originalArg;
        }

        Style originalStyle = originalArg.style();

        Component result = displayName.style(
                displayName.style().merge(originalStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
        );

        if (originalStyle.clickEvent() != null) {
            result = result.clickEvent(originalStyle.clickEvent());
        }

        if (originalStyle.hoverEvent() != null) {
            result = result.hoverEvent(originalStyle.hoverEvent());
        }

        return result;
    }

    private Component replaceNamesInTranslatableMessage(Component original, Player main, Player other) {
        if (!(original instanceof TranslatableComponent translatable)) {
            return original;
        }

        String mainName = main.getName();
        Component mainDisplay = main.displayName();

        String otherName = null;
        Component otherDisplay = null;

        if (other != null) {
            otherName = other.getName();
            otherDisplay = other.displayName();
        }

        List<Component> modifiedArgs = new ArrayList<>();

        for (Component arg : translatable.args()) {
            Component modified = arg;

            String plain = PLAIN.serialize(arg);
            if (plain.equals(mainName)) {
                modified = replacePlayerNameComponent(arg, mainName, mainDisplay);
            }
            else if (plain.equals(otherName)) {
                modified = replacePlayerNameComponent(arg, otherName, otherDisplay);
            }

            modifiedArgs.add(modified);
        }

        return translatable.toBuilder()
                .args(modifiedArgs)
                .build();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        Player killer = deceased.getKiller();
        Component originalMessage = event.deathMessage();

        if (originalMessage == null) {
            return;
        }

        if (DEBUG) {
            Bukkit.getLogger().info("---- DEATH MESSAGE (ORIGINAL) ----");
            debugPrintComponent(originalMessage, 0);
        }

        Component modified = replaceNamesInTranslatableMessage(originalMessage, deceased, killer);
        event.deathMessage(modified);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Component originalMessage = event.joinMessage();

        if (originalMessage == null) {
            return;
        }

        if (DEBUG) {
            Bukkit.getLogger().info("---- JOIN MESSAGE (ORIGINAL) ----");
            debugPrintComponent(originalMessage, 0);
        }

        Component modified = replaceNamesInTranslatableMessage(originalMessage, player, null);
        modified = modified.style(style -> style.color(NamedTextColor.YELLOW));
        event.joinMessage(modified);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Component originalMessage = event.quitMessage();

        if (originalMessage == null) {
            return;
        }

        if (DEBUG) {
            Bukkit.getLogger().info("---- QUIT MESSAGE (ORIGINAL) ----");
            debugPrintComponent(originalMessage, 0);
        }

        Component modified = replaceNamesInTranslatableMessage(originalMessage, player, null);
        event.quitMessage(modified);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        Component originalMessage = event.leaveMessage();

        if (DEBUG) {
            Bukkit.getLogger().info("---- KICK MESSAGE (ORIGINAL) ----");
            debugPrintComponent(originalMessage, 0);
        }

        Component modified = replaceNamesInTranslatableMessage(originalMessage, player, null);
        event.leaveMessage(modified);
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Component originalMessage = event.message();

        if (originalMessage == null) {
            return;
        }

        if (DEBUG) {
            Bukkit.getLogger().info("---- ADVANCEMENT MESSAGE (ORIGINAL) ----");
            debugPrintComponent(originalMessage, 0);
        }

        Component modified = replaceNamesInTranslatableMessage(originalMessage, player, null);
        event.message(modified);
    }
}
