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
 * Makes sure Timber shows up in the enchanting table offers.
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
                return;
            }
        }

        if (Math.random() >= CutterConfig.TIMBER_ENCHANTMENT_TABLE_CHANCE) {
            return;
        }

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
        if (timber.equals(event.getEnchantmentHint()) && !event.getEnchantsToAdd().containsKey(timber)) {
            event.getEnchantsToAdd().put(timber, event.getLevelHint());
        }
    }

}
