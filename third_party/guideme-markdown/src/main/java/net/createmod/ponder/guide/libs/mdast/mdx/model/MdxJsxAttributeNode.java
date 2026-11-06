package net.createmod.ponder.guide.libs.mdast.mdx.model;

import com.google.gson.stream.JsonWriter;
import net.createmod.ponder.guide.libs.unist.UnistNode;
import java.io.IOException;

/**
 * Potential attributes of {@link MdxJsxElementFields}
 */
public interface MdxJsxAttributeNode extends UnistNode {
    void toJson(JsonWriter writer) throws IOException;
}
