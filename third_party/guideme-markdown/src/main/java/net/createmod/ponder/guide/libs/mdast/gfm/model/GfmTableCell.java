package net.createmod.ponder.guide.libs.mdast.gfm.model;

import net.createmod.ponder.guide.libs.mdast.model.MdAstAnyContent;
import net.createmod.ponder.guide.libs.mdast.model.MdAstParent;
import net.createmod.ponder.guide.libs.mdast.model.MdAstPhrasingContent;

public class GfmTableCell extends MdAstParent<MdAstPhrasingContent> implements MdAstAnyContent {
    public static final String TYPE = "tableCell";

    public GfmTableCell() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
