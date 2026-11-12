package com.syntaxphoenix.spigot.smoothtimber.utilities;

import static com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig.GLOBAL_DEBUG;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig;

/*
 * Auto Sapling "replant" mode: plants a sapling on every stump of a
 * felled tree, taken from the chopping player's inventory.
 *
 * - a 2x2 tree is replanted as a full square, so it can grow
 * - creative mode uses no saplings
 * - too few saplings means nothing is planted
 * - the sapling has to survive on the soil, checked by vanilla
 */
public final class TreeReplanter {

    private static final String STRIPPED = "STRIPPED_";
    private static final String LOG = "_LOG";
    private static final String WOOD = "_WOOD";
    private static final String SAPLING = "_SAPLING";

    private TreeReplanter() {}

    public static void replant(final Player player, final Map<Location, Material> felledLogs) {
        try {
            if (player == null || !player.isOnline()) {
                debug("Replant skipped: player is offline");
                return;
            }

            if (felledLogs == null || felledLogs.isEmpty()) {
                debug("Replant skipped: no logs were felled");
                return;
            }

            final Map<Location, Material> stumps = findStumps(felledLogs);
            if (stumps.isEmpty()) {
                debug("Replant skipped: no stump among " + felledLogs.size() + " felled logs");
                return;
            }

            debug("Replant: " + felledLogs.size() + " felled logs, " + stumps.size() + " stumps");

            final boolean creative = player.getGameMode() == GameMode.CREATIVE;
            if (!creative && !canPayFor(player, stumps)) {
                return;
            }

            for (final Map.Entry<Location, Material> stump : stumps.entrySet()) {
                plant(player, stump.getKey(), saplingForLog(stump.getValue()), creative);
            }
        } catch (final Exception exception) {
            if (GLOBAL_DEBUG) {
                PluginUtils.sendConsoleError("Something went wrong while replanting a felled tree", exception);
            }
        }
    }

    /*
     * All or nothing: too few saplings means nothing is planted.
     */

    private static boolean canPayFor(final Player player, final Map<Location, Material> stumps) {
        final Map<Material, Integer> needed = new LinkedHashMap<>();

        for (final Material log : stumps.values()) {
            final Material sapling = saplingForLog(log);
            if (sapling != null) {
                needed.merge(sapling, 1, Integer::sum);
            }
        }

        for (final Map.Entry<Material, Integer> entry : needed.entrySet()) {
            final int carried = count(player, entry.getKey());
            if (carried < entry.getValue()) {
                debug("Replant skipped: player carries " + carried + " " + entry.getKey() + " but " + entry.getValue() + " are needed for " + stumps.size() + " stumps");
                return false;
            }
        }

        return true;
    }

    private static int count(final Player player, final Material material) {
        int carried = 0;

        for (final ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                carried += stack.getAmount();
            }
        }

        return carried;
    }

    /*
     * Plants one sapling when the stump is free and the soil accepts it.
     */

    private static void plant(final Player player, final Location stump, final Material sapling, final boolean creative) {
        if (sapling == null) {
            return;
        }

        final Block target = stump.getBlock();
        if (!target.isEmpty()) {
            debug("Replant skipped at " + describe(stump) + ": block is " + target.getType() + ", not empty");
            return;
        }

        if (!target.canPlace(sapling.createBlockData())) {
            debug("Replant skipped at " + describe(stump) + ": vanilla refuses " + sapling + " on this soil");
            return;
        }

        if (!creative && !player.getInventory().removeItem(new ItemStack(sapling, 1)).isEmpty()) {
            return;
        }

        target.setType(sapling, true);

        debug("Replanted a " + sapling.name()
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ') + " at " + describe(stump));
    }

    /*
     * The lowest felled logs, plus the missing corners of a 2x2 tree.
     */

    private static Map<Location, Material> findStumps(final Map<Location, Material> felledLogs) {
        int lowestY = Integer.MAX_VALUE;
        for (final Location location : felledLogs.keySet()) {
            lowestY = Math.min(lowestY, location.getBlockY());
        }

        final Map<Location, Material> stumps = new LinkedHashMap<>();
        for (final Map.Entry<Location, Material> felled : felledLogs.entrySet()) {
            if (felled.getKey().getBlockY() == lowestY) {
                stumps.put(felled.getKey(), felled.getValue());
            }
        }

        if (stumps.size() >= 2 && stumps.size() < 4) {
            fillSquare(stumps, lowestY);
        }

        return stumps;
    }

    private static void fillSquare(final Map<Location, Material> stumps, final int y) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (final Location location : stumps.keySet()) {
            minX = Math.min(minX, location.getBlockX());
            maxX = Math.max(maxX, location.getBlockX());
            minZ = Math.min(minZ, location.getBlockZ());
            maxZ = Math.max(maxZ, location.getBlockZ());
        }
        if (maxX - minX > 1 || maxZ - minZ > 1) {
            return;
        }

        final Material log = stumps.values().iterator().next();
        final World world = stumps.keySet().iterator().next().getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (isStump(stumps, x, z)) {
                    continue;
                }

                final Location corner = new Location(world, x, y, z);
                if (corner.getBlock().isEmpty()) {
                    stumps.put(corner, log);
                }
            }
        }
    }

    private static boolean isStump(final Map<Location, Material> stumps, final int x, final int z) {
        for (final Location location : stumps.keySet()) {
            if (location.getBlockX() == x && location.getBlockZ() == z) {
                return true;
            }
        }

        return false;
    }

    /*
     * The sapling for a log, or null when the log has none.
     */

    private static Material saplingForLog(final Material log) {
        if (log == null) {
            return null;
        }

        String name = log.name();
        if (name.startsWith(STRIPPED)) {
            name = name.substring(STRIPPED.length());
        }

        final Material sapling = saplingOf(name);
        return sapling != null && CutterConfig.AUTO_SAPLING_MATERIALS.contains(sapling) ? sapling : null;
    }

    private static Material saplingOf(final String logName) {
        switch (logName) {
            case "MANGROVE_LOG":
            case "MANGROVE_WOOD":
                return Material.MANGROVE_PROPAGULE;
            case "CRIMSON_STEM":
            case "CRIMSON_HYPHAE":
                return Material.CRIMSON_FUNGUS;
            case "WARPED_STEM":
            case "WARPED_HYPHAE":
                return Material.WARPED_FUNGUS;
            default:
                break;
        }

        final String suffix = logName.endsWith(LOG) ? LOG : logName.endsWith(WOOD) ? WOOD : null;
        if (suffix == null) {
            return null;
        }

        return matchMaterial(logName.substring(0, logName.length() - suffix.length()) + SAPLING);
    }

    private static Material matchMaterial(final String name) {
        try {
            return Material.valueOf(name);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private static String describe(final Location location) {
        return location.getWorld().getName() + ' ' + location.getBlockX() + ' ' + location.getBlockY() + ' ' + location.getBlockZ();
    }

    private static void debug(final String message) {
        if (GLOBAL_DEBUG) {
            PluginUtils.sendConsoleMessage(false, message);
        }
    }

}
