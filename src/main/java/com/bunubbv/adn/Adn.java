package com.bunubbv.adn;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public final class Adn extends JavaPlugin implements Listener, TabExecutor {
    private MessageManager msg;
    private NickManager nick;
    private SqlStore store;

    private final Map<String, String> lastDisplayNames = new HashMap<>();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();

        ConfigMigrator migrator = new ConfigMigrator(this);
        ConfigMigrator.Result result = migrator.migrate();

        if (result == ConfigMigrator.Result.UNSUPPORTED) {
            getLogger().severe("Detected legacy configuration! Direct upgrade to 2.4.0 is NOT supported.");
            getLogger().severe("Please upgrade to 2.3.x first, then upgrade to 2.4.0.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (result == ConfigMigrator.Result.INVALID) {
            getLogger().severe("Invalid or unsupported config.yml detected.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (result == ConfigMigrator.Result.MIGRATED) {
            getLogger().info("Configuration migrated successfully.");
        }

        // Plugin Initialization
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        reloadConfig();
        msg = new MessageManager(this);

        try {
            store = new SqlStore(new File(getDataFolder(), "adn.db"));
            store.open();
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        nick = new NickManager(getConfig(), msg, store);

        PluginCommand command = getCommand("adn");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

            boolean debug = getConfig().getBoolean("debug", false);
            new ProtocolHook(this, protocolManager, debug, lastDisplayNames).register();
            getLogger().info("ProtocolLib detected! Packet rewrite enabled.");
        } catch (ClassNotFoundException e) {
            getLogger().warning("ProtocolLib not found. Username replacement requires ProtocolLib.");
        }
    }

    @Override
    public void onDisable() {
        if (store != null) store.close();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        String rawNick = nick.getCurrent(player.getUniqueId());

        if (rawNick != null && !rawNick.isEmpty()) {
            nick.applyNickname(player, rawNick);
        } else {
            nick.applyReset(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastDisplayNames.put(player.getName(), player.getDisplayName());

        Bukkit.getScheduler().runTaskLater(this, () ->
                lastDisplayNames.remove(player.getName()), 20 * 3); // 3 seconds
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (args.length == 0) {
            msg.send(sender, "info.help");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "set" -> handleSet(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            default -> msg.send(sender, "info.help");
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length == 1) {
            handleReset(sender, args);
            return;
        }

        if (args.length > 3) {
            msg.send(sender, "info.help");
            return;
        }

        String rawNick = args[1];

        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                msg.send(sender, "error.must-be-player");
                return;
            }

            if (!player.hasPermission("adn.set.self")) {
                msg.send(sender, "error.no-permission");
                return;
            }

            var result = nick.validateNickname(player, rawNick);
            if (nick.handleValidationError(sender, result)) return;

            nick.setCurrent(player.getUniqueId(), rawNick);
            nick.applyNickname(player, rawNick);

            msg.send(player, "nickname.set.self", rawNick, null, null);

            return;
        }

        if (!sender.hasPermission("adn.set.user")) {
            msg.send(sender, "error.no-permission");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            msg.send(sender, "error.invalid-player");
            return;
        }

        var result = nick.validateNickname(target, rawNick);
        if (nick.handleValidationError(sender, result)) return;

        nick.setCurrent(target.getUniqueId(), rawNick);
        nick.applyNickname(target, rawNick);

        msg.send(sender, "nickname.set.other-sender", rawNick, target.getName(), null);
        msg.send(target, "nickname.set.other-target", rawNick, null, sender.getName());
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                msg.send(sender, "error.must-be-player");
                return;
            }

            if (!player.hasPermission("adn.reset.self")) {
                msg.send(sender, "error.no-permission");
                return;
            }

            nick.removeCurrent(player.getUniqueId());
            nick.applyReset(player);

            msg.send(player, "nickname.reset.self");
            return;
        }

        if (args.length == 2) {
            if (!sender.hasPermission("adn.reset.user")) {
                msg.send(sender, "error.no-permission");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                msg.send(sender, "error.invalid-player");
                return;
            }

            nick.removeCurrent(target.getUniqueId());
            nick.applyReset(target);

            msg.send(sender, "nickname.reset.other-sender", null, target.getName(), null);
            msg.send(target, "nickname.reset.other-target", null, null, sender.getName());
            return;
        }

        msg.send(sender, "info.help");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("adn.reload")) {
            msg.send(sender, "error.no-permission");
            return;
        }

        reloadConfig();
        msg.reload();
        nick.reload(getConfig());

        msg.send(sender, "info.reload");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("set");
            list.add("reset");
            if (sender.hasPermission("adn.reload")) list.add("reload");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
        }
        return list;
    }
}
