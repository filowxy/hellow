package com.hellow.page;

import com.hellow.layout.ContentAlign;

/**
 * Window geometry for a page, replacing the constants {@code PageScreen} used to
 * hardcode. Every field has the historical value as its default, so a page that
 * does not opt in looks exactly as before.
 *
 * @param width            outer window width in GUI pixels, {@link #AUTO} = full screen
 * @param height           outer window height, {@link #AUTO} = full screen
 * @param anchor           where the window sits inside the screen
 * @param padding          gap between the window edge and the content
 * @param titleBarHeight   height reserved above the content for the title
 * @param contentMaxWidth  cap on the content column, {@link #AUTO} = no cap; when
 *                         capped the content is centered inside the window
 */
public record WindowSpec(int width, int height, ContentAlign anchor, int padding,
                         int titleBarHeight, int contentMaxWidth) {

    /** "Use the screen's dimension" sentinel. */
    public static final int AUTO = -1;

    /** The pre-WindowSpec behavior: full screen, 10px padding, 20px title bar. */
    public static final WindowSpec DEFAULT =
            new WindowSpec(AUTO, AUTO, ContentAlign.CENTER_MIDDLE, 10, 20, AUTO);

    public WindowSpec {
        anchor = anchor == null ? ContentAlign.CENTER_MIDDLE : anchor;
        padding = Math.max(0, padding);
        titleBarHeight = Math.max(0, titleBarHeight);
        if (width <= 0) width = AUTO;
        if (height <= 0) height = AUTO;
        if (contentMaxWidth <= 0) contentMaxWidth = AUTO;
    }

    /** A fixed-size centered window with default chrome. */
    public static WindowSpec of(int width, int height) {
        return new WindowSpec(width, height, ContentAlign.CENTER_MIDDLE, 10, 20, AUTO);
    }

    /** A full-screen page with a readable-width content column. */
    public static WindowSpec readingColumn(int contentMaxWidth) {
        return new WindowSpec(AUTO, AUTO, ContentAlign.CENTER_MIDDLE, 10, 20, contentMaxWidth);
    }
}
