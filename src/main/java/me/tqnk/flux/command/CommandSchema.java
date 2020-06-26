package me.tqnk.flux.command;

import me.tqnk.flux.annotation.FluxHandle;

import java.lang.reflect.Method;

/**
 * Created by MSH on 3/25/2020
 */
public class CommandSchema {
    private FluxHandle commandInfo;
    private Method cmdCallback;

    public CommandSchema(FluxHandle commandInfo, Method cmdCallback) {
        Class<?>[] params = cmdCallback.getParameterTypes();
        assert commandInfo.paramNames().length == params.length - 1 && commandInfo.aliases().length > 0;
        this.commandInfo = commandInfo;
        this.cmdCallback = cmdCallback;
    }

    public FluxHandle getCommandInfo() {
        return commandInfo;
    }

    public Method getCmdCallback() {
        return this.cmdCallback;
    }
}
