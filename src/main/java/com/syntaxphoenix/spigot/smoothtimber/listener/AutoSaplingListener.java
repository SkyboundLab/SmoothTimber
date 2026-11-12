package com.syntaxphoenix.spigot.smoothtimber.listener;

import static com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig.GLOBAL_DEBUG;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
 * Auto Sapling "dropped" mode: a dropped sapling plants itself when it
 * rests on soil it can grow on.
 *
 * "replant" mode is handled by TreeReplanter instead.
 */
public final class AutoSaplingListener implements Listener {

    /*
     * Items despawn after 6000 ticks, so stop checking a bit earlier.
     */

    private static final int DESPAWN_CUTOFF_TICKS = 5800;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(final ItemSpawnEvent event) {
        final Item item = event.getEntity();
        if (isDroppedSapling(item)) {
            checkLater(item, CutterConfig.AUTO_SAPLING_DELAY);
        }
    }

    /*
     * Plants the sapling, or tries again later.
     */

    private void check(final Item item) {
        try {
            if (!isDroppedSapling(item) || item.getTicksLived() > DESPAWN_CUTOFF_TICKS) {
                return;
            }

            if (item.isOnGround() && plant(item)) {
                return;
            }

            checkLater(item, CutterConfig.AUTO_SAPLING_CHECK_INTERVAL);
        } catch (final Exception exception) {
            if (GLOBAL_DEBUG) {
                PluginUtils.sendConsoleError("Something went wrong while checking a dropped sapling for auto-planting", exception);
            }
        }
    }

    private void checkLater(final Item item, final long delay) {
        Platform.getPlatform().regionalSyncTaskLater(item.getLocation(), () -> check(item), Math.max(0, delay));
    }

    /*
     * Plants one sapling from the stack, false when the block refuses it.
     */

    private boolean plant(final Item item) {
        final ItemStack stack = item.getItemStack();
        final Material type = stack.getType();
        final Block target = targetUnder(item);

        if (!target.isEmpty() || !target.canPlace(type.createBlockData())) {
            return false;
        }

        target.setType(type, true);

        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
            item.setItemStack(stack);
        } else {
            item.remove();
        }

        debug("Auto-planted a " + type.name()
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ') + " at " + describe(target));

        return true;
    }

    /*
     * The item's own block while it floats, otherwise the block above it.
     */

    private static Block targetUnder(final Item item) {
        final Block feet = item.getLocation().getBlock();
        return feet.isEmpty() || feet.isLiquid() ? feet : feet.getRelative(BlockFace.UP);
    }

    /*
     * Only in dropped mode, for saplings we may plant, in allowed worlds.
     */

    private static boolean isDroppedSapling(final Item item) {
        return CutterConfig.AUTO_SAPLING_ENABLED && !CutterConfig.AUTO_SAPLING_REPLANT && item.isValid() && CutterConfig.AUTO_SAPLING_MATERIALS.contains(item.getItemStack().getType()) && isWorldEnabled(item.getWorld());
    }

    /*
     * Same world check as tree cutting.
     */

    private static boolean isWorldEnabled(final World world) {
        if (!CutterConfig.ENABLE_WORLD) {
            return true;
        }

        final boolean contains = CutterConfig.WORLD_LIST.contains(world.getName());
        return CutterConfig.ENABLE_WORLD_BLACKLIST != contains;
    }

    private static String describe(final Block block) {
        return block.getWorld().getName() + ' ' + block.getX() + ' ' + block.getY() + ' ' + block.getZ();
    }

    private static void debug(final String message) {
        if (GLOBAL_DEBUG) {
            PluginUtils.sendConsoleMessage(false, message);
        }
    }

}
