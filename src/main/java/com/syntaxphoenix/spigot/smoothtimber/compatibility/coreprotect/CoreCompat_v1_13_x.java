package com.syntaxphoenix.spigot.smoothtimber.compatibility.coreprotect;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.plugin.Plugin;

import net.coreprotect.v2.CoreProtect;
import net.coreprotect.v2.CoreProtectAPI;

public class CoreCompat_v1_13_x implements ICoreCompat {

    private final CoreProtectAPI api;

    protected CoreCompat_v1_13_x(final Plugin plugin) {
        api = ((CoreProtect) plugin).getAPI();
    }

    @Override
    public void logRemoval(final String user, final Location location, final BlockState block) {
        api.logRemoval(user, location, block.getType(), block.getBlockData());
    }
    
    @Override
    public boolean isPlayerPlaced(BlockState state) {
        final List<String[]> list = api.blockLookup(state.getBlock(), 0);
        if (list == null || list.isEmpty()) {
            return false;
        }
        final String[] array = list.get(0);
        final CoreProtectAPI.ParseResult parseResult = api.parseResult(array);

        if (parseResult.getPlayer().isEmpty() || parseResult.getPlayer().startsWith("#") || parseResult.isRolledBack()) {
            return false;
        }
        
        // Check if it's a placement action (actionId != 0)
        if (parseResult.getActionId() == 0) {
            return false;
        }
        
        // Verify the placed block type matches the current block type
        // This prevents false positives when saplings grow into trees
        return parseResult.getType().equals(state.getType());
    }

}
