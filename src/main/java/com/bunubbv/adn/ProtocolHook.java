package com.bunubbv.adn;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ProtocolHook {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private final boolean debug;

    private static final LegacyComponentSerializer legacy =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .character('§')
                    .build();

    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<String, String> lastDisplayNames;

    public ProtocolHook(JavaPlugin plugin, ProtocolManager protocolManager, boolean debug,
                        Map<String, String> lastDisplayNames) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        this.debug = debug;
        this.lastDisplayNames = lastDisplayNames;
    }

    public void register() {
        protocolManager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.SYSTEM_CHAT,
                PacketType.Play.Server.CHAT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                Component root = readComponentFromPacket(packet);
                if (root == null) return;

                if (debug) plugin.getLogger().info("--- PACKET (ORIGINAL JSON) --- " + GSON.serialize(root));

                Component modified = replaceNamesInTranslatables(root);

                if (debug) plugin.getLogger().info("--- PACKET (MODIFIED JSON) --- " + GSON.serialize(modified));

                writeComponentToPacket(packet, modified);
            }
        });
    }

    private Component readComponentFromPacket(PacketContainer packet) {
        StructureModifier<Component> comps = packet.getSpecificModifier(Component.class);
        if (!comps.getValues().isEmpty()) {
            Component c = comps.read(0);
            if (c != null) return c;
        }

        StructureModifier<WrappedChatComponent> chatComps = packet.getChatComponents();
        if (!chatComps.getValues().isEmpty()) {
            WrappedChatComponent wrapped = chatComps.read(0);
            if (wrapped != null && wrapped.getJson() != null) {
                return GSON.deserialize(wrapped.getJson());
            }
        }
        return null;
    }

    private void writeComponentToPacket(PacketContainer packet, Component component) {
        StructureModifier<Component> comps = packet.getSpecificModifier(Component.class);
        if (!comps.getValues().isEmpty()) {
            comps.write(0, component);
            return;
        }

        String json = GSON.serialize(component);
        StructureModifier<WrappedChatComponent> chatComps = packet.getChatComponents();
        if (!chatComps.getValues().isEmpty()) {
            chatComps.write(0, WrappedChatComponent.fromJson(json));
        }
    }

    private Component replacePlayerName(Component original, Player target) {
        String plain = PLAIN.serialize(original);
        if (!plain.equals(target.getName())) return original;

        return applyDisplayName(original, target.getDisplayName());
    }

    private Component replaceNamesInTranslatables(Component original) {
        return replaceNamesInTranslatables(original, false);
    }

    private Component replaceNamesInTranslatables(Component original, boolean inTranslatable) {
        List<Component> newChildren = new ArrayList<>();

        for (Component child : original.children()) {
            newChildren.add(replaceNamesInTranslatables(child, inTranslatable));
        }

        Component current = original.children(newChildren);

        if (current instanceof TranslatableComponent tr) {
            List<TranslationArgument> newArgs = new ArrayList<>();

            for (TranslationArgument arg : tr.arguments()) {
                Component comp = arg.asComponent();
                Component replaced = replaceNamesInTranslatables(comp, true);
                newArgs.add(TranslationArgument.component(replaced));
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

                if (lastDisplayNames.containsKey(plain)) {
                    return applyDisplayName(current, lastDisplayNames.get(plain));
                }
            }
        }

        return current;
    }

    private Component applyDisplayName(Component original, String displayName) {
        Style originalStyle = original.style();

        Component display = legacy.deserialize(displayName);

        Component result = display.style(
                display.style().merge(originalStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
        );

        if (originalStyle.clickEvent() != null)
            result = result.clickEvent(originalStyle.clickEvent());

        if (originalStyle.hoverEvent() != null)
            result = result.hoverEvent(originalStyle.hoverEvent());

        if (!original.children().isEmpty()) {
            List<Component> newChildren = new ArrayList<>(result.children());
            newChildren.addAll(original.children());
            result = result.children(newChildren);
        }

        return result;
    }
}
