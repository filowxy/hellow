package com.hellow.event;

public record ElementId(String pageId, String elementId) {

    public static ElementId of(String pageId, String elementId) {
        return new ElementId(pageId == null ? "" : pageId, elementId == null ? "" : elementId);
    }

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
