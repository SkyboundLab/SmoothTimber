package com.syntaxphoenix.spigot.smoothtimber.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.syntaxphoenix.spigot.smoothtimber.compatibility.CompatibilityHandler;
import com.syntaxphoenix.spigot.smoothtimber.compatibility.blockylog.BlockyLog;
import com.syntaxphoenix.spigot.smoothtimber.compatibility.coreprotect.CoreProtectAddon;
import com.syntaxphoenix.spigot.smoothtimber.compatibility.logblock.LogBlock;
import com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig;
import com.syntaxphoenix.spigot.smoothtimber.utilities.PluginUtils;
import com.syntaxphoenix.spigot.smoothtimber.utilities.locate.PlacedBlockTracker;
import com.syntaxphoenix.spigot.smoothtimber.version.manager.VersionChanger;

public class BlockPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        VersionChanger changer = PluginUtils.CHANGER;
        if (changer.isWoodBlock(event.getBlock().getState())) {
            // Skip marking if any enabled logging plugin is active
            boolean useExternalTracking = 
                (CutterConfig.USE_COREPROTECT_TRACKING && CompatibilityHandler.getAddon(CoreProtectAddon.class).isPresent()) ||
                (CutterConfig.USE_LOGBLOCK_TRACKING && CompatibilityHandler.getAddon(LogBlock.class).isPresent()) ||
                (CutterConfig.USE_BLOCKYLOG_TRACKING && CompatibilityHandler.getAddon(BlockyLog.class).isPresent());
            
            if (useExternalTracking) {
                return;
            }
            PlacedBlockTracker.markAsPlaced(event.getBlock());
        }
    }
}
