package com.syntaxphoenix.spigot.smoothtimber.enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Access to the Timber enchantment ({@code smoothtimber:timber}).
 *
 * <p>
 * Registered on Paper 1.21+ by {@link TimberEnchantBootstrap}, so on
 * Spigot and older Paper {@link #isRegistered()} is false and timber stays
 * ungated.
 */
public final class TimberEnchantment {

    public static final NamespacedKey KEY = new NamespacedKey("smoothtimber", "timber");

    public static final int MAX_LEVEL = 1;

    private TimberEnchantment() {
    }

    /**
     * @return the registered enchantment, or null when it is not available.
     */
    public static Enchantment get() {
        return Enchantment.getByKey(KEY);
    }

    public static boolean isRegistered() {
        return get() != null;
    }

    /**
     * @return true when the item is an axe.
     */
    public static boolean isAxe(final ItemStack item) {
        return item != null && item.getType().name().endsWith("_AXE");
    }

    /**
     * @return true when the item has Timber.
     */
    public static boolean hasTimber(final ItemStack item) {
        if (item == null) {
            return false;
        }

        final Enchantment timber = get();
        return timber != null && item.getEnchantmentLevel(timber) > 0;
    }

    /**
     * Applies Timber I to the item.
     *
     * @return true when it worked.
     */
    public static boolean applyTimber(final ItemStack item) {
        if (item == null) {
            return false;
        }

        final Enchantment timber = get();
        if (timber == null) {
            return false;
        }

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (!meta.addEnchant(timber, MAX_LEVEL, false)) {
            meta.addEnchant(timber, MAX_LEVEL, true);
        }

        item.setItemMeta(meta);

        return hasTimber(item);
    }

}
