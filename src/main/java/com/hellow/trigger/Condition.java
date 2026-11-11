package com.hellow.trigger;

import net.minecraft.client.gui.screens.Screen;

@FunctionalInterface
public interface Condition {
    boolean test(Screen screen);
}
