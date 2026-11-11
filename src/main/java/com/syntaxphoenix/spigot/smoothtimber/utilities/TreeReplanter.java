package com.syntaxphoenix.spigot.smoothtimber.utilities;

import static com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig.GLOBAL_DEBUG;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig;

/*
 * Auto Sapling, "replant" mode: the TreeChop-style behaviour. Once a tree
 * has been felled, a sapling is planted back on each stump — the lowest
 * felled log of the tree — taken from the chopping player's inventory.
 *
 * This mirrors TreeChopMod's autoReplant (ChunkedTreeBreaker):
 * - the sapling type is derived from the felled log's name ("_log" or
 *   "_wood" becomes "_sapling"; mangrove logs become propagules; crimson
 *   and warped stems become fungi),
 * - a 2x2 tree (dark oak, spruce, jungle) replants all four stumps, and
 *   a partly-felled one is completed to the full 2x2 square when the
 *   missing corners are free,
 * - in creative mode no sapling is consumed,
 * - when the player does not carry enough saplings for every stump,
 *   nothing is planted at all, so a 2x2 tree is never left half-planted
 *   (and therefore unable to grow).
 *
 * Two things differ from TreeChop on purpose. The soil check is the same
 * vanilla canSurvive check the "dropped" mode uses (Block#canPlace), not
 * TreeChop's "below is not air, a log or leaves", so a sapling is never
 * planted on stone, sand or any other soil vanilla would reject. And a
 * sapling is only planted when its type is in
 * options.auto-sapling.materials, so the one material list governs both
 * modes.
 *
 * Called from the felling loop's region thread, after the logs are broken,
 * with the logs that were actually felled (location -> log material).
 * A partial fell — the axe broke, or the tool-break guard stopped it —
 * still replants any stump that was felled.
 */
public final class TreeReplanter {

    private TreeReplanter() {

    }

    public static void replant(final Player player, final Map<Location, Material> felledLogs) {
        try {
            if (player == null || !player.isOnline()) {
                if (GLOBAL_DEBUG) {
                    PluginUtils.sendConsoleMessage(false, "Replant skipped: player is offline");
                }
                return;
            }
            if (felledLogs == null || felledLogs.isEmpty()) {
                if (GLOBAL_DEBUG) {
                    PluginUtils.sendConsoleMessage(false, "Replant skipped: no logs were felled");
                }
                return;
            }
            final Map<Location, Material> stumps = findStumps(felledLogs);
            if (stumps.isEmpty()) {
                if (GLOBAL_DEBUG) {
                    PluginUtils.sendConsoleMessage(false, "Replant skipped: no stump among " + felledLogs.size() + " felled logs");
                }
                return;
            }
            if (GLOBAL_DEBUG) {
                PluginUtils.sendConsoleMessage(false, "Replant: " + felledLogs.size() + " felled logs, " + stumps.size() + " stumps");
            }
            final boolean creative = player.getGameMode() == GameMode.CREATIVE;
            if (!creative) {
                // All-or-nothing per sapling type, as in TreeChop: count
                // what the player carries of each type first, so a 2x2
                // tree is never left with only some of its stumps planted.
                for (final Map.Entry<Location, Material> stump : stumps.entrySet()) {
                    final Material sapling = saplingForLog(stump.getValue());
                    if (sapling == null) {
                        continue;
                    }
                    int carried = 0;
                    for (final ItemStack stack : player.getInventory().getContents()) {
                        if (stack != null && stack.getType() == sapling) {
                            carried += stack.getAmount();
                        }
                    }
                    int wanted = 0;
                    for (final Material otherLog : stumps.values()) {
                        if (saplingForLog(otherLog) == sapling) {
                            wanted++;
                        }
                    }
                    if (carried < wanted) {
                        if (GLOBAL_DEBUG) {
                            PluginUtils.sendConsoleMessage(false, "Replant skipped: player carries " + carried + " " + sapling
                                + " but " + wanted + " are needed for " + stumps.size() + " stumps");
                        }
                        return;
                    }
                }
            }
            for (final Map.Entry<Location, Material> stump : stumps.entrySet()) {
                final Material sapling = saplingForLog(stump.getValue());
                if (sapling == null) {
                    continue;
                }
                final Block target = stump.getKey().getBlock();
                if (!target.isEmpty()) {
                    // Still a log (instant-collect leaves the block in
                    // place) or something else in the way; leave it.
                    if (GLOBAL_DEBUG) {
                        PluginUtils.sendConsoleMessage(false, "Replant skipped at " + target.getX() + ' ' + target.getY() + ' '
                            + target.getZ() + ": block is " + target.getType() + ", not empty");
                    }
                    continue;
                }
                final BlockData saplingData = Bukkit.createBlockData(sapling);
                if (!target.canPlace(saplingData)) {
                    // Vanilla says this sapling cannot survive on the soil
                    // underneath; leave the stump bare.
                    if (GLOBAL_DEBUG) {
                        PluginUtils.sendConsoleMessage(false, "Replant skipped at " + target.getX() + ' ' + target.getY() + ' '
                            + target.getZ() + ": vanilla refuses " + sapling + " on this soil");
                    }
                    continue;
                }
                if (!creative) {
                    final Map<Integer, ItemStack> leftover = player.getInventory()
                        .removeItem(new ItemStack(sapling, 1));
                    if (!leftover.isEmpty()) {
                        // The sapling counted a moment ago is gone (another
                        // plugin, or the player, took it); skip this stump.
                        continue;
                    }
                }
                target.setType(sapling, true);
                if (GLOBAL_DEBUG) {
                    PluginUtils.sendConsoleMessage(false, "Replanted a " + sapling.name().toLowerCase().replace('_', ' ') + " at "
                        + target.getWorld().getName() + ' ' + target.getX() + ' ' + target.getY() + ' ' + target.getZ());
                }
            }
        } catch (final Exception exception) {
            if (GLOBAL_DEBUG) {
                PluginUtils.sendConsoleError("Something went wrong while replanting a felled tree", exception);
            }
        }
    }

