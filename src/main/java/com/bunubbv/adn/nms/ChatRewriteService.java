package com.bunubbv.adn.nms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatRewriteService {
    private final JavaPlugin plugin;
    private final PlayerNameRewriter rewriter;

    public ChatRewriteService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rewriter = new PlayerNameRewriter();
    }

    public void enable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
        }
    }

    public void disable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            uninject(player);
        }
    }

    public void inject(Player player) {
        ChannelInjector.inject(player, plugin, rewriter);
    }

    public void uninject(Player player) {
        ChannelInjector.uninject(player);
    }

    public void refreshPlayer(Player target) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String originalName = target.getName();

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                viewer.addAdditionalChatCompletions(
                        java.util.List.of(originalName)
                );
            }

            var nmsTarget = ((org.bukkit.craftbukkit.entity.CraftPlayer) target).getHandle();
            var uuid = nmsTarget.getUUID();

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                var nmsViewer = ((org.bukkit.craftbukkit.entity.CraftPlayer) viewer).getHandle();

                nmsViewer.connection.send(
                        new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(
                                java.util.List.of(uuid)
                        )
                );

                nmsViewer.connection.send(
                        net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
                                .createPlayerInitializing(java.util.List.of(nmsTarget))
                );
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer.equals(target)) {
                        continue;
                    }

                    viewer.hidePlayer(plugin, target);
                    viewer.showPlayer(plugin, target);
                }
            }, 1L);
        });
    }
}
