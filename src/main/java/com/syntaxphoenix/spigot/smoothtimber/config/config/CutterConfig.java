package com.syntaxphoenix.spigot.smoothtimber.config.config;

import static org.bukkit.Bukkit.getLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;

import com.syntaxphoenix.spigot.smoothtimber.config.ActionType;
import com.syntaxphoenix.spigot.smoothtimber.config.Message;
import com.syntaxphoenix.spigot.smoothtimber.config.STConfig;
import com.syntaxphoenix.spigot.smoothtimber.config.migration.CutterMigration;
import com.syntaxphoenix.spigot.smoothtimber.enchant.TimberEnchantment;
import com.syntaxphoenix.spigot.smoothtimber.utilities.PluginUtils;
import com.syntaxphoenix.spigot.smoothtimber.utilities.cooldown.CooldownHelper;
import com.syntaxphoenix.spigot.smoothtimber.utilities.limit.Limiter;
import com.syntaxphoenix.spigot.smoothtimber.utilities.locate.Locator;

public final class CutterConfig extends STConfig {

    public static final CutterConfig INSTANCE = new CutterConfig();

    public static int CHECK_RADIUS = 2;
    public static int ROOT_DEPTH = 3;
    public static List<String> CUTTER_MATERIALS = new ArrayList<>();
    public static List<String> CUTTER_PERMISSIONS = new ArrayList<>();

    public static boolean ENABLE_WOOD_PERMISSIONS = false;
    public static boolean ENABLE_CUTTER_PERMISSIONS = false;

    public static ActionType SNEAK = ActionType.OFF_ACTION;
    public static ActionType TOGGLEABLE = ActionType.OFF_ACTION;
    public static boolean SYNC_BLOCK_DETECTION = false;

    public static boolean ENABLE_BLOCK_LIMIT = true;
    public static int DEFAULT_BLOCK_LIMIT = 1000;

    public static boolean ENABLE_COOLDOWN = false;
    public static long DEFAULT_COOLDOWN_TIME = 20000;

    public static boolean ENABLE_UNBREAKING = true;

    public static boolean PREVENT_TOOL_BREAK = true;

    public static boolean AUTO_SAPLING_ENABLED = true;
    public static String AUTO_SAPLING_MODE = "dropped";
    public static boolean AUTO_SAPLING_REPLANT = false;
    public static int AUTO_SAPLING_DELAY = 100;
    public static int AUTO_SAPLING_CHECK_INTERVAL = 20;
    public static List<Material> AUTO_SAPLING_MATERIALS = new ArrayList<>();

    public static boolean ENABLE_LUCK = false;
    public static double LUCK_MULTIPLIER = 1.0;

    public static boolean TIMBER_ENCHANTMENT_ENABLED = true;
    public static double TIMBER_ENCHANTMENT_TABLE_CHANCE = 0.5;

    public static boolean ENABLE_EXCLUSION = false;
    public static List<Material> EXCLUDED_MATERIALS = new ArrayList<>();

    public static boolean ENABLE_INCLUSION = false;
    public static List<Material> INCLUDED_MATERIALS = new ArrayList<>();

    public static boolean ENABLE_WORLD = false;
    public static boolean ENABLE_WORLD_BLACKLIST = false;
    public static List<String> WORLD_LIST;

    public static boolean GLOBAL_DEBUG = false;
    public static int GLOBAL_SYNC_TIME = 50;

    public static boolean ENABLE_ANIMATION = true;
    public static boolean INSTANT_COLLECT = false;

    private CutterConfig() {
        super(new File("plugins/SmoothTimber", "config.yml"), CutterMigration.class, 10);
    }

    /*
     * Type
     */

    @Override
    protected String getSingleType() {
        return Message.TYPE_SETTING_MAIN.message();
    }

    @Override
    protected String getMultipleType() {
        return Message.TYPE_SETTINGS_MAIN.message();
    }

    /*
     * Handle
     */

    @Override
    protected void onSetup() {
        PluginUtils.CHANGER.setupConfig();
    }

