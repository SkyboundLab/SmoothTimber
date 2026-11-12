package com.syntaxphoenix.spigot.smoothtimber.command;

import java.util.Collection;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Paper plugins have no YAML commands, this forwards to {@link CommandRedirect}.
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
