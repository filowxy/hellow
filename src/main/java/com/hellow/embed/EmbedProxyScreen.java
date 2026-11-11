package com.hellow.embed;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EmbedProxyScreen extends Screen {

    private final EmbedNavigation.Sink owner;
    private boolean handedBack;

    EmbedProxyScreen(EmbedNavigation.Sink owner) {
        super(Component.empty());
        this.owner = owner;
    }

    EmbedNavigation.Sink owner() {
        return this.owner;
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void tick() {
        if (this.handedBack) return;
        this.handedBack = true;
        EmbedNavigation.resurface(this.owner);
    }
}
