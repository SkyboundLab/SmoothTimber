package com.syntaxphoenix.spigot.smoothtimber.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import com.syntaxphoenix.spigot.smoothtimber.utilities.PluginUtils;
import com.syntaxphoenix.spigot.smoothtimber.utilities.locate.PlacedBlockTracker;

public final class BlockTrackingListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        // Saplings are intentionally not tracked. If one grows, the resulting tree
        // consists of new blocks and must remain choppable.
        if (PluginUtils.CHANGER.isWoodBlock(event.getBlock().getState())) {
            PlacedBlockTracker.markAsPlaced(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        PlacedBlockTracker.unmark(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        PlacedBlockTracker.unmark(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(final BlockFadeEvent event) {
        PlacedBlockTracker.unmark(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        PlacedBlockTracker.unmark(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        unmarkAll(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        unmarkAll(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(final StructureGrowEvent event) {
        // The sapling is gone and the grown blocks are new, natural blocks.
        PlacedBlockTracker.unmark(event.getLocation());
        event.getBlocks().forEach(state -> PlacedBlockTracker.unmark(state.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        move(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        move(event.getBlocks(), event.getDirection());
    }

    private void unmarkAll(final List<Block> blocks) {
        blocks.forEach(PlacedBlockTracker::unmark);
    }

    private void move(final List<Block> blocks, final BlockFace direction) {
        final Map<Block, Material> moved = new HashMap<>();
        for (final Block block : blocks) {
            if (PlacedBlockTracker.isPlayerPlaced(block)) {
                moved.put(block, block.getType());
            }
        }
        // Remove all old markers first so a block that is also the destination of
        // another moved block cannot remove the marker that was just written to it.
        moved.keySet().forEach(PlacedBlockTracker::unmark);
        moved.forEach((block, material) -> PlacedBlockTracker.markAsPlaced(block.getRelative(direction), material));
    }

}
