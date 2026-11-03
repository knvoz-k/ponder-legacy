package net.createmod.ponder.foundation.ui;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.registration.PonderTagRegistry;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

public class PonderCatalogTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    @Test public void mergesSceneAndComponentThemesWithoutDuplicatingMultiSceneItems() {
        PonderTagRegistry tags = tags();
        tags.addTagToComponent(id("test:machines"), id("first:gear"));
        PonderCatalog catalog = new PonderCatalog(Arrays.asList(scene("first:gear", "test:storage"),
            scene("first:gear", "ponder:_all"), scene("second:gear")), tags, component -> true);
        assertEquals(2, catalog.items.size());
        assertEquals(2, catalog.items.get(0).sceneCount);
        assertEquals(2, catalog.items.get(0).tags.size());
        assertEquals(1, catalog.filter(null, id("test:machines"), false, "", component -> "Gear").size());
        assertEquals(1, catalog.filter(null, id("test:storage"), false, "", component -> "Gear").size());
        assertEquals(id("second:gear"), catalog.filter(null, null, true, "", component -> "Gear").get(0).id);
    }

    @Test public void filtersNamespaceAndTranslatedNamesAndIdsWithExclusions() {
        PonderCatalog catalog = new PonderCatalog(Arrays.asList(scene("first:gear"), scene("second:cog"), scene("first:hidden")),
            tags(), component -> !component.getPath().equals("hidden"));
        assertEquals(2, catalog.items.size());
        assertEquals(1, catalog.filter("first", null, false, "齿轮", component -> "铜齿轮").size());
        assertEquals(id("second:cog"), catalog.filter(null, null, false, "SECOND:COG", component -> "铜齿轮").get(0).id);
        assertTrue(catalog.filter("second", null, false, "gear", component -> "铜齿轮").isEmpty());
    }

    @Test public void unlistedHighlightDoesNotCreateAnEmptyVisibleCategory() {
        PonderCatalog catalog = new PonderCatalog(Arrays.asList(scene("first:gear", "test:internal")), tags(), component -> true);
        assertEquals(2, catalog.themes.size());
        assertEquals(1, catalog.filter(null, null, true, "", component -> "Gear").size());
        assertEquals(1, catalog.filter(null, id("test:internal"), false, "", component -> "Gear").size());
    }

    private static PonderTagRegistry tags() {
        PonderTagRegistry tags = new PonderTagRegistry();
        for (String name : Arrays.asList("test:machines", "test:storage")) {
            PonderTag tag = new PonderTag(id(name), null, ItemStack.EMPTY, ItemStack.EMPTY);
            tags.registerTag(tag); tags.listTag(tag);
        }
        return tags;
    }

    private static Map.Entry<ResourceLocation, StoryBoardEntry> scene(String component, String... themes) {
        PonderStoryBoardEntry entry = new PonderStoryBoardEntry((scene, util) -> {}, "test", id("test:structure"), id(component));
        List<ResourceLocation> ids = new ArrayList<>();
        for (String theme : themes) ids.add(id(theme));
        entry.highlightTags(ids.toArray(new ResourceLocation[0]));
        return new AbstractMap.SimpleImmutableEntry<>(id(component), entry);
    }
    private static ResourceLocation id(String id) { return new ResourceLocation(id); }
}