    @Override
    protected void onLoad() {

        GLOBAL_DEBUG = check("global.debug", GLOBAL_DEBUG);
        GLOBAL_SYNC_TIME = check("global.sync-time", GLOBAL_SYNC_TIME);

        CHECK_RADIUS = check("cutter.radius", CHECK_RADIUS);
        ROOT_DEPTH = check("cutter.depth", ROOT_DEPTH);
        CUTTER_MATERIALS = check("cutter.materials", CUTTER_MATERIALS);

        SNEAK = ActionType.check(this, "options.sneak", SNEAK);
        TOGGLEABLE = ActionType.check(this, "options.toggleable", TOGGLEABLE);
        SYNC_BLOCK_DETECTION = check("options.sync-detection", SYNC_BLOCK_DETECTION);

        ENABLE_ANIMATION = check("options.animation", ENABLE_ANIMATION);
        INSTANT_COLLECT = check("options.collect-instantly", INSTANT_COLLECT);

        ENABLE_WOOD_PERMISSIONS = check("options.permission.wood-type", ENABLE_WOOD_PERMISSIONS);
        ENABLE_CUTTER_PERMISSIONS = check("options.permission.cutter-type", ENABLE_CUTTER_PERMISSIONS);

        ENABLE_BLOCK_LIMIT = check("limit.enabled", ENABLE_BLOCK_LIMIT);
        DEFAULT_BLOCK_LIMIT = check("limit.default", DEFAULT_BLOCK_LIMIT);

        ENABLE_COOLDOWN = check("cooldown.enabled", ENABLE_COOLDOWN);
        DEFAULT_COOLDOWN_TIME = check("cooldown.time", DEFAULT_COOLDOWN_TIME);

        ENABLE_UNBREAKING = check("enchantments.unbreaking.enabled", ENABLE_UNBREAKING);

        PREVENT_TOOL_BREAK = check("options.prevent-tool-break", PREVENT_TOOL_BREAK);

        AUTO_SAPLING_ENABLED = check("options.auto-sapling.enabled", AUTO_SAPLING_ENABLED);
        AUTO_SAPLING_MODE = check("options.auto-sapling.mode", AUTO_SAPLING_MODE);
        AUTO_SAPLING_MODE = AUTO_SAPLING_MODE == null ? "dropped" : AUTO_SAPLING_MODE.trim().toLowerCase();
        if (!AUTO_SAPLING_MODE.equals("dropped") && !AUTO_SAPLING_MODE.equals("replant")) {
            getLogger().warning("Unknown auto-sapling mode '" + AUTO_SAPLING_MODE + "' (expected 'dropped' or 'replant'); using 'dropped'.");
            set("options.auto-sapling.mode", AUTO_SAPLING_MODE = "dropped");
        }

        AUTO_SAPLING_REPLANT = AUTO_SAPLING_MODE.equals("replant");
        AUTO_SAPLING_DELAY = check("options.auto-sapling.delay", AUTO_SAPLING_DELAY);
        AUTO_SAPLING_CHECK_INTERVAL = check("options.auto-sapling.check-interval", AUTO_SAPLING_CHECK_INTERVAL);
        if (!config.contains("options.auto-sapling.materials")) {
            AUTO_SAPLING_MATERIALS = defaultAutoSaplingMaterials();
        }

        AUTO_SAPLING_MATERIALS = check("options.auto-sapling.materials", AUTO_SAPLING_MATERIALS);
        if (AUTO_SAPLING_DELAY < 0) {
            set("options.auto-sapling.delay", AUTO_SAPLING_DELAY = 0);
        }
        if (AUTO_SAPLING_CHECK_INTERVAL <= 0) {
            set("options.auto-sapling.check-interval", AUTO_SAPLING_CHECK_INTERVAL = 20);
        }

        ENABLE_LUCK = check("enchantments.fortune.enabled", ENABLE_LUCK);
        LUCK_MULTIPLIER = check("enchantments.fortune.multiplier", LUCK_MULTIPLIER);

        TIMBER_ENCHANTMENT_ENABLED = check("enchantments.timber.enabled", TIMBER_ENCHANTMENT_ENABLED);
        TIMBER_ENCHANTMENT_TABLE_CHANCE = check("enchantments.timber.table-chance", TIMBER_ENCHANTMENT_TABLE_CHANCE);
        if (TIMBER_ENCHANTMENT_TABLE_CHANCE < 0.0) {
            set("enchantments.timber.table-chance", TIMBER_ENCHANTMENT_TABLE_CHANCE = 0.0);
        }
        if (TIMBER_ENCHANTMENT_TABLE_CHANCE > 1.0) {
            set("enchantments.timber.table-chance", TIMBER_ENCHANTMENT_TABLE_CHANCE = 1.0);
        }
        if (TIMBER_ENCHANTMENT_ENABLED && !TimberEnchantment.isRegistered()) {
            getLogger().warning("Timber enchantment gating is enabled, but the Timber enchantment is not registered on this server (it needs Paper 1.21+). Timber will work without the enchantment until then.");
        }

        ENABLE_EXCLUSION = check("exclusion.enabled", ENABLE_EXCLUSION);
        EXCLUDED_MATERIALS = check("exclusion.list", EXCLUDED_MATERIALS);

        ENABLE_INCLUSION = check("inclusion.enabled", ENABLE_INCLUSION);
        final List<Material> includedMaterials = check("inclusion.list", INCLUDED_MATERIALS);

        // Materials have to be solid
        INCLUDED_MATERIALS.clear();
        for (final Material includedMaterial : includedMaterials) {
            if (includedMaterial.isSolid()) {
                INCLUDED_MATERIALS.add(includedMaterial);
            } else {
                getLogger().info("Ignoring invalid material '" + includedMaterial + "' in the inclusion.list");
            }
        }

        ENABLE_WORLD = check("worlds.enabled", ENABLE_WORLD);
        ENABLE_WORLD_BLACKLIST = check("worlds.blacklist", ENABLE_WORLD_BLACKLIST);
        WORLD_LIST = check("worlds.list", worldStringList());

        //
        // Corrections

        if (CHECK_RADIUS <= 0) {
            set("cutter.radius", CHECK_RADIUS = Math.abs(CHECK_RADIUS));
        }
        if (ROOT_DEPTH < 0) {
            set("cutter.depth", ROOT_DEPTH = Math.abs(ROOT_DEPTH));
        }
        if (DEFAULT_COOLDOWN_TIME <= 0) {
            set("cooldown.enabled", ENABLE_COOLDOWN = false);
            set("cooldown.time", DEFAULT_COOLDOWN_TIME = 20000);
        }

        //
        // Applying

        Locator.setSyncBlockDetection(SYNC_BLOCK_DETECTION);
        Limiter.setEnabled(ENABLE_BLOCK_LIMIT);

        CooldownHelper.setEnabled(ENABLE_COOLDOWN);

    }

