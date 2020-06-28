package me.tqnk.flux;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import me.tqnk.flux.annotation.FluxHandle;
import me.tqnk.flux.arguments.ArgumentMap;
import me.tqnk.flux.arguments.FluxLiteral;
import me.tqnk.flux.command.CommandSchema;
import me.tqnk.flux.context.FluxCommandWrapper;
import me.tqnk.flux.listener.CommandSendListener;
import net.minecraft.server.v1_15_R1.ArgumentRegistry;
import net.minecraft.server.v1_15_R1.ArgumentSerializerVoid;
import net.minecraft.server.v1_15_R1.CommandDispatcher;
import net.minecraft.server.v1_15_R1.CommandListenerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MSH on 3/25/2020
 */
public class CommandFlux {
    private WeakReference<JavaPlugin> plugin;
    private final String prefix;
    private SimpleCommandMap commandMap;
    private CommandDispatcher cloneDispatcher = new CommandDispatcher();
    private com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> bDispatcher;
    private ArgumentMap fluxArgumentMap = new ArgumentMap(); 
    private CommandSendListener commandSendListener;

    public CommandFlux(JavaPlugin plugin, String prefix) {
        this.plugin = new WeakReference<>(plugin);

        this.prefix = prefix == null ? "flux" : prefix;
        this.bDispatcher = cloneDispatcher.a();
        this.commandMap = ((CraftServer) Bukkit.getServer()).getCommandMap();

        this.fluxArgumentMap.addEntry(Boolean.class, () -> BoolArgumentType.bool());
        this.fluxArgumentMap.addEntry(String.class, () -> StringArgumentType.word());
        this.fluxArgumentMap.addEntry(Float.class, () -> FloatArgumentType.floatArg());
        this.fluxArgumentMap.addEntry(Integer.class, () -> IntegerArgumentType.integer());
        this.fluxArgumentMap.addEntry(Long.class, () -> LongArgumentType.longArg());

        this.commandSendListener = new CommandSendListener(plugin, this.cloneDispatcher, prefix);
    }


