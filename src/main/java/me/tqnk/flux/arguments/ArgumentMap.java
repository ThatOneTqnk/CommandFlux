package me.tqnk.flux.arguments;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.server.v1_15_R1.CommandListenerWrapper;

public class ArgumentMap {
    private Map<Class<?>, Supplier<ArgumentType>> argumentTypeProviderMap = new HashMap<>();

    public void addEntry(Class<?> clazz, Supplier<ArgumentType> argSupplier) {
        this.argumentTypeProviderMap.put(clazz, argSupplier);
    }

    public ArgumentType getArgumentType(Class<?> clazz) {
        Supplier<ArgumentType> supplier = argumentTypeProviderMap.getOrDefault(clazz, null);
        return (supplier == null) ? null : supplier.get();
    }

    public ArgumentBuilder<CommandListenerWrapper, ?> generateNode(Class<?> clazz, String paramName) {
        ArgumentType argType = this.getArgumentType(clazz);
        if (argType == null) return null;
        return RequiredArgumentBuilder.argument(paramName, argType);
    }
}
