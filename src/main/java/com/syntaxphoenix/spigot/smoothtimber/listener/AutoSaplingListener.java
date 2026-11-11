package com.syntaxphoenix.spigot.smoothtimber.listener;

import static com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig.GLOBAL_DEBUG;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig;
import com.syntaxphoenix.spigot.smoothtimber.platform.Platform;
import com.syntaxphoenix.spigot.smoothtimber.utilities.PluginUtils;

/*
 * Auto Sapling: a dropped sapling that comes to rest on soil plants itself.
 *
 * There is no Bukkit event for an item landing, so a sapling item is tracked
 * from the moment it spawns and re-checked on the region thread that owns
 * it until it plants, is picked up, merges away or despawns.
 *
 * Whether a sapling may grow on the block below it is NOT decided here from
 * a hardcoded list: Block#canPlace(BlockData) runs vanilla's own
 * canSurvive check for that exact sapling at that exact spot. That is what
 * makes this work on more than just grass: a plain sapling plants on grass,
 * dirt, coarse dirt, podzol, mycelium, rooted dirt, mud, muddy mangrove
 * roots and farmland; azaleas additionally accept clay and moss; mangrove
 * propagules accept clay and muddy mangrove roots; crimson and warped fungi
 * accept crimson/warped nylium and soul soil (as well as grass, dirt,
 * coarse dirt, podzol and farmland). New soils added by vanilla in a later
 * version work with no change here, because the server answers the
 * question instead of this plugin guessing it.
 *
 * One subtlety: the space a sapling goes into is not always the block the
 * dropped item is sitting in. On a short block — farmland (15/16 tall),
 * mud (14/16), soul sand (7/8), a slab — the item's feet sit inside the
 * soil block's own cell, so the block at the item's position IS the soil
 * and the sapling belongs one block higher. The check below works that
 * out from the block at the item's feet instead of assuming it is air.
 */
public class AutoSaplingListener implements Listener {

    /*
     * Vanilla despawns a dropped item after 6000 ticks (5 minutes); stop
     * re-checking just before that so a sapling that can never plant does
     * not keep scheduling work until the server removes it.
     */

    private static final int DESPAWN_CUTOFF_TICKS = 5800;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(final ItemSpawnEvent event) {
        if (!CutterConfig.AUTO_SAPLING_ENABLED) {
            return;
        }
        final Item item = event.getEntity();
        if (!isPlantable(item.getItemStack().getType())) {
            return;
        }
        if (!isWorldEnabled(item.getWorld())) {
            return;
        }
        scheduleCheck(item, CutterConfig.AUTO_SAPLING_DELAY);
    }

    /*
     *
     */

    private void scheduleCheck(final Item item, final long delay) {
        Platform.getPlatform().regionalSyncTaskLater(item.getLocation(), () -> check(item), Math.max(0, delay));
    }

    private void check(final Item item) {
        try {
            if (!item.isValid()) {
                return;
            }
            if (!CutterConfig.AUTO_SAPLING_ENABLED) {
                return;
            }
            final ItemStack stack = item.getItemStack();
            if (stack == null || stack.getAmount() <= 0) {
                return;
            }
            final Material type = stack.getType();
            if (!isPlantable(type)) {
                return;
            }
            if (!isWorldEnabled(item.getWorld())) {
                return;
            }
            if (item.getTicksLived() > DESPAWN_CUTOFF_TICKS) {
                return;
            }
            if (!item.isOnGround()) {
                scheduleCheck(item, CutterConfig.AUTO_SAPLING_CHECK_INTERVAL);
                return;
            }
            final Block feet = item.getLocation().getBlock();
            final Block target;
            if (feet.isEmpty() || feet.isLiquid()) {
                // The usual case: the item sits in the air above its soil.
                target = feet;
            } else {
                // The item's feet are inside a short block (farmland, mud,
                // soul sand, a slab, ...): that block is the soil, and the
                // sapling belongs in the space above it.
                target = feet.getRelative(BlockFace.UP);
            }
            if (!target.isEmpty()) {
                // Something solid (or a liquid) is where the sapling would
                // go; keep watching in case it moves or the block changes.
                scheduleCheck(item, CutterConfig.AUTO_SAPLING_CHECK_INTERVAL);
                return;
            }
            final BlockData sapling = Bukkit.createBlockData(type);
            if (!target.canPlace(sapling)) {
                // Vanilla says this sapling cannot survive here (wrong
                // soil underneath, ...); keep watching in case the item
                // is knocked onto soil before it despawns.
                scheduleCheck(item, CutterConfig.AUTO_SAPLING_CHECK_INTERVAL);
                return;
            }
            target.setType(type, true);
            if (stack.getAmount() == 1) {
                item.remove();
            } else {
                // One sapling per dropped stack plants itself; the rest
                // stays a normal dropped item for the player to pick up.
                // (The remainder now sits inside the new sapling, so this
                // item is no longer tracked.)
                stack.setAmount(stack.getAmount() - 1);
                item.setItemStack(stack);
            }
            if (GLOBAL_DEBUG) {
                PluginUtils.sendConsoleMessage(false, "Auto-planted a " + type.name().toLowerCase().replace('_', ' ') + " at "
                    + target.getWorld().getName() + ' ' + target.getX() + ' ' + target.getY() + ' ' + target.getZ());
            }
        } catch (final Exception exception) {
            if (GLOBAL_DEBUG) {
                PluginUtils.sendConsoleError("Something went wrong while checking a dropped sapling for auto-planting", exception);
            }
        }
    }

    /*
     *
     */

    private boolean isPlantable(final Material type) {
        return CutterConfig.AUTO_SAPLING_MATERIALS.contains(type);
    }

    /*
     * Same world gating as tree cutting (BlockBreakListener): when the
     * worlds list is enabled, a world is off limits if it is blacklisted
     * and listed, or whitelisted and not listed.
     */

    private boolean isWorldEnabled(final World world) {
        if (!CutterConfig.ENABLE_WORLD) {
            return true;
        }
        final boolean contains = CutterConfig.WORLD_LIST.contains(world.getName());
        return CutterConfig.ENABLE_WORLD_BLACKLIST != contains;
    }

}