    public <T> void registerCommands(Class<T> commandClazz) {
        List<CommandSchema> cmdSchemas = new ArrayList<>();
        for (Method method : commandClazz.getMethods()) {
            FluxHandle fluxHandle = method.getAnnotation(FluxHandle.class);
            if (fluxHandle == null) continue;
            String methodName = method.getName();
            if (!Modifier.isPublic(method.getModifiers())) {
                System.out.println("Method " + methodName + " is not public, but marked with FluxHandle");
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                System.out.println("Method " + methodName + " must be static to be treated as a command");
                continue;
            }
            if(!(method.getReturnType() == boolean.class)) {
                System.out.println("Method " + methodName + " must have a boolean return type to be treated as a command");
                continue;
            }
            cmdSchemas.add(new CommandSchema(fluxHandle, method));
        }

        for (CommandSchema cmdSchema : cmdSchemas) {
            String[] paramNames = cmdSchema.getCommandInfo().paramNames();
            LiteralArgumentBuilder<CommandListenerWrapper> startNode = LiteralArgumentBuilder.<CommandListenerWrapper>literal(cmdSchema.getCommandInfo().aliases()[0]);
            if (!cmdSchema.getCommandInfo().permission().isEmpty()) {
                startNode = startNode.requires((nativeCmdCtx) -> {
                    if (nativeCmdCtx.getBukkitSender() instanceof Player) {
                        return ((Player) nativeCmdCtx.getBukkitSender()).hasPermission(cmdSchema.getCommandInfo().permission());
                    } else return true;
                }); 
            }

            List<ArgumentBuilder<CommandListenerWrapper, ?>> builders = new ArrayList<>();

            int skipParams = 1;
            int paramCounter = 0;
            final Parameter[] paramz = cmdSchema.getCmdCallback().getParameters();
            for (Class<?> paramType : cmdSchema.getCmdCallback().getParameterTypes()) {
                if (skipParams > 0) {
                    skipParams--;
                    continue;
                }
                Parameter param = paramz[paramCounter + 1];

                String paramName = paramNames[paramCounter];
                ArgumentBuilder<CommandListenerWrapper, ?> argBuilder = fluxArgumentMap.generateNode(param, paramType, paramName);
                builders.add(argBuilder);

                paramCounter++;
            }
            int builderLength = builders.size();
            int minArgs = cmdSchema.getCommandInfo().min();

            List<ArgumentBuilder<CommandListenerWrapper, ?>> mappedBuilders = new ArrayList<>();
            int argIndex = (minArgs == 0 ? 0 : minArgs - 1);
            for (int x = 0; x < argIndex; x++) {
                mappedBuilders.add(builders.get(x));
            }
            for (int x = argIndex; x < builderLength; x++) {
                mappedBuilders.add(this.setDispatcher(cmdSchema, builders.get(x), paramNames, x));  
            }

            for (int x = builderLength; x > 1; x--) {
                ArgumentBuilder<CommandListenerWrapper, ?> childBuilder = mappedBuilders.get(x - 1);
                ArgumentBuilder<CommandListenerWrapper, ?> parentBuilder = mappedBuilders.get(x - 2);
                parentBuilder.then(childBuilder);
            }
            System.out.println("Registering " + cmdSchema.getCommandInfo().aliases()[0]);

            if (minArgs == 0) {
                startNode = startNode.executes((nativeCmdCtx) -> {
                    Object[] cmdArgs = new Object[cmdSchema.getCmdCallback().getParameterTypes().length];
                    cmdArgs[0] = nativeCmdCtx.getSource().getBukkitSender();
                    Method cmdMethod = cmdSchema.getCmdCallback();
                    try {
                        return (boolean) cmdMethod.invoke(null, cmdArgs) ? 1 : 0;
                    } catch (IllegalAccessException | InvocationTargetException exception) {
                        return 0;
                    }
                });
            }

            if (builderLength > 0) {
                startNode = startNode.then(mappedBuilders.get(0));
            } 

            String namespace = this.prefix;
            CommandDispatcher vanillaDispatcher = ((CraftServer) Bukkit.getServer()).getServer().vanillaCommandDispatcher;
            CommandNode<CommandListenerWrapper> rootCmdNode = bDispatcher.register(startNode);
            CommandNode<CommandListenerWrapper> fallbackRootCmdNode = bDispatcher.register(
                LiteralArgumentBuilder.<CommandListenerWrapper>literal(this.prefix + ":" + cmdSchema.getCommandInfo().aliases()[0]).redirect(rootCmdNode)
            );

            String[] aliases = cmdSchema.getCommandInfo().aliases();
            for (int aliasC = 1; aliasC < aliases.length; aliasC++) {
                CommandNode<CommandListenerWrapper> aliasCmdNode = bDispatcher.register(LiteralArgumentBuilder.<CommandListenerWrapper>literal(aliases[aliasC]).redirect(rootCmdNode));
                CommandNode<CommandListenerWrapper> fallbackAliasCmdNode = 
                    bDispatcher.register(LiteralArgumentBuilder.<CommandListenerWrapper>literal(this.prefix + ":" + aliases[aliasC]).redirect(rootCmdNode));
                this.commandMap.register(namespace, new FluxCommandWrapper(cmdSchema.getCommandInfo(), cloneDispatcher, aliasCmdNode));
                this.commandMap.register(namespace, new FluxCommandWrapper(cmdSchema.getCommandInfo(), cloneDispatcher, fallbackAliasCmdNode));
            }

            this.commandMap.register(namespace, new FluxCommandWrapper(cmdSchema.getCommandInfo(), cloneDispatcher, rootCmdNode));
            this.commandMap.register(namespace, new FluxCommandWrapper(cmdSchema.getCommandInfo(), cloneDispatcher, fallbackRootCmdNode));
        }
    }

    public void addLiteral(Class<?> paramClass, FluxLiteral literalProvider) {
        this.fluxArgumentMap.addLiteral(paramClass, literalProvider);
    }

    private ArgumentBuilder<CommandListenerWrapper, ?> setDispatcher(CommandSchema schema, 
            ArgumentBuilder<CommandListenerWrapper, ?> builder, String[] paramNames, int upTo) {
        Class<?>[] paramTypes = schema.getCmdCallback().getParameterTypes();
        Method cmdMethod = schema.getCmdCallback();
        ArgumentBuilder<CommandListenerWrapper, ?> mappedBuilder = builder.executes((nativeCmdCtx) -> {
            Object[] dynamicParameters = new Object[paramTypes.length];
            dynamicParameters[0] = nativeCmdCtx.getSource().getBukkitSender();
            for (int x = 0; x <= upTo; x++) {
                String paramName = paramNames[x];
                Class<?> paramType = paramTypes[x + 1]; 
                FluxLiteral fluxLiteral = this.fluxArgumentMap.getMappedLiteral(paramType);
                if (fluxLiteral != null) {
                    dynamicParameters[x + 1] = fluxLiteral.toValue(nativeCmdCtx.getArgument(paramName, String.class));
                } else {
                    dynamicParameters[x + 1] = nativeCmdCtx.getArgument(paramName, paramType);
                }
            }
            try {
                return (boolean) cmdMethod.invoke(null, dynamicParameters) ? 1 : 0;
            } catch (IllegalAccessException | InvocationTargetException exception) {
                return 0;
            }
        });
        return mappedBuilder;
    }
}
