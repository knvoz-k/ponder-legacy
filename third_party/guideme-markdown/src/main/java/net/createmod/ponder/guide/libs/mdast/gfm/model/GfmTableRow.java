package net.createmod.ponder.guide.libs.mdast.gfm.model;

import net.createmod.ponder.guide.libs.mdast.model.MdAstAnyContent;
import net.createmod.ponder.guide.libs.mdast.model.MdAstParent;

public class GfmTableRow extends MdAstParent<GfmTableCell> implements MdAstAnyContent {
    public static final String TYPE = "tableRow";

    public GfmTableRow() {
        super(TYPE);
    }

    @Override
    protected Class<GfmTableCell> childClass() {
        return GfmTableCell.class;
    }
}
