package net.createmod.ponder.guide.libs.mdast.gfmstrikethrough;

import net.createmod.ponder.guide.libs.mdast.MdastContext;
import net.createmod.ponder.guide.libs.mdast.MdastExtension;
import net.createmod.ponder.guide.libs.micromark.Token;

public final class GfmStrikethroughMdastExtension {
    public static final MdastExtension INSTANCE = MdastExtension.builder()
            .canContainEol("delete")
            .enter("strikethrough", GfmStrikethroughMdastExtension::enterStrikethrough)
            .exit("strikethrough", GfmStrikethroughMdastExtension::exitStrikethrough)
            .build();

    private GfmStrikethroughMdastExtension() {
    }

    private static void enterStrikethrough(MdastContext context, Token token) {
        context.enter(new MdAstDelete(), token);
    }

    private static void exitStrikethrough(MdastContext context, Token token) {
        context.exit(token);
    }
}
