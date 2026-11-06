package net.createmod.ponder.guide;

import com.google.gson.JsonPrimitive;
import net.minecraft.util.ResourceLocation;

/** Produces literal MDX attributes, so pasted titles and URLs cannot become markup or code. */
public final class MarkdownMedia {
    public enum Type {
        IMAGE("Image"), VIDEO("Video");
        private final String element;
        Type(String element) { this.element = element; }
    }

    private MarkdownMedia() {}

    public static String create(ResourceLocation page, Type type, String source, String title,
                                String poster, int width, int height) {
        if (width < 0 || width > 2048 || height < 0 || height > 2048)
            throw new IllegalArgumentException("Media dimensions must be between 0 and 2048");
        StringBuilder result = new StringBuilder("<").append(type.element);
        attribute(result, "src", source.trim());
        if (!title.isBlank()) attribute(result, type == Type.IMAGE ? "alt" : "title", title);
        if (type == Type.VIDEO && !poster.isBlank()) attribute(result, "poster", poster.trim());
        if (width > 0) result.append(" width={").append(width).append('}');
        if (height > 0) result.append(" height={").append(height).append('}');
        String markdown = result.append(" />").toString();
        MarkdownTutorialCompiler.compile(page, markdown, "Media insertion");
        return markdown;
    }

    private static void attribute(StringBuilder target, String name, String value) {
        target.append(' ').append(name).append("={").append(new JsonPrimitive(value)).append('}');
    }
}
