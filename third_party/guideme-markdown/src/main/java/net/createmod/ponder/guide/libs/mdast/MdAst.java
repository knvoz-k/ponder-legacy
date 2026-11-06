package net.createmod.ponder.guide.libs.mdast;

import net.createmod.ponder.guide.libs.mdast.model.MdAstRoot;
import net.createmod.ponder.guide.libs.micromark.Micromark;

public final class MdAst {
    private MdAst() {
    }

    public static MdAstRoot fromMarkdown(String markdown, MdastOptions options) {
        var evts = Micromark.parseAndPostprocess(markdown, options);
        return new MdastCompiler(options).compile(evts);
    }
}
