package me.tqnk.flux.arguments;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.ArrayList;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import me.tqnk.flux.annotation.Greedy;
import me.tqnk.flux.annotation.LongRange;
import me.tqnk.flux.annotation.Quoted;
import net.minecraft.server.v1_15_R1.CommandListenerWrapper;
import net.minecraft.server.v1_15_R1.ICompletionProvider;

public class ArgumentMap {
    private Map<Class<?>, FluxLiteral> fluxLiterals = new HashMap<>(); 
    private Map<Class<?>, Supplier<ArgumentType>> argumentTypeProviderMap = new HashMap<>();

    public void addEntry(Class<?> clazz, Supplier<ArgumentType> argSupplier) {
        this.argumentTypeProviderMap.put(clazz, argSupplier);
    }

    public ArgumentType getArgumentType(Class<?> clazz) {
        Supplier<ArgumentType> supplier = argumentTypeProviderMap.getOrDefault(clazz, null);
        return (supplier == null) ? null : supplier.get();
    }

    public void addLiteral(Class<?> clazz, FluxLiteral literal) {
        this.fluxLiterals.put(clazz, literal);
    }

    public FluxLiteral getMappedLiteral(Class<?> clazz) {
        return this.fluxLiterals.getOrDefault(clazz, null);
    }

    public ArgumentBuilder<CommandListenerWrapper, ?> generateNode(Parameter relevantParameter, Class<?> clazz, String paramName) {
        FluxLiteral literalResult = fluxLiterals.getOrDefault(clazz, null);
        if (literalResult != null) {
            RequiredArgumentBuilder<CommandListenerWrapper, ?> epicBuilder = RequiredArgumentBuilder.argument(paramName, StringArgumentType.word());
            epicBuilder = epicBuilder.suggests((cmdctx, suggestionbuilder) -> {
                return ICompletionProvider.b(literalResult.getChoices(), suggestionbuilder);
            });
            return epicBuilder;
        }

        ArgumentType argType = this.preprocess(relevantParameter, clazz);
        if (argType == null) return null;
        return RequiredArgumentBuilder.argument(paramName, argType);
    }

    // Preprocess for special cases
    private ArgumentType preprocess(Parameter relevantParameter, Class<?> clazz) {

        Annotation[] annotations = relevantParameter.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == Greedy.class && clazz == String.class) {
                return StringArgumentType.greedyString();
            } else if (annotation.annotationType() == Quoted.class && clazz == String.class) {
                return StringArgumentType.string();
            } else if (annotation.annotationType() == LongRange.class && clazz == Long.class) {
                LongRange annInstance = relevantParameter.getAnnotation(LongRange.class);
                long minBound = annInstance.minBound();
                long maxBound = annInstance.maxBound();
                return (maxBound == -1) ? LongArgumentType.longArg(minBound) : LongArgumentType.longArg(minBound, maxBound);
            }
        }
        return this.getArgumentType(clazz);
    }
}
