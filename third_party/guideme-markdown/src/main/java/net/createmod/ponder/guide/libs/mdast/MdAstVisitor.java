package net.createmod.ponder.guide.libs.mdast;

import net.createmod.ponder.guide.libs.mdast.model.MdAstNode;

public interface MdAstVisitor {
    default Result beforeNode(MdAstNode node) {
        return Result.CONTINUE;
    }

    default Result afterNode(MdAstNode node) {
        return Result.CONTINUE;
    }

    default void text(String text) {
    }

    enum Result {
        CONTINUE,
        SKIP_CHILDREN,
        STOP
    }
}