    /*
     * The stumps: every felled log at the lowest Y of the fell, as
     * position -> log material. When two or three of them form part of a
     * 2x2 square, the missing corners are added (TreeChop's
     * findBasePositions) with the log type of a felled stump in the same
     * square, so a 2x2 tree always replants as a full square that can
     * grow. A corner is only added when its block is empty; the soil
     * check happens per stump in replant().
     */

    private static Map<Location, Material> findStumps(final Map<Location, Material> felledLogs) {
        int minY = Integer.MAX_VALUE;
        for (final Location location : felledLogs.keySet()) {
            if (location.getBlockY() < minY) {
                minY = location.getBlockY();
            }
        }
        final Map<Location, Material> stumps = new LinkedHashMap<>();
        for (final Map.Entry<Location, Material> felled : felledLogs.entrySet()) {
            if (felled.getKey().getBlockY() == minY) {
                stumps.put(felled.getKey(), felled.getValue());
            }
        }
        if (stumps.size() >= 2 && stumps.size() < 4) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (final Location location : stumps.keySet()) {
                if (location.getBlockX() < minX) {
                    minX = location.getBlockX();
                }
                if (location.getBlockX() > maxX) {
                    maxX = location.getBlockX();
                }
                if (location.getBlockZ() < minZ) {
                    minZ = location.getBlockZ();
                }
                if (location.getBlockZ() > maxZ) {
                    maxZ = location.getBlockZ();
                }
            }
            if (maxX - minX <= 1 && maxZ - minZ <= 1) {
                final List<Location> corners = new ArrayList<>();
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        // Compare by block coordinates, not Location#equals
                        // (a felled location can carry yaw/pitch).
                        boolean felled = false;
                        for (final Location stump : stumps.keySet()) {
                            if (stump.getBlockX() == x && stump.getBlockZ() == z) {
                                felled = true;
                                break;
                            }
                        }
                        if (felled) {
                            continue;
                        }
                        final Location corner = new Location(stumps.keySet().iterator().next().getWorld(), x, minY, z);
                        if (!corner.getBlock().isEmpty()) {
                            continue;
                        }
                        corners.add(corner);
                    }
                }
                if (!corners.isEmpty()) {
                    // Every corner replants the same sapling as the rest
                    // of the square; take the type of any felled stump.
                    final Material squareLog = stumps.values().iterator().next();
                    for (final Location corner : corners) {
                        stumps.put(corner, squareLog);
                    }
                }
            }
        }
        return stumps;
    }

    /*
     * The sapling that grows from a log, by name — the same derivation
     * TreeChop uses. Returns null when the log has no sapling (bamboo,
     * mangrove roots, included "other" blocks) or when the sapling is not
     * in the auto-sapling material list.
     */

    private static Material saplingForLog(final Material log) {
        if (log == null) {
            return null;
        }
        String name = log.name();
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
        }
        final Material sapling;
        if (name.equals("MANGROVE_LOG") || name.equals("MANGROVE_WOOD")) {
            sapling = Material.MANGROVE_PROPAGULE;
        } else if (name.equals("CRIMSON_STEM") || name.equals("CRIMSON_HYPHAE")) {
            sapling = Material.CRIMSON_FUNGUS;
        } else if (name.equals("WARPED_STEM") || name.equals("WARPED_HYPHAE")) {
            sapling = Material.WARPED_FUNGUS;
        } else if (name.endsWith("_LOG")) {
            sapling = matchMaterial(name.substring(0, name.length() - "_LOG".length()) + "_SAPLING");
        } else if (name.endsWith("_WOOD")) {
            sapling = matchMaterial(name.substring(0, name.length() - "_WOOD".length()) + "_SAPLING");
        } else {
            return null;
        }
        if (sapling == null || !CutterConfig.AUTO_SAPLING_MATERIALS.contains(sapling)) {
            return null;
        }
        return sapling;
    }

    private static Material matchMaterial(final String name) {
        try {
            return Material.valueOf(name);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

}