    @Override
    protected void onUnload() {

    }

    /*
     * The default saplings: all vanilla saplings plus azaleas,
     * propagules and nether fungi.
     */

    private static List<Material> defaultAutoSaplingMaterials() {
        final LinkedHashSet<Material> materials = new LinkedHashSet<>(Tag.SAPLINGS.getValues());
        materials.add(Material.AZALEA);
        materials.add(Material.FLOWERING_AZALEA);
        materials.add(Material.MANGROVE_PROPAGULE);
        materials.add(Material.CRIMSON_FUNGUS);
        materials.add(Material.WARPED_FUNGUS);
        return new ArrayList<>(materials);
    }

    @SuppressWarnings("unchecked")
    private List<Material> check(final String path, final List<Material> input) {
        if (config.contains(path)) {
            final List<String> list = (List<String>) get(path);
            final ArrayList<Material> output = new ArrayList<>();
            for (final String value : list) {
                try {
                    output.add(Material.valueOf(value.toUpperCase()));
                } catch (final IllegalArgumentException exp) {
                    getLogger().info("The material '" + value + "' doesn't exist! (" + path + ")");
                }
            }
            return output;
        }
        final ArrayList<String> write = new ArrayList<>();
        if (!input.isEmpty()) {
            for (final Material material : input) {
                write.add(material.name());
            }
        }
        config.set(path, write);
        return input;
    }

    private static List<String> worldStringList() {

        if (WORLD_LIST != null) {
            return WORLD_LIST;
        }

        WORLD_LIST = new ArrayList<>();
        for (final World world : Bukkit.getWorlds()) {
            WORLD_LIST.add(world.getName());
        }
        return WORLD_LIST;
    }

}
