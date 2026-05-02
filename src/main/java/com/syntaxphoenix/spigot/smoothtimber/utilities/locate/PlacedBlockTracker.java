package com.syntaxphoenix.spigot.smoothtimber.utilities.locate;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Tracks player-placed blocks using Minecraft's PersistentDataContainer (PDC).
 * This approach stores metadata directly on blocks, which automatically handles:
 * - Block removal (any method: player break, explosion, fire, WorldEdit, etc.)
 * - Server restarts (data persists in chunk files)
 * - No memory overhead or file I/O
 * - No cleanup needed
 */
public class PlacedBlockTracker {

    private static NamespacedKey PLAYER_PLACED_KEY;

    public static void setup(Plugin plugin) {
        PLAYER_PLACED_KEY = new NamespacedKey(plugin, "player_placed");
    }

    /**
     * Marks a block as player-placed.
     * @param block The block to mark
     */
    public static void markAsPlaced(Block block) {
        if (PLAYER_PLACED_KEY == null) {
            return;
        }
        block.getChunk().getPersistentDataContainer().set(
            getChunkKey(block.getLocation()), 
            PersistentDataType.BYTE, 
            (byte) 1
        );
    }
    
    /**
     * Gets a unique key for a block location within a chunk.
     */
    private static NamespacedKey getChunkKey(Location location) {
        String key = "player_placed_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        return new NamespacedKey(PLAYER_PLACED_KEY.getNamespace(), key);
    }

    /**
     * Marks a block at the given location as player-placed.
     * @param location The location of the block to mark
     */
    public static void markAsPlaced(Location location) {
        markAsPlaced(location.getBlock());
    }

    /**
     * Checks if a block was placed by a player.
     * @param block The block to check
     * @return true if the block was placed by a player, false otherwise
     */
    public static boolean isPlayerPlaced(Block block) {
        if (PLAYER_PLACED_KEY == null) {
            return false;
        }
        return block.getChunk().getPersistentDataContainer().has(
            getChunkKey(block.getLocation()), 
            PersistentDataType.BYTE
        );
    }

    /**
     * Checks if a block at the given location was placed by a player.
     * @param location The location of the block to check
     * @return true if the block was placed by a player, false otherwise
     */
    public static boolean isPlayerPlaced(Location location) {
        return isPlayerPlaced(location.getBlock());
    }

    /**
     * Removes the player-placed marker from a block.
     * Note: This is usually not needed as the PDC is automatically removed when blocks are destroyed.
     * @param block The block to unmark
     */
    public static void unmark(Block block) {
        if (PLAYER_PLACED_KEY == null) {
            return;
        }
        block.getChunk().getPersistentDataContainer().remove(getChunkKey(block.getLocation()));
    }

    /**
     * Removes the player-placed marker from a block at the given location.
     * Note: This is usually not needed as the PDC is automatically removed when blocks are destroyed.
     * @param location The location of the block to unmark
     */
    public static void unmark(Location location) {
        unmark(location.getBlock());
    }
}
