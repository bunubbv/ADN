package com.bunubbv.adn;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class adn extends JavaPlugin implements Listener, TabExecutor {
    private LocaleManager locale;
    private NickManager nick;
    private SqlNickStore store;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileMigrator migrator = new FileMigrator(this);
        migrator.migrateTextsFile();
        reloadConfig();

        locale = new LocaleManager(this);

        try {
            store = new SqlNickStore(new File(getDataFolder(), "adn.db"));
            store.open();

            int migrated = migrator.migrateNicksFile(store);
            if (migrated > 0) getLogger().info("Migrated " + migrated + " entries.");

        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        nick = new NickManager(getConfig(), locale, store);

        PluginCommand command = getCommand("adn");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
        Bukkit.getPluginManager().registerEvents(this, this);

        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            new ProtocolHook(this, protocolManager).register();

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

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (args.length == 0) {
            locale.send(sender, "info.help-message");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "set" -> handleSet(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            default -> locale.send(sender, "error.invalid.command");
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            locale.send(sender, "error.args.not-provided");
            return;
        }

        if (args.length > 3) {
            locale.send(sender, "error.args.too-many");
            return;
        }

        String rawNick = args[1];

        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                locale.send(sender, "error.must-be-player");
                return;
            }

            if (!player.hasPermission("adn.set.self") && !player.hasPermission("adn.use")) {
                locale.send(sender, "error.no-permission");
                return;
            }

            var result = nick.validateNickname(player, rawNick);
            if (nick.handleValidationError(sender, result, rawNick)) return;

            nick.setCurrent(player.getUniqueId(), rawNick);
            nick.applyNickname(player, rawNick);
            locale.send(player, "nick.set.self", rawNick, null, null);

            return;
        }

        if (!sender.hasPermission("adn.set.user")) {
            locale.send(sender, "error.no-permission");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            locale.send(sender, "error.invalid.player");
            return;
        }

        var result = nick.validateNickname(target, rawNick);
        if (nick.handleValidationError(sender, result, rawNick)) return;

        nick.setCurrent(target.getUniqueId(), rawNick);
        nick.applyNickname(target, rawNick);

        locale.send(sender, "nick.set.user", rawNick, target.getName(), null);
        locale.send(target, "nick.set.by-user", rawNick, null, sender.getName());
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                locale.send(sender, "error.must-be-player");
                return;
            }

            if (!player.hasPermission("adn.reset.self") && !player.hasPermission("adn.use")) {
                locale.send(sender, "error.no-permission");
                return;
            }

            nick.removeCurrent(player.getUniqueId());
            nick.applyReset(player);
            locale.send(player, "nick.reset.self");
            return;
        }

        if (args.length == 2) {
            if (!sender.hasPermission("adn.reset.user")) {
                locale.send(sender, "error.no-permission");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                locale.send(sender, "error.invalid.player");
                return;
            }

            nick.removeCurrent(target.getUniqueId());
            nick.applyReset(target);

            locale.send(sender, "nick.reset.user", null, target.getName(), null);
            locale.send(target, "nick.reset.by-user", null, null, sender.getName());
            return;
        }

        locale.send(sender, "error.args.too-many");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("adn.reload")) {
            locale.send(sender, "error.no-permission");
            return;
        }

        reloadConfig();
        locale.reload();
        nick.reload();

        locale.send(sender, "info.config-reloaded");
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
