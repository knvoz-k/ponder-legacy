package net.createmod.ponder.guide.libs.micromark.extensions.gfmstrikethrough;

import net.createmod.ponder.guide.libs.micromark.html.HtmlExtension;

public final class GfmStrikethroughHtml {
    private GfmStrikethroughHtml() {
    }

    public static final HtmlExtension EXTENSION = HtmlExtension.builder()
            .enter("strikethrough", (context, token) -> context.tag("<del>"))
            .exit("strikethrough", (context, token) -> context.tag("</del>"))
            .build();
}
