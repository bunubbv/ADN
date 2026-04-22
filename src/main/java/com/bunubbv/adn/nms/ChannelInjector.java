package com.bunubbv.adn.nms;

import io.netty.channel.Channel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChannelInjector {
    private static final String HANDLER_NAME = "adn";

    private ChannelInjector() {
    }

    public static void inject(Player player, JavaPlugin plugin, PlayerNameRewriter rewriter) {
        try {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
            Channel channel = connection.connection.channel;

            if (channel.pipeline().get(HANDLER_NAME) != null) {
                return;
            }

            channel.pipeline().addBefore(
                    "packet_handler",
                    HANDLER_NAME,
                    new PacketRewriteHandler(plugin, rewriter)
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to inject " + player.getName() + ": " + e.getMessage());
        }
    }

    public static void uninject(Player player) {
        try {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
            Channel channel = connection.connection.channel;

            if (channel.pipeline().get(HANDLER_NAME) != null) {
                channel.pipeline().remove(HANDLER_NAME);
            }
        } catch (Exception ignored) {

        }
    }
}
