package net.createmod.ponder.guide.libs.unist;

/**
 * A {@link UnistNode} containing a value.
 */
public interface UnistLiteral extends UnistNode {
    /**
     * The value field can contain any value.
     */
    Object value();
}
