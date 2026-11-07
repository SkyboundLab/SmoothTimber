package com.syntaxphoenix.spigot.smoothtimber.command.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.spigot.smoothtimber.command.MinecraftInfo;
import com.syntaxphoenix.spigot.smoothtimber.command.SmoothCommand;
import com.syntaxphoenix.spigot.smoothtimber.config.Message;
import com.syntaxphoenix.spigot.smoothtimber.enchant.TimberEnchantment;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

public class EnchantCommand extends SmoothCommand {

    @Override
    public void execute(final MinecraftInfo info, final Arguments arguments) {

        final CommandSender sender = info.getSender();
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.GLOBAL_PREFIX.colored() + ' ' + Message.COMMAND_ONLY_PLAYER.colored());
            return;
        }

        if (!sender.hasPermission("smoothtimber.enchant") && !sender.hasPermission("smoothtimber.*")) {
            sender.sendMessage(Message.GLOBAL_PREFIX.colored() + ' ' + Message.COMMAND_MISSING222PERMISSION.colored(new String[] {
                "%permission%",
                "smoothtimber.enchant"
            }));
            return;
        }

        if (!TimberEnchantment.isRegistered()) {
            sender.sendMessage(Message.GLOBAL_PREFIX.colored() + ' ' + Message.ENCHANT_NOT222AVAILABLE.colored());
            return;
        }

        final Player player = (Player) sender;
        final ItemStack held = player.getInventory().getItemInMainHand();
        if (!TimberEnchantment.isAxe(held)) {
            sender.sendMessage(Message.GLOBAL_PREFIX.colored() + ' ' + Message.ENCHANT_NO222AXE.colored());
            return;
        }

        if (TimberEnchantment.hasTimber(held)) {
            sender.sendMessage(Message.GLOBAL_PREFIX.colored() + ' ' + Message.ENCHANT_ALREADY.colored());
            return;
        }

        if (!TimberEnchantment.applyTimber(held)) {
            sender.sendMessage(Message.GLOBAL_PREFIX.colored() + ' ' + Message.ENCHANT_FAILED.colored());
            return;
        }

        sender.sendMessage(Message.GLOBAL_PREFIX.colored() + ' ' + Message.ENCHANT_APPLIED.colored());
    }

    @Override
    public DefaultCompletion complete(final MinecraftInfo info, final Arguments arguments) {
        return new DefaultCompletion();
    }

}
