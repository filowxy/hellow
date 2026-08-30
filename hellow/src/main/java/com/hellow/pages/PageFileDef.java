package com.hellow.pages;

import com.hellow.element.Element;
import com.hellow.page.Page;
import com.hellow.page.PageLayer;
import com.hellow.page.PageSpec;
import com.hellow.action.Action;
import com.hellow.element.*;
import com.hellow.action.Actions;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A page loaded from a {@code config/hellow/pages/*.json} file: the {@link Page}
 * plus the trigger declaration ("when should this page open").
 *
 * @param page       the parsed page (ready for {@code HellowPages.register})
 * @param triggerScreen class name the page opens on (or null)
 * @param triggerEvent  NeoForge event class name the page opens on (or null)
 * @param strategy    "once" | "everyTime" | "decaying"
 */
public record PageFileDef(Page page, String triggerScreen, String triggerEvent, String strategy) {

    public boolean hasTrigger() {
        return triggerScreen != null || triggerEvent != null;
    }
}