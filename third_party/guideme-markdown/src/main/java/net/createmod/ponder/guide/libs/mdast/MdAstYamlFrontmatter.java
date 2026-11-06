package net.createmod.ponder.guide.libs.mdast;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.createmod.ponder.guide.libs.mdast.model.MdAstAnyContent;
import net.createmod.ponder.guide.libs.mdast.model.MdAstNode;
import java.io.IOException;

public class MdAstYamlFrontmatter extends MdAstNode implements MdAstAnyContent {
    public static final String TYPE = "yamlFrontmatter";
    public String value = "";

    public MdAstYamlFrontmatter() {
        super(TYPE);
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        writer.name("value").value(value);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        value = readJsonString(jsonObject, "value", "");
    }

    @Override
    public void toText(StringBuilder buffer) {
    }
}
