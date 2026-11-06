package net.createmod.ponder.guide.libs.mdast.gfmstrikethrough;

import net.createmod.ponder.guide.libs.mdast.model.MdAstParent;
import net.createmod.ponder.guide.libs.mdast.model.MdAstPhrasingContent;
import net.createmod.ponder.guide.libs.mdast.model.MdAstStaticPhrasingContent;

/**
 * Delete (Parent) represents contents that are no longer accurate or no longer relevant. Delete can be used where
 * phrasing content is expected. Its content model is phrasing content.
 * 
 * <pre>
 * {type:'delete',children:[{type:'text',value:'alpha'}]}
 * </pre>
 */
public class MdAstDelete extends MdAstParent<MdAstPhrasingContent> implements MdAstStaticPhrasingContent {
    public static final String TYPE = "delete";

    public MdAstDelete() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
