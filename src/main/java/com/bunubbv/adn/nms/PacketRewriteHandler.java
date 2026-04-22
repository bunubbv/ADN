package com.bunubbv.adn.nms;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.bukkit.plugin.java.JavaPlugin;

public final class PacketRewriteHandler extends ChannelDuplexHandler {
    private final JavaPlugin plugin;
    private final PlayerNameRewriter rewriter;

    public PacketRewriteHandler(JavaPlugin plugin, PlayerNameRewriter rewriter) {
        this.plugin = plugin;
        this.rewriter = rewriter;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {

            if (msg instanceof ClientboundSystemChatPacket(
                    net.minecraft.network.chat.Component original, boolean overlay
            )) {
                net.minecraft.network.chat.Component modified =
                        NmsComponentBridge.rewrite(original, rewriter);

                if (!modified.equals(original)) {
                    msg = new ClientboundSystemChatPacket(modified, overlay);
                }
            }

            if (msg instanceof ClientboundPlayerInfoUpdatePacket packet) {
                if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                    super.write(ctx, msg, promise);
                    return;
                }

                var newEntries = new java.util.ArrayList<ClientboundPlayerInfoUpdatePacket.Entry>();

                for (var entry : packet.entries()) {

                    if (entry.profile() instanceof com.mojang.authlib.GameProfile profile) {

                        String originalName = profile.name();
                        String newName = rewriter.rewriteProfileName(originalName);

                        if (newName == null) newName = originalName;

                        if (!newName.equals(originalName)) {

                            com.mojang.authlib.GameProfile newProfile =
                                    new com.mojang.authlib.GameProfile(
                                            profile.id(),
                                            newName,
                                            profile.properties()
                                    );

                            entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                                    entry.profileId(),
                                    newProfile,
                                    entry.listed(),
                                    entry.latency(),
                                    entry.gameMode(),
                                    entry.displayName(),
                                    entry.showHat(),
                                    entry.listOrder(),
                                    null
                            );
                        }
                    }

                    newEntries.add(entry);
                }

                msg = new ClientboundPlayerInfoUpdatePacket(packet.actions(), newEntries);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        super.write(ctx, msg, promise);
    }
}
