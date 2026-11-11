package com.syntaxphoenix.spigot.smoothtimber.utilities;

import static com.syntaxphoenix.spigot.smoothtimber.SmoothTimber.COMMANDS;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.syntaxphoenix.spigot.smoothtimber.SmoothTimber;
import com.syntaxphoenix.spigot.smoothtimber.command.CommandRedirect;
import com.syntaxphoenix.spigot.smoothtimber.command.commands.EnchantCommand;
import com.syntaxphoenix.spigot.smoothtimber.command.commands.HelpCommand;
import com.syntaxphoenix.spigot.smoothtimber.command.commands.ToggleCommand;
import com.syntaxphoenix.spigot.smoothtimber.compatibility.CompatibilityHandler;
import com.syntaxphoenix.spigot.smoothtimber.config.ConfigTimer;
import com.syntaxphoenix.spigot.smoothtimber.config.Message;
import com.syntaxphoenix.spigot.smoothtimber.config.config.AddonConfig;
import com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig;
import com.syntaxphoenix.spigot.smoothtimber.config.config.MessageConfig;
import com.syntaxphoenix.spigot.smoothtimber.enchant.TimberEnchantListener;
import com.syntaxphoenix.spigot.smoothtimber.listener.AutoSaplingListener;
import com.syntaxphoenix.spigot.smoothtimber.listener.BlockBreakListener;
import com.syntaxphoenix.spigot.smoothtimber.listener.BlockFallListener;
import com.syntaxphoenix.spigot.smoothtimber.listener.BlockTrackingListener;
import com.syntaxphoenix.spigot.smoothtimber.listener.PluginLoadListener;
import com.syntaxphoenix.spigot.smoothtimber.platform.Platform;
import com.syntaxphoenix.spigot.smoothtimber.stats.SyntaxPhoenixStats;
import com.syntaxphoenix.spigot.smoothtimber.utilities.locate.PlacedBlockTracker;
import com.syntaxphoenix.spigot.smoothtimber.utilities.plugin.PluginSettings;
import com.syntaxphoenix.spigot.smoothtimber.version.manager.VersionChanger;
import com.syntaxphoenix.spigot.smoothtimber.version.manager.VersionExchanger;
import com.syntaxphoenix.syntaxapi.utils.java.Arrays;
import com.syntaxphoenix.syntaxapi.utils.java.Exceptions;

public class PluginUtils {

    public static final PluginSettings SETTINGS = new PluginSettings();

    public static PluginUtils UTILS;
    public static SmoothTimber MAIN;
    public static VersionChanger CHANGER;
    public static SyntaxPhoenixStats STATS;

    public static void setUp(final SmoothTimber main) {
        MAIN = main;
        UTILS = new PluginUtils();
    }

    public PluginUtils() {
        MessageConfig.INSTANCE.reload();
        CHANGER = VersionExchanger.getVersionChanger();
        if (!CHANGER.isValid()) {
            return;
        }
        PlacedBlockTracker.setup(MAIN);
        CompatibilityHandler.registerDefaults();
        CutterConfig.INSTANCE.reload();
        registerCommands();
        registerListener();
        registerTasks();
        checkPlugins();
        STATS = new SyntaxPhoenixStats("7vTfe4hf", MAIN);
    }

    /*
     * 
     */

    private void checkPlugins() {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        for (final Plugin plugin : pluginManager.getPlugins()) {
            if (plugin == null) {
                continue;
            }
            SETTINGS.updatePlugin(plugin, plugin.isEnabled());
        }
        AddonConfig.INSTANCE.reload();
        CompatibilityHandler.handleSettingsUpdate(SETTINGS);
    }

