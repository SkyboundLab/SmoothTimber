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
            // Paper below 1.21 (or any other registry failure): the
            // enchantment is unavailable and timber stays ungated. The main
            // plugin logs a warning about this during enable.
            context.getLogger().warn("Could not register the Timber enchantment, timber will not require an enchantment: {}",
                throwable.toString());
        }
    }

    private void registerTimber(final BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose(), event -> {
            event.registry().register(
                EnchantmentKeys.create(Key.key("smoothtimber:timber")),
                builder -> builder
                    .description(Component.text("Timber"))
                    // Axes only, both for applying (anvil, /enchant, books)
                    // and for the enchanting table.
                    .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.AXES))
                    .primaryItems(event.getOrCreateTag(ItemTypeTagKeys.AXES))
                    .anvilCost(2)
                    .maxLevel(TimberEnchantment.MAX_LEVEL)
                    // Common, like Efficiency/Fortune (weight 10).
                    .weight(10)
                    // Costs like Fortune/Silk Touch: shows up in the
                    // mid to high slots of the enchanting table.
                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(15, 0))
                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(65, 0))
                    .activeSlots(EquipmentSlotGroup.MAINHAND));
            context.getLogger().info("Registered smoothtimber:timber in the enchantment registry");
        });

        // The enchanting table only offers enchantments listed in this tag,
        // on top of the primary-items check above.
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ENCHANTMENT), event -> {
            event.registrar().addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE,
                Collections.singletonList(TagEntry.valueEntry(EnchantmentKeys.create(Key.key("smoothtimber:timber")))));
        });
    }

}
