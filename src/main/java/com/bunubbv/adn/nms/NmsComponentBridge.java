package com.bunubbv.adn.nms;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.ComponentSerialization;

public final class NmsComponentBridge {

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private NmsComponentBridge() {}

    public static net.minecraft.network.chat.Component rewrite(
            net.minecraft.network.chat.Component original,
            PlayerNameRewriter rewriter
    ) {
        try {
            JsonElement jsonTree = ComponentSerialization.CODEC.encodeStart(
                    JsonOps.INSTANCE,
                    original
            ).getOrThrow();

            Component adventure = GSON.deserializeFromTree(jsonTree);

            Component modified = rewriter.replaceNamesInTranslatables(adventure);

            if (modified.equals(adventure)) {
                return original;
            }

            JsonElement newTree = GSON.serializeToTree(modified);

            return ComponentSerialization.CODEC.parse(
                    JsonOps.INSTANCE,
                    newTree
            ).getOrThrow();

        } catch (Throwable t) {
            t.printStackTrace();
            return original;
        }
    }
}