    private void registerListener() {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new BlockBreakListener(), MAIN);
        pluginManager.registerEvents(new BlockFallListener(), MAIN);
        pluginManager.registerEvents(new BlockTrackingListener(), MAIN);
        pluginManager.registerEvents(new PluginLoadListener(), MAIN);
        pluginManager.registerEvents(new TimberEnchantListener(), MAIN);
        pluginManager.registerEvents(new AutoSaplingListener(), MAIN);
    }

    private void registerCommands() {
        final CommandRedirect command = new CommandRedirect();

        try {
            final PluginCommand plugin = MAIN.getCommand("smoothtimber");
            plugin.setExecutor(command);
            plugin.setTabCompleter(command);
        } catch (final UnsupportedOperationException exception) {
            // Paper plugin (loaded via paper-plugin.yml): YAML command
            // declarations are not supported, so register the command
            // through Paper's Brigadier API instead. This must be done
            // reflectively: JavaPlugin#registerCommand exists in Paper's
            // API but not in Spigot's, and the project compiles against
            // the Spigot API, so a direct call fails the Maven build
            // (see PluginUtils.java:104). Spigot never reaches this
            // branch (and never loads PaperCommandAdapter).
            registerPaperCommand(command);
        }

        /*
         * 
         */

        COMMANDS.register(new HelpCommand(), "help", "?");
        COMMANDS.register(new ToggleCommand(), "toggle");
        COMMANDS.register(new EnchantCommand(), "enchant");

    }

    /*
     * Paper-only command registration, via reflection. Paper's
     * JavaPlugin#registerCommand(String, String, Collection, BasicCommand)
     * does not exist in the Spigot API the project compiles against, so
     * neither the method nor PaperCommandAdapter may be referenced
     * directly here — doing so breaks `mvn package` with
     * "cannot find symbol: method registerCommand(...)". The adapter
     * class is only loaded on this path, i.e. only on Paper, where both
     * the method and io.papermc.paper.command.brigadier.BasicCommand
     * exist at runtime.
     */

    private void registerPaperCommand(final CommandRedirect command) {
        try {
            final Class<?> adapterClass = Class.forName("com.syntaxphoenix.spigot.smoothtimber.command.PaperCommandAdapter");
            final Object adapter = adapterClass.getConstructor(CommandRedirect.class).newInstance(command);
            final Class<?> basicCommandClass = Class.forName("io.papermc.paper.command.brigadier.BasicCommand");
            MAIN.getClass()
                .getMethod("registerCommand", String.class, String.class, java.util.Collection.class, basicCommandClass)
                .invoke(MAIN, "smoothtimber", "SmoothTimber main command",
                    java.util.Arrays.asList("sm", "stimb"), adapter);
        } catch (final ReflectiveOperationException exception) {
            sendConsoleError("Failed to register the smoothtimber command via the Paper Brigadier API", exception);
        }
    }

    private void registerTasks() {
        Platform.getPlatform().asyncTaskTimer(ConfigTimer.TIMER, 20, 60);
    }

    /*
     * Console Util
     */

    public static void sendConsoleMessage(final boolean prefix, final String message) {
        Bukkit.getConsoleSender()
            .sendMessage(ChatColor.translateAlternateColorCodes('&', prefix ? Message.GLOBAL_PREFIX.message() + ' ' + message : message));
    }

    public static void sendConsoleMessage(final boolean prefix, final String... messages) {
        for (final String message : messages) {
            sendConsoleMessage(prefix, message);
        }
    }

    public static void sendConsoleMessageMultiline(final boolean prefix, final String message) {
        sendConsoleMessage(prefix, message.split("\n"));
    }

    public static void sendConsoleError(final String reason, final Throwable throwable) {
        PluginUtils.sendConsoleMessage(false, Arrays.merge(String[]::new, Arrays.merge(String[]::new,
            Centering.center("&8##################################################################", "&r", "&8<=> &5Smooth&dTimber &8<=>",
                "&7" + reason, "&r", "&8##################################################################"),
            Exceptions.stackTraceToStringArray(throwable)), "&8##################################################################"));
    }

}
