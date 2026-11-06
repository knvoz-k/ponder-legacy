package net.createmod.ponder.api.guide;

import net.minecraft.util.ResourceLocation;

/** Register assets/<namespace>/ponder/guides/[<language>/]<path>.md. */
public interface TutorialRegistrationHelper {
    void addMarkdown(ResourceLocation resource);
    void addMarkdown(ResourceLocation resource, String source);
    /** Read {"pages":["relative_page", "namespace:page"]} from ponder/guides/<path>.json. */
    void addMarkdownIndex(ResourceLocation resource);
}
