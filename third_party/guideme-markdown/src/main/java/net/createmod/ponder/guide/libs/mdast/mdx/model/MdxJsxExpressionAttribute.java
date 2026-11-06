package net.createmod.ponder.guide.libs.mdast.mdx.model;

import net.createmod.ponder.guide.libs.mdast.model.MdAstLiteral;

public class MdxJsxExpressionAttribute extends MdAstLiteral implements MdxJsxAttributeNode {
    public static final String TYPE = "mdxJsxExpressionAttribute";

    public MdxJsxExpressionAttribute() {
        super(TYPE);
    }
}
