package me.tqnk.flux.exception;

/**
 * Created by MSH on 3/25/2020
 */
public class DispatcherNotFoundException extends Exception {
    public DispatcherNotFoundException() {
        super("Brigadier dispatcher not found, cannot create commands");
    }
}
