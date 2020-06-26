package me.tqnk.flux.arguments;

import java.util.List;

public interface FluxLiteral<T> {
    List<String> getChoices();
    T toValue(String choice);
}
