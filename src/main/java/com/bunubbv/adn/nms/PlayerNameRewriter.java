package com.bunubbv.adn.nms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlayerNameRewriter {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .character('§')
                    .build();

    public Component replaceNamesInTranslatables(Component original) {
        return replaceNamesInTranslatables(original, false);
    }

    private Component replaceNamesInTranslatables(Component original, boolean inTranslatable) {
        List<Component> newChildren = new ArrayList<>();
        for (Component child : original.children()) {
            newChildren.add(replaceNamesInTranslatables(child, inTranslatable));
        }
        Component current = original.children(newChildren);

        if (current instanceof TranslatableComponent tr) {
            List<Component> newArgs = new ArrayList<>();
            for (Component arg : tr.args()) {
                newArgs.add(replaceNamesInTranslatables(arg, true));
            }

            current = tr.toBuilder().arguments(newArgs).build();
        }

        if (inTranslatable) {
            String plain = PLAIN.serialize(current);
            if (!plain.isEmpty()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plain.equals(p.getName())) {
                        return replacePlayerName(current, p);
                    }
                }
            }
        }

        return current;
    }

    private Component replacePlayerName(Component original, Player target) {
        String plain = PLAIN.serialize(original);
        if (!plain.equals(target.getName())) return original;

        Style originalStyle = original.style();
        Component display = LEGACY.deserialize(target.getDisplayName());

        Component result = display.style(
                display.style().merge(originalStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
        );

        if (originalStyle.clickEvent() != null) {
            result = result.clickEvent(originalStyle.clickEvent());
        }

        if (originalStyle.hoverEvent() != null) {
            result = result.hoverEvent(originalStyle.hoverEvent());
        }

        if (!original.children().isEmpty()) {
            List<Component> newChildren = new ArrayList<>(result.children());
            newChildren.addAll(original.children());
            result = result.children(newChildren);
        }

        return result;
    }

    public String rewriteProfileName(String originalName) {
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (originalName.equals(player.getName())) {
                String display = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                .deserialize(player.getDisplayName()));

                if (display.length() > 16) {
                    display = display.substring(0, 16);
                }
                return display;
            }
        }
        return originalName;
    }
}
