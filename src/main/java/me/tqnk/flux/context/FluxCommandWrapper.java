package me.tqnk.flux.context;

import com.google.common.base.Joiner;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_16_R1.command.VanillaCommandWrapper;

import me.tqnk.flux.annotation.FluxHandle;
import net.minecraft.server.v1_16_R1.CommandDispatcher;
import net.minecraft.server.v1_16_R1.CommandListenerWrapper;

public final class FluxCommandWrapper extends BukkitCommand {

    private final FluxHandle handle;
    private final CommandDispatcher dispatcher;
    public final CommandNode<CommandListenerWrapper> fluxCmd;

    public FluxCommandWrapper(FluxHandle handle, CommandDispatcher dispatcher, CommandNode<CommandListenerWrapper> fluxCmd) {
        super(fluxCmd.getName(), handle.description().isEmpty() ? "A bukkit command." : handle.description(), fluxCmd.getUsageText(), Collections.EMPTY_LIST);
        this.handle = handle;
        this.dispatcher = dispatcher;
        this.fluxCmd = fluxCmd;
        this.setPermission(getPermission(this.handle, fluxCmd));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;

        CommandListenerWrapper icommandlistener = getListener(sender);
        dispatcher.a(icommandlistener, toDispatcher(args, getName()), toDispatcher(args, commandLabel));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        CommandListenerWrapper icommandlistener = getListener(sender);
        ParseResults<CommandListenerWrapper> parsed = dispatcher.a().parse(toDispatcher(args, getName()), icommandlistener);

        List<String> results = new ArrayList<>();
        dispatcher.a().getCompletionSuggestions(parsed).thenAccept((suggestions) -> {
            suggestions.getList().forEach((s) -> results.add(s.getText()));
        });

        return results;
    }

    public static CommandListenerWrapper getListener(CommandSender sender) {
        return VanillaCommandWrapper.getListener(sender);
    }

    public static String getPermission(FluxHandle handle, CommandNode<CommandListenerWrapper> fluxCmd) {
        String perm = handle.permission().isEmpty() ? null : handle.permission();
        return perm;
    }

    private String toDispatcher(String[] args, String name) {
        return name + ((args.length > 0) ? " " + Joiner.on(' ').join(args) : "");
    }
}
