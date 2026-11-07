package com.syntaxphoenix.spigot.smoothtimber.enchant;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.spigot.smoothtimber.config.config.CutterConfig;

/**
 * Makes sure Timber can actually be found in the enchanting table.
 *
 * <p>
 * The registered enchantment (see {@link TimberEnchantBootstrap}) is already
 * offered by the vanilla table through its primary items and the
 * {@code in_enchanting_table} tag. This listener is the safety net on top of
 * that: when an axe is enchanted and none of the three offers is Timber, one
 * offer slot is replaced with Timber I (at the slot's shown cost) with the
 * configured chance, so the enchantment is reliably obtainable without
 * grinding through the table.
 */
public final class TimberEnchantListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPrepareItemEnchant(final PrepareItemEnchantEvent event) {
        if (!CutterConfig.TIMBER_ENCHANTMENT_ENABLED || !TimberEnchantment.isRegistered()) {
            return;
        }
        final ItemStack item = event.getItem();
        if (!TimberEnchantment.isAxe(item)) {
            return;
        }
        final Enchantment timber = TimberEnchantment.get();
        final EnchantmentOffer[] offers = event.getOffers();
        for (final EnchantmentOffer offer : offers) {
            if (offer != null && timber.equals(offer.getEnchantment())) {
                // The vanilla table already offers Timber.
                return;
            }
        }
        if (Math.random() >= CutterConfig.TIMBER_ENCHANTMENT_TABLE_CHANCE) {
            return;
        }
        // Replace the most expensive non-empty offer slot, keeping its shown
        // level cost so the player pays exactly what the table displayed.
        int slot = -1;
        for (int index = offers.length - 1; index >= 0; index--) {
            if (offers[index] != null) {
                slot = index;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        offers[slot] = new EnchantmentOffer(timber, TimberEnchantment.MAX_LEVEL, event.getExpLevelCostsOffered()[slot]);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnchantItem(final EnchantItemEvent event) {
        if (!CutterConfig.TIMBER_ENCHANTMENT_ENABLED || !TimberEnchantment.isRegistered()) {
            return;
        }
        if (!TimberEnchantment.isAxe(event.getItem())) {
            return;
        }
        final Enchantment timber = TimberEnchantment.get();
        // Safety net: if the player picked a Timber offer, make sure the
        // enchantment actually lands on the item.
        if (timber.equals(event.getEnchantmentHint()) && !event.getEnchantsToAdd().containsKey(timber)) {
            event.getEnchantsToAdd().put(timber, event.getLevelHint());
        }
    }

}
