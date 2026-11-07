package com.syntaxphoenix.spigot.smoothtimber.command;

import java.util.Collection;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Paper-loader adapter for {@link CommandRedirect}.
 *
 * <p>
 * Paper plugins (loaded via {@code paper-plugin.yml}) cannot use
 * {@code JavaPlugin#getCommand} — YAML command declarations are not
 * supported for them — so the command is registered through Paper's
 * Brigadier API ({@code JavaPlugin#registerCommand}) and this adapter
 * forwards execution and tab completion to the existing Bukkit
 * {@link CommandRedirect}. Only loaded on Paper; Spigot never touches
 * this class.
 */
public final class PaperCommandAdapter implements BasicCommand {

    private final CommandRedirect redirect;

    public PaperCommandAdapter(final CommandRedirect redirect) {
        this.redirect = redirect;
    }

    @Override
    public void execute(final CommandSourceStack stack, final String[] args) {
        redirect.onCommand(stack.getSender(), null, "smoothtimber", args);
    }

    @Override
    public Collection<String> suggest(final CommandSourceStack stack, final String[] args) {
        return redirect.onTabComplete(stack.getSender(), null, "smoothtimber", args);
    }

}
