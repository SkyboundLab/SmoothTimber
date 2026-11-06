package com.syntaxphoenix.spigot.smoothtimber.utilities.locate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Tracks player-placed blocks in the persistent data container of their chunk.
 * <p>
 * Every marker also stores the material that was placed. If the block at that
 * location no longer has the same material (it was broken, replaced by the
 * environment or a sapling grew into a tree), the marker is stale and is
 * removed automatically.
 */
public final class PlacedBlockTracker {

    private static final String KEY_PREFIX = "player_placed_";

    private static NamespacedKey key;

    private PlacedBlockTracker() {}

    public static void setup(final Plugin plugin) {
        key = new NamespacedKey(plugin, "player_placed");
    }

    /**
     * Marks a block as player-placed.
     *
     * @param block the block that was placed
     */
    public static void markAsPlaced(final Block block) {
        if (block != null) {
            markAsPlaced(block, block.getType());
        }
    }

    /**
     * Marks a block as player-placed with an explicit material.
     * <p>
     * This is used when a piston moves a block: the destination does not contain
     * the moved material yet when the event is fired.
     *
     * @param block    the block to mark
     * @param material the material that will be at the block's location
     */
    public static void markAsPlaced(final Block block, final Material material) {
        if (key == null || block == null || material == null) {
            return;
        }
        container(block).set(blockKey(block), PersistentDataType.STRING, material.name());
    }

    /**
     * Checks if a block was placed by a player and still is the same block.
     *
     * @param block the block to check
     *
     * @return true if the block is still the player-placed block
     */
    public static boolean isPlayerPlaced(final Block block) {
        if (key == null || block == null) {
            return false;
        }
        final PersistentDataContainer container = container(block);
        final NamespacedKey blockKey = blockKey(block);
        final String material = container.get(blockKey, PersistentDataType.STRING);
        if (material == null) {
            // Remove markers written by the old tracker, which did not store a material.
            if (container.has(blockKey, PersistentDataType.BYTE)) {
                container.remove(blockKey);
            }
            return false;
        }
        if (!material.equals(block.getType().name())) {
            // The block was removed or replaced, so the marker is no longer valid.
            container.remove(blockKey);
            return false;
        }
        return true;
    }

    public static boolean isPlayerPlaced(final Location location) {
        return location != null && isPlayerPlaced(location.getBlock());
    }

    /**
     * Removes the player-placed marker from a block.
     *
     * @param block the block to unmark
     */
    public static void unmark(final Block block) {
        if (key == null || block == null) {
            return;
        }
        container(block).remove(blockKey(block));
    }

    public static void unmark(final Location location) {
        if (location != null) {
            unmark(location.getBlock());
        }
    }

    private static PersistentDataContainer container(final Block block) {
        return block.getChunk().getPersistentDataContainer();
    }

    private static NamespacedKey blockKey(final Block block) {
        return new NamespacedKey(key.getNamespace(), KEY_PREFIX + block.getX() + '_' + block.getY() + '_' + block.getZ());
    }

}
