package com.syntaxphoenix.spigot.smoothtimber.enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Access to the Timber enchantment ({@code smoothtimber:timber}).
 *
 * <p>
 * The enchantment itself is registered on Paper 1.21+ by
 * {@link TimberEnchantBootstrap} through the registry API, so it behaves like a
 * vanilla enchantment: it shows up in the enchanting table, on enchanted
 * books, in anvils and in the {@code /enchant} command. This class only uses
 * plain Bukkit API on purpose, so it is safe to load on Spigot and on older
 * Paper versions where the enchantment was never registered - in that case
 * {@link #isRegistered()} is false and timber simply stays ungated (see
 * {@code CutterConfig}).
 */
public final class TimberEnchantment {

    public static final NamespacedKey KEY = new NamespacedKey("smoothtimber", "timber");

    public static final int MAX_LEVEL = 1;

    private TimberEnchantment() {
    }

    /**
     * @return the registered Timber enchantment, or null when it was not
     *         registered (Spigot or Paper below 1.21).
     */
    public static Enchantment get() {
        return Enchantment.getByKey(KEY);
    }

    public static boolean isRegistered() {
        return get() != null;
    }

    /**
     * @return true when the given item is an axe of any material.
     */
    public static boolean isAxe(final ItemStack item) {
        return item != null && item.getType().name().endsWith("_AXE");
    }

    /**
     * @return true when the given item carries the Timber enchantment.
     */
    public static boolean hasTimber(final ItemStack item) {
        if (item == null) {
            return false;
        }
        final Enchantment timber = get();
        return timber != null && item.getEnchantmentLevel(timber) > 0;
    }

    /**
     * Applies Timber I to the given item.
     *
     * @return true when the enchantment was applied.
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
        // The item is an axe and therefore supported, but stay lenient if the
        // safety check disagrees for any reason.
        if (!meta.addEnchant(timber, MAX_LEVEL, false)) {
            meta.addEnchant(timber, MAX_LEVEL, true);
        }
        item.setItemMeta(meta);
        return hasTimber(item);
    }

}
