package net.createmod.ponder.guide;

import java.util.*;
import net.createmod.ponder.api.guide.*;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.util.ResourceLocation;

/** Built with each PonderIndex replacement, so documents and generated storyboards swap together. */
public final class TutorialRegistry {
    private final Map<ResourceLocation, Tutorial> pages;
    public TutorialRegistry(Map<ResourceLocation, Tutorial> pages) { this.pages = Collections.unmodifiableMap(new LinkedHashMap<>(pages)); }
    public static TutorialRegistry empty() { return new TutorialRegistry(Map.of()); }
    public Tutorial get(ResourceLocation id) { return pages.get(id); }
    public Collection<Tutorial> all() { return pages.values(); }
    public List<Tutorial> forComponent(ResourceLocation component) {
        return pages.values().stream().filter(t -> t.components().contains(component)).toList();
    }
    public boolean containsComponent(ResourceLocation component) {
        return pages.values().stream().anyMatch(t -> t.components().contains(component));
    }
    public Set<ResourceLocation> components() {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        pages.values().forEach(t -> result.addAll(t.components())); return Collections.unmodifiableSet(result);
    }
    public static final class Builder implements TutorialRegistrationHelper {
        private final Map<ResourceLocation, Tutorial> pages = new LinkedHashMap<>();
        @Override public void addMarkdown(ResourceLocation resource) {
            String source = GuideSources.read(resource);
            addMarkdown(resource, source);
        }
        @Override public void addMarkdown(ResourceLocation resource, String source) {
            Tutorial page = MarkdownTutorialCompiler.compile(resource, source, resource + ".md");
            if (pages.putIfAbsent(page.id(), page) != null) throw new IllegalArgumentException("Duplicate tutorial: " + page.id());
        }
        @Override public void addMarkdownIndex(ResourceLocation resource) {
            for (ResourceLocation page : GuideSources.readIndex(resource)) addMarkdown(page);
        }
        public void loadLocal() { GuideSources.loadLocal(pages); }
        public TutorialRegistry build(net.createmod.ponder.foundation.registration.PonderSceneRegistry registry) {
            for (Tutorial tutorial : pages.values()) {
                if (tutorial.scenes().isEmpty()) continue;
                // A logical scene shares one storyboard identity across every associated item.
                var board = tutorial.scenes().get(0).asStoryBoard();
                for (var definition : tutorial.scenes()) {
                registry.removeTutorialScene(definition.getComponent(), definition.getSceneId());
                var helper = new net.createmod.ponder.foundation.registration.DefaultPonderSceneRegistrationHelper(
                    tutorial.id().getNamespace(), registry);
                helper.addStoryBoard(definition.getComponent(), definition.getStructure(), board,
                    definition.getTags().toArray(new ResourceLocation[0])).identifiedBy(definition.getSceneId());
                }
            }
            return new TutorialRegistry(pages);
        }
    }
}
