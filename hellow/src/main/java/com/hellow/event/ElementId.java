package com.hellow.event;

/**
 * Identity of an interactive element inside a page, so callbacks and NeoForge events
 * can say exactly <em>which</em> element was pressed/changed.
 *
 * <p>Format is {@code pageId:elementId}. The {@link #of(String)} parser accepts both
 * the compound form and a bare {@code elementId} (empty page id).
 */
public record ElementId(String pageId, String elementId) {

    public static ElementId of(String pageId, String elementId) {
        return new ElementId(pageId == null ? "" : pageId, elementId == null ? "" : elementId);
    }

    /** Parse {@code "pageId:elementId"}, or treat a plain string as a bare element id. */
    public static ElementId of(String compound) {
        if (compound == null) return new ElementId("", "");
        int idx = compound.indexOf(':');
        if (idx < 0) return new ElementId("", compound);
        return new ElementId(compound.substring(0, idx), compound.substring(idx + 1));
    }

    @Override
    public String toString() {
        return this.pageId + ":" + this.elementId;
    }
}
