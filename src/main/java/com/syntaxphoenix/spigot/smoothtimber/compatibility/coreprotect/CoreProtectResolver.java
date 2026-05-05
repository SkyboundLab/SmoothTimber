package com.syntaxphoenix.spigot.smoothtimber.compatibility.coreprotect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig;
import com.syntaxphoenix.spigot.smoothtimber.utilities.PluginUtils;
import com.syntaxphoenix.spigot.smoothtimber.utilities.limit.IntCounter;
import com.syntaxphoenix.spigot.smoothtimber.utilities.locate.LocationResolver;
import com.syntaxphoenix.spigot.smoothtimber.utilities.locate.Locator;
import com.syntaxphoenix.spigot.smoothtimber.version.manager.VersionChanger;

public class CoreProtectResolver extends LocationResolver {

    private final ICoreCompat compat;
    private final ExecutorService service;

    protected CoreProtectResolver(final ExecutorService service, final ICoreCompat compat) {
        this.service = service;
        this.compat = compat;
    }

    @Override
    public List<Location> resolve(final Location start, final int radius, final List<Location> current, final IntCounter counter,
        final int limit, final boolean ignore) {
        if (limit >= 0 && counter.get() >= limit) {
            return Collections.emptyList();
        }
        final VersionChanger change = PluginUtils.CHANGER;
        final World world = start.getWorld();
        final int x = start.getBlockX();
        final int y = start.getBlockY();
        final int z = start.getBlockZ();

        final List<Location> output = Collections.synchronizedList(new ArrayList<>());
        final List<Location> found = Collections.synchronizedList(new ArrayList<>());
        final ArrayList<Future<?>> tasks = new ArrayList<>();
        for (int cx = x - radius; cx <= x + radius; cx++) {
            for (int cz = z - radius; cz <= z + radius; cz++) {
                final Location location = new Location(world, cx, y, cz);
                final BlockState block = Locator.getBlockState(location);
                if (change.isWoodBlock(block)) {
                    if (current.contains(location)) {
                        continue;
                    }
                    tasks.add(service.submit(() -> {
                        if (compat.isPlayerPlaced(block)) {
                            return;
                        }
                        output.add(location);
                        found.add(location);
                    }));
                }
            }
        }
        for (final Future<?> task : tasks) {
            try {
                task.get(CutterConfig.GLOBAL_SYNC_TIME, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
                // We just ignore it
            }
        }
        int size = output.size();
        for (int index = 0; index < size; index++) {
            final Location location = output.get(index);
            if (limit >= 0 && counter.get() >= limit) {
                found.remove(location);
                output.remove(location);
                size--;
                index--;
                continue;
            }
            counter.increment();
            current.add(location);
        }
        return new ArrayList<>(found);
    }

    @Override
    public boolean isPlayerPlaced(final Location location) {
        final BlockState block = Locator.getBlockState(location);
        if (block == null) {
            return false;
        }
        // Check CoreProtect if enabled in config, otherwise fall back to PDC tracking
        // PDC auto-removes on block break, so it's safe as a fallback
        if (com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig.USE_COREPROTECT_TRACKING) {
            if (compat.isPlayerPlaced(block)) {
                return true;
            }
        }
        return com.syntaxphoenix.spigot.smoothtimber.utilities.locate.PlacedBlockTracker.isPlayerPlaced(location);
    }

}
