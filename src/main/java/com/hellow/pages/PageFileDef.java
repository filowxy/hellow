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

public record PageFileDef(Page page, String triggerScreen, String triggerEvent, String strategy) {

    public boolean hasTrigger() {
        return triggerScreen != null || triggerEvent != null;
    }
}
