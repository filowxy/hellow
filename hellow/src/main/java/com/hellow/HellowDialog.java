package com.hellow;

import com.hellow.element.Element;
import com.hellow.element.RenderContext;
import com.hellow.modal.HellowModal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A generic dialog window that renders an {@link Element} tree as its body.
 *
 * <p>This is Hellow's "show arbitrary content in a window" workhorse: callers build
 * an element tree (usually with {@link com.hellow.element.Elements}) and show it via
 * {@link Hellow#showDialog(Component, Element)}. All interactive behavior of the
 * element tree (buttons, links, toggles, text inputs, scrolling) is forwarded here.
 *
 * <p>Unlike {@link com.hellow.page.PageScreen}, this dialog is self-contained — it
 * does not participate in the page registry/stack; the caller owns its lifecycle via
 * the parent screen.
 */
public class HellowDialog extends HellowModal {
    private static final int DEFAULT_DIALOG_WIDTH = 320;
    private static final int DEFAULT_DIALOG_HEIGHT = 200;

    private final Element body;

    public HellowDialog(Component title, Screen parent, Element body) {
        super(title, parent);
        this.body = body;
        this.dialogWidth = DEFAULT_DIALOG_WIDTH;
        this.dialogHeight = DEFAULT_DIALOG_HEIGHT;
    }

    @Override
    protected void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.body == null) return;
        RenderContext ctx = new RenderContext(g, this.font, bodyLeft(), bodyTop(),
                bodyWidth(), mouseX, mouseY);
        g.enableScissor(bodyLeft(), bodyTop(), bodyLeft() + bodyWidth(), bodyTop() + bodyHeight());
        this.body.render(ctx);
        g.disableScissor();
        Component tooltip = this.body.tooltipAt(ctx);
        if (tooltip != null) {
            g.renderTooltip(this.font, java.util.List.of(tooltip),
                    java.util.Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.body != null) {
            RenderContext ctx = new RenderContext(null, this.font, bodyLeft(), bodyTop(),
                    bodyWidth(), (int) mouseX, (int) mouseY);
            if (this.body.mouseClicked(ctx, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.body != null) {
            RenderContext ctx = new RenderContext(null, this.font, bodyLeft(), bodyTop(),
                    bodyWidth(), (int) mouseX, (int) mouseY);
            if (this.body.mouseScrolled(ctx, deltaX, deltaY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.body != null) {
            RenderContext ctx = new RenderContext(null, this.font, bodyLeft(), bodyTop(),
                    bodyWidth(), (int) mouseX, (int) mouseY);
            if (this.body.mouseDragged(ctx, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.body != null && this.body.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.body != null && this.body.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }
}
