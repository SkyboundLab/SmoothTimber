package com.syntaxphoenix.spigot.smoothtimber.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.syntaxphoenix.spigot.smoothtimber.utilities.PluginUtils;
import com.syntaxphoenix.spigot.smoothtimber.utilities.locate.PlacedBlockTracker;
import com.syntaxphoenix.spigot.smoothtimber.version.manager.VersionChanger;

public class BlockPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        VersionChanger changer = PluginUtils.CHANGER;
        if (changer.isWoodBlock(event.getBlock().getState())) {
            PlacedBlockTracker.markAsPlaced(event.getBlock());
        }
    }
}
