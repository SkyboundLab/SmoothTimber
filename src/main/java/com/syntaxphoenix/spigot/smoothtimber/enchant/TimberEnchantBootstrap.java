package com.syntaxphoenix.spigot.smoothtimber.enchant;

import java.util.Collections;

import org.bukkit.inventory.EquipmentSlotGroup;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.tag.TagEntry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Paper bootstrap for the Timber enchantment.
 *
 * <p>
 * Registered through Paper's registry API (Paper 1.21+), so the enchantment
 * is a real registry entry: it is sent to clients, shows up in the enchanting
 * table, works with enchanted books, anvils and {@code /enchant}, and survives
 * restarts. Declared via {@code paper-plugin.yml}; Spigot and Paper versions
 * without {@code paper-plugin.yml} support never load this class at all.
 *
 * <p>
 * Everything that touches the 1.21 registry API happens inside
 * {@link #bootstrap(BootstrapContext)} and is guarded: on a Paper version
 * without that API the registration simply fails and timber stays ungated
 * (see {@code TimberEnchantment#isRegistered()}), instead of breaking the
 * whole plugin.
 */
public final class TimberEnchantBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(final BootstrapContext context) {
        try {
            registerTimber(context);
        } catch (final Throwable throwable) {
            context.getLogger().warn("Could not register the Timber enchantment, timber will not require an enchantment: {}", throwable.toString());
        }
    }

    private void registerTimber(final BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose(), event -> {
            event.registry().register(
                EnchantmentKeys.create(Key.key("smoothtimber:timber")),
                builder -> builder
                    .description(Component.text("Timber"))
                    .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.AXES))
                    .primaryItems(event.getOrCreateTag(ItemTypeTagKeys.AXES))
                    .anvilCost(2)
                    .maxLevel(TimberEnchantment.MAX_LEVEL)
                    .weight(10)
                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(15, 0))
                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(65, 0))
                    .activeSlots(EquipmentSlotGroup.MAINHAND)
            );

            context.getLogger().info("Registered smoothtimber:timber in the enchantment registry");
        });

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ENCHANTMENT), event -> {
            event.registrar().addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, Collections.singletonList(TagEntry.valueEntry(EnchantmentKeys.create(Key.key("smoothtimber:timber")))));
        });
    }

}
