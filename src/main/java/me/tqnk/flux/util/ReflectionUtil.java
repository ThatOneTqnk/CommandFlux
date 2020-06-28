package me.tqnk.flux.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.brigadier.tree.CommandNode;

import org.bukkit.Bukkit;

public class ReflectionUtil {
    public static Class<?> ICOMPLETION_PROVIDER_CLASS;
    public static Class<?> COMMAND_LISTENER_WRAPPER_CLASS;
    public static Method FILL_USABLE_CMDS_METHOD;
    
    static {
        try {
			ICOMPLETION_PROVIDER_CLASS = getNMSClass("ICompletionProvider");
			COMMAND_LISTENER_WRAPPER_CLASS = getNMSClass("CommandListenerWrapper");
            Class<?> cDispatch = getNMSClass("CommandDispatcher");

            FILL_USABLE_CMDS_METHOD = getNMSClass("CommandDispatcher").getDeclaredMethod("a", CommandNode.class, CommandNode.class, COMMAND_LISTENER_WRAPPER_CLASS, Map.class);
		    FILL_USABLE_CMDS_METHOD.setAccessible(true);
        } catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
    }

    public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
    }
}
