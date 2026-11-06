package net.createmod.ponder.guide.libs.mdast.model;

import net.createmod.ponder.guide.libs.unist.UnistPoint;

/**
 * A point in the source file read from JSON.
 */
record MdAstPoint(int line, int column, int offset) implements UnistPoint {
}
