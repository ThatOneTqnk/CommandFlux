package me.tqnk.flux.listener;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import com.google.common.collect.Maps;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;

import me.tqnk.flux.util.ReflectionUtil;
import net.minecraft.server.v1_15_R1.CommandDispatcher;
import net.minecraft.server.v1_15_R1.CommandListenerWrapper;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.ICompletionProvider;
import net.minecraft.server.v1_15_R1.PacketPlayOutCommands;
import net.minecraft.server.v1_15_R1.PlayerConnection;

public class CommandSendListener implements Listener {
    private WeakReference<JavaPlugin> plugin;
    private CommandDispatcher fluxDispatcher;
    private final String FALLBACK_PREFIX;

    public CommandSendListener(JavaPlugin plugin, CommandDispatcher fluxDispatcher, String fallbackPrefix) {
        this.plugin = new WeakReference<JavaPlugin>(plugin);
        this.FALLBACK_PREFIX = fallbackPrefix;
        this.fluxDispatcher = fluxDispatcher;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCmdSend(PlayerCommandSendEvent event) {
        // Hack to fix command sync
        Bukkit.getScheduler().runTaskLater(this.plugin.get(), this::syncCommands, 5L);
    }

    private void syncCommands() {
        CommandDispatcher vanillaDispatcher = ((CraftServer) Bukkit.getServer()).getServer().vanillaCommandDispatcher;
        CommandDispatcher bukkitDispatcher = ((CraftServer) Bukkit.getServer()).getServer().commandDispatcher;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<CommandNode<CommandListenerWrapper>, CommandNode<ICompletionProvider>> map = Maps.newIdentityHashMap();
            RootCommandNode vanillaRoot = new RootCommandNode();

            RootCommandNode<CommandListenerWrapper> vanilla = vanillaDispatcher.a().getRoot();
            map.put(vanilla, vanillaRoot);

            EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
            CommandListenerWrapper listenerWrapper = entityPlayer.getCommandListener();
            PlayerConnection playerConnection = entityPlayer.playerConnection;

            try {
				ReflectionUtil.FILL_USABLE_CMDS_METHOD.invoke(bukkitDispatcher, vanilla, vanillaRoot, listenerWrapper, (Map) map);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

            RootCommandNode<ICompletionProvider> fluxRoot = new RootCommandNode();

            map.put(bukkitDispatcher.a().getRoot(), fluxRoot);

            // First, add in bukkit nodes
            try {
				ReflectionUtil.FILL_USABLE_CMDS_METHOD.invoke(bukkitDispatcher, bukkitDispatcher.a().getRoot(), fluxRoot, listenerWrapper, (Map) map);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

            // Bukkit uses command map to register nodes to its dispatcher, so we must remove ours
            for (CommandNode<CommandListenerWrapper> node : this.fluxDispatcher.a().getRoot().getChildren()) {
                fluxRoot.removeCommand(node.getName());
                fluxRoot.removeCommand(FALLBACK_PREFIX + ":" + node.getName());
            }

            // Now, register flux commands
            

            // First, dump them in a waste node to register nodes for aliases.
            RootCommandNode<ICompletionProvider> dumpFluxRoot = new RootCommandNode();
            RootCommandNode<CommandListenerWrapper> flux = this.fluxDispatcher.a().getRoot();
            map.put(flux, dumpFluxRoot);
            try {
				ReflectionUtil.FILL_USABLE_CMDS_METHOD.invoke(bukkitDispatcher, flux, dumpFluxRoot, listenerWrapper, (Map) map);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

            // Then, register to fluxRoot
            try {
				ReflectionUtil.FILL_USABLE_CMDS_METHOD.invoke(bukkitDispatcher, flux, fluxRoot, listenerWrapper, (Map) map);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

            playerConnection.sendPacket(new PacketPlayOutCommands(fluxRoot));
        }
    }
}
