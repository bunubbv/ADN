package com.bunubbv.adn;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class adn extends JavaPlugin implements Listener {

    private enum SavingType {
        FILE,
        PDC
    }

    private SavingType saveType;
    private int nickLength;
    private Pattern nickPattern;
    private int maxSaves;
    private boolean tablistNick;
    private int nickProtection;

    private enum NickValidationResult {
        OK,
        ERROR_INVALID,
        ERROR_LENGTH,
        ERROR_REGEX,
        ERROR_TAG_PERMISSION,
        ERROR_USERNAME_TAKEN
    }

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final boolean DEBUG = false;

    private ProtocolManager protocolManager;
    private FileConfiguration localeConfig;
    private String prefixRaw;
    private File dataFile;
    private YamlConfiguration dataConfig;
    private NamespacedKey currentKey;
    private NamespacedKey savedKey;
    private final Pattern tagPattern = Pattern.compile("<[^>]+>");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("texts.yml", false);

        loadLocale();
        reloadNickConfig();
        setupStorage();

        currentKey = new NamespacedKey(this, "current_nick");
        savedKey = new NamespacedKey(this, "saved_nicks");

        if (getCommand("adn") != null) {
            Objects.requireNonNull(getCommand("adn")).setExecutor(this);
            Objects.requireNonNull(getCommand("adn")).setTabCompleter(this);
        }
        Bukkit.getPluginManager().registerEvents(this, this);

        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            protocolManager = ProtocolLibrary.getProtocolManager();
            registerPacketListener();
            getLogger().info("ProtocolLib detected! Packet rewrite enabled.");
        }
        catch (ClassNotFoundException ex) {
            getLogger().warning("ProtocolLib not found. Packet rewrite disabled.");
        }
    }

    @Override
    public void onDisable() {
        saveAll();
    }

    private void loadLocale() {
        File localeFile = new File(getDataFolder(), "texts.yml");
        localeConfig = YamlConfiguration.loadConfiguration(localeFile);
        prefixRaw = localeConfig.getString("plugin.prefix", "<gray><b>[ADN]</b></gray>");
    }

    private void reloadLocale() {
        loadLocale();
    }

    private void reloadNickConfig() {
        FileConfiguration cfg = getConfig();

        String sType = cfg.getString("save-type", "file").toUpperCase(Locale.ROOT);
        try {
            saveType = SavingType.valueOf(sType);
        }
        catch (IllegalArgumentException e) {
            saveType = SavingType.FILE;
        }

        nickLength = cfg.getInt("nick-length", 30);
        String regex = cfg.getString("nick-pattern", "[가-힣a-zA-Z0-9]+");
        try {
            nickPattern = Pattern.compile(regex);
        }
        catch (Exception e) {
            getLogger().warning("nick-pattern is malformed. Using default.");
            nickPattern = Pattern.compile("[가-힣a-zA-Z0-9]+");
        }

        maxSaves = cfg.getInt("max-saves", 5);
        tablistNick = cfg.getBoolean("tablist-nick", true);
        nickProtection = cfg.getInt("nick-protection", -1);
    }

    private void setupStorage() {
        if (saveType == SavingType.FILE) {
            dataFile = new File(getDataFolder(), "nicks.yml");
            if (!dataFile.exists()) {
                try {
                    dataFile.getParentFile().mkdirs();
                    dataFile.createNewFile();
                }
                catch (IOException e) {
                    getLogger().severe("Could not create nicks.yml");
                }
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        }
        else {
            dataFile = null;
            dataConfig = null;
        }
    }

    private void saveAll() {
        if (saveType == SavingType.FILE && dataConfig != null && dataFile != null) {
            try {
                dataConfig.save(dataFile);
            }
            catch (IOException e) {
                getLogger().severe("Failed to save nicks.yml");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String rawNick = getCurrentNickname(player.getUniqueId());
        if (rawNick != null && !rawNick.isEmpty()) {
            applyNickname(player, rawNick);
        }
        else {
            player.setDisplayName(player.getName());
            if (tablistNick) {
                player.setPlayerListName(player.getName());
            }
        }
    }

    private String getLocaleRaw(String path) {
        return localeConfig.getString(path, "");
    }

    private String applyPlaceholders(String template, String value, String target, String initiator) {
        if (template == null) return "";
        template = template.replace("<prefix>", prefixRaw);
        if (value != null) {
            template = template.replace("<value>", value);
        }
        if (target != null) {
            template = template.replace("<target>", target);
        }
        if (initiator != null) {
            template = template.replace("<initiator>", initiator);
        }
        return template;
    }

    private void sendLocale(CommandSender sender, String path) {
        sendLocale(sender, path, null, null, null);
    }

    private void sendLocale(CommandSender sender, String path, String value, String target, String initiator) {
        String tmpl = getLocaleRaw(path);
        if (tmpl.isEmpty()) return;
        tmpl = applyPlaceholders(tmpl, value, target, initiator);
        Component comp = miniMessage.deserialize(tmpl);
        String legacy = legacySerializer.serialize(comp);
        sender.sendMessage(legacy);
    }

    private String miniToLegacy(String mini) {
        Component comp = miniMessage.deserialize(mini);
        return legacySerializer.serialize(comp);
    }

    private String getCurrentNickname(UUID uuid) {
        if (saveType == SavingType.FILE) {
            if (dataConfig == null) return null;
            String path = uuid.toString() + ".current";
            return dataConfig.getString(path);
        }
        else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (!op.isOnline()) return null;
            Player p = op.getPlayer();
            if (p == null) return null;
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            return pdc.get(currentKey, PersistentDataType.STRING);
        }
    }

    private void setCurrentNickname(UUID uuid, String raw) {
        if (saveType == SavingType.FILE) {
            if (dataConfig == null) return;
            dataConfig.set(uuid.toString() + ".current", raw);
        }
        else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (!op.isOnline()) return;
            Player p = op.getPlayer();
            if (p == null) return;
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            pdc.set(currentKey, PersistentDataType.STRING, raw);
        }
    }

    private void clearCurrentNickname(UUID uuid) {
        if (saveType == SavingType.FILE) {
            if (dataConfig == null) return;
            dataConfig.set(uuid.toString() + ".current", null);
        }
        else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (!op.isOnline()) return;
            Player p = op.getPlayer();
            if (p == null) return;
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            pdc.remove(currentKey);
        }
    }

    private List<String> getSavedNicknames(UUID uuid) {
        if (saveType == SavingType.FILE) {
            if (dataConfig == null) return new ArrayList<>();
            String path = uuid.toString() + ".saved";
            return dataConfig.getStringList(path);
        }
        else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (!op.isOnline()) return new ArrayList<>();
            Player p = op.getPlayer();
            if (p == null) return new ArrayList<>();
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            String raw = pdc.get(savedKey, PersistentDataType.STRING);
            if (raw == null || raw.isEmpty()) return new ArrayList<>();
            return new ArrayList<>(Arrays.asList(raw.split("\\|\\|")));
        }
    }

    private void saveSavedNicknames(UUID uuid, List<String> list) {
        if (saveType == SavingType.FILE) {
            if (dataConfig == null) return;
            String path = uuid.toString() + ".saved";
            dataConfig.set(path, list);
        }
        else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (!op.isOnline()) return;
            Player p = op.getPlayer();
            if (p == null) return;
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            String joined = String.join("||", list);
            pdc.set(savedKey, PersistentDataType.STRING, joined);
        }
    }

    private boolean saveCurrentNicknameToList(UUID uuid, String raw) {
        List<String> saved = getSavedNicknames(uuid);
        if (saved.contains(raw)) return true;
        if (saved.size() >= maxSaves) return false;
        saved.add(raw);
        saveSavedNicknames(uuid, saved);
        return true;
    }

    private boolean deleteSavedNickname(UUID uuid, String raw) {
        List<String> saved = getSavedNicknames(uuid);
        if (!saved.remove(raw)) return false;
        saveSavedNicknames(uuid, saved);
        return true;
    }

    private void applyNickname(Player player, String rawNickname) {
        String prefix = getConfig().getString("nick-prefix", "");
        String fullMini = prefix + rawNickname;
        String legacy = miniToLegacy(fullMini);

        player.setDisplayName(legacy);
        if (tablistNick) {
            player.setPlayerListName(legacy);
        }
    }

    private void resetNickname(Player player) {
        clearCurrentNickname(player.getUniqueId());
        player.setDisplayName(player.getName());
        if (tablistNick) {
            player.setPlayerListName(player.getName());
        }
    }

    private NickValidationResult validateNickname(Player requester, String rawNickname) {
        if (!requester.hasPermission("adn.format")) {
            if (rawNickname.contains("<") || rawNickname.contains(">")) {
                return NickValidationResult.ERROR_TAG_PERMISSION;
            }
        }

        String stripped = stripTags(rawNickname);
        if (stripped == null || stripped.isEmpty()) {
            return NickValidationResult.ERROR_INVALID;
        }

        if (stripped.length() > nickLength) {
            return NickValidationResult.ERROR_LENGTH;
        }

        if (!nickPattern.matcher(stripped).matches()) {
            return NickValidationResult.ERROR_REGEX;
        }

        if (nickProtection >= 0) {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() == null) continue;
                if (op.getName().equalsIgnoreCase(stripped)) {
                    long lastPlayed = op.getLastPlayed();
                    if (lastPlayed == 0L) {
                        return NickValidationResult.ERROR_USERNAME_TAKEN;
                    }
                    long now = System.currentTimeMillis();
                    long diff = now - lastPlayed;
                    long days = diff / (1000L * 60 * 60 * 24);
                    if (days <= nickProtection) {
                        return NickValidationResult.ERROR_USERNAME_TAKEN;
                    }
                }
            }
        }

        return NickValidationResult.OK;
    }

    private String stripTags(String input) {
        Matcher matcher = tagPattern.matcher(input);
        return matcher.replaceAll("");
    }

    private boolean handleValidationError(CommandSender sender, NickValidationResult result, String raw) {
        return !switch (result) {
            case OK -> true;
            case ERROR_LENGTH -> {
                sendLocale(sender, "error.invalid.nick-length",
                        String.valueOf(getConfig().getInt("nick-length", 30)),
                        null, null);
                yield false;
            }
            case ERROR_REGEX -> {
                sendLocale(sender, "error.invalid.nick",
                        getConfig().getString("nick-pattern", "[가-힣a-zA-Z0-9]+"),
                        null, null);
                yield false;
            }
            case ERROR_TAG_PERMISSION -> {
                sendLocale(sender, "error.invalid.tags");
                yield false;
            }
            case ERROR_USERNAME_TAKEN -> {
                sendLocale(sender, "error.nick.other-players-nick", raw, null, null);
                yield false;
            }
            default -> {
                sendLocale(sender, "error.nick.is-null");
                yield false;
            }
        };
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String[] args
    ) {

        if (args.length == 0) {
            sendLocale(sender, "plugin.help-message");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        return switch (sub) {
            case "set" -> {
                handleSet(sender, args);
                yield true;
            }
            case "save" -> {
                handleSave(sender, args);
                yield true;
            }
            case "reset" -> {
                handleReset(sender, args);
                yield true;
            }
            case "remove" -> {
                handleDelete(sender, args);
                yield true;
            }
            case "reload" -> {
                handleReload(sender);
                yield true;
            }
            default -> {
                sendLocale(sender, "error.invalid.command");
                yield true;
            }
        };
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length < 3) {
            sendLocale(sender, "error.must-be-player");
            return;
        }

        if (args.length < 2) {
            sendLocale(sender, "error.args.not-provided");
            return;
        }

        if (args.length == 2) {
            Player player = (Player) sender;

            if (!player.hasPermission("adn.set.self") && !player.hasPermission("adn.use")) {
                sendLocale(sender, "error.no-permission");
                return;
            }

            String rawNick = args[1];
            NickValidationResult result = validateNickname(player, rawNick);
            if (handleValidationError(sender, result, rawNick)) {
                return;
            }

            setCurrentNickname(player.getUniqueId(), rawNick);
            applyNickname(player, rawNick);
            sendLocale(player, "nick.set.self", rawNick, null, null);

        }
        else if (args.length == 3) {
            if (!sender.hasPermission("adn.set.user")) {
                sendLocale(sender, "error.no-permission");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sendLocale(sender, "error.invalid.player");
                return;
            }

            String rawNick = args[2];
            NickValidationResult result = validateNickname(target, rawNick);
            if (handleValidationError(sender, result, rawNick)) {
                return;
            }

            setCurrentNickname(target.getUniqueId(), rawNick);
            applyNickname(target, rawNick);

            sendLocale(sender, "nick.set.user", rawNick, target.getName(), null);
            sendLocale(target, "nick.set.by-user", rawNick, null, sender.getName());

        }
        else {
            sendLocale(sender, "error.args.too-many");
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sendLocale(sender, "error.must-be-player");
                return;
            }

            if (!player.hasPermission("adn.reset.self") && !player.hasPermission("adn.use")) {
                sendLocale(sender, "error.no-permission");
                return;
            }

            resetNickname(player);
            sendLocale(player, "nick.reset.self");

        }
        else if (args.length == 2) {
            if (!sender.hasPermission("adn.reset.user")) {
                sendLocale(sender, "error.no-permission");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sendLocale(sender, "error.invalid.player");
                return;
            }

            resetNickname(target);
            sendLocale(sender, "nick.reset.user", null, target.getName(), null);
            sendLocale(target, "nick.reset.by-user", null, null, sender.getName());

        }
        else {
            sendLocale(sender, "error.args.too-many");
        }
    }

    private void handleSave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendLocale(sender, "error.must-be-player");
            return;
        }

        if (!player.hasPermission("adn.save")) {
            sendLocale(sender, "error.no-permission");
            return;
        }

        UUID uuid = player.getUniqueId();
        String current = getCurrentNickname(uuid);

        if (current == null || current.isEmpty()) {
            sendLocale(sender, "error.nick.is-null");
            return;
        }

        boolean ok = saveCurrentNicknameToList(uuid, current);
        if (!ok) {
            sendLocale(sender, "error.nick.too-many-to-save",
                    String.valueOf(getConfig().getInt("max-saves", 5)),
                    null, null);
            return;
        }

        sendLocale(sender, "nick.save", current, null, null);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendLocale(sender, "error.must-be-player");
            return;
        }

        if (!player.hasPermission("adn.remove")) {
            sendLocale(sender, "error.no-permission");
            return;
        }

        if (args.length < 2) {
            sendLocale(sender, "error.args.not-provided");
            return;
        }

        String raw = args[1];
        boolean ok = deleteSavedNickname(player.getUniqueId(), raw);
        if (!ok) {
            sendLocale(sender, "error.nick.name-nonexistent");
            return;
        }
        sendLocale(sender, "nick.remove", raw, null, null);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("adn.reload")) {
            sendLocale(sender, "error.no-permission");
            return;
        }

        reloadConfig();
        reloadLocale();
        reloadNickConfig();
        setupStorage();

        sendLocale(sender, "plugin.config-reloaded");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("set");
            list.add("save");
            list.add("reset");
            list.add("remove");
            if (sender.hasPermission("adn.reload")) {
                list.add("reload");
            }
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
            }
        }

        return list;
    }

    private void registerPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.SYSTEM_CHAT,
                PacketType.Play.Server.CHAT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                Component root = readComponentFromPacket(packet);
                if (root == null) {
                    return;
                }

                if (DEBUG) {
                    getLogger().info("---- PACKET (ORIGINAL) ---- " + PLAIN.serialize(root));
                }

                Component modified = replaceNamesInTranslatables(root);

                if (modified.equals(root)) {
                    return;
                }

                if (DEBUG) {
                    getLogger().info("---- PACKET (MODIFIED) ---- " + PLAIN.serialize(modified));
                }

                writeComponentToPacket(packet, modified);
            }
        });
    }

    private Component readComponentFromPacket(PacketContainer packet) {
        StructureModifier<Component> comps = packet.getSpecificModifier(Component.class);
        if (!comps.getValues().isEmpty()) {
            Component c = comps.read(0);
            if (c != null) {
                return c;
            }
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

    private Component replacePlayerNameLeaf(Component original, Player target) {
        String plain = PLAIN.serialize(original);
        if (!plain.equals(target.getName())) {
            return original;
        }

        Style originalStyle = original.style();
        Component display = target.displayName();

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
            List<Component> newArgs = new ArrayList<>();
            for (Component arg : tr.args()) {
                newArgs.add(replaceNamesInTranslatables(arg, true));
            }
            current = tr.toBuilder().args(newArgs).build();
        }

        if (inTranslatable) {
            String plain = PLAIN.serialize(current);
            if (!plain.isEmpty()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plain.equals(p.getName())) {
                        return replacePlayerNameLeaf(current, p);
                    }
                }
            }
        }

        return current;
    }
}