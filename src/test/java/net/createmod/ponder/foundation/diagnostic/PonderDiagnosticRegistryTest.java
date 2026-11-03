package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.minecraft.util.ResourceLocation;

public class PonderDiagnosticRegistryTest {
    @Test
    public void oneStoryboardCanBeAssociatedWithDifferentItems() {
        PonderStoryBoard board = (scene, util) -> { };
        List<PonderSceneDiagnostic> result = validateEntries(
            entry(board, "drive", "network"), entry(board, "terminal", "network"));
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(PonderSceneDiagnostic::hasErrors));
    }

    @Test
    public void repeatedAssociationWithTheSameItemIsStillAnError() {
        PonderStoryBoard board = (scene, util) -> { };
        assertTrue(validateEntries(entry(board, "drive", "network"), entry(board, "drive", "network"))
            .stream().allMatch(PonderSceneDiagnostic::hasErrors));
    }

    @Test
    public void distinctStoryboardsCannotClaimTheSameSceneId() {
        PonderStoryBoard first = (scene, util) -> { };
        PonderStoryBoard second = (scene, util) -> { };
        assertTrue(validateEntries(entry(first, "drive", "network"), entry(second, "terminal", "network"))
            .stream().allMatch(PonderSceneDiagnostic::hasErrors));
    }

    @Test
    public void differentStructuresAreNotComponentAliases() {
        PonderStoryBoard board = (scene, util) -> { };
        assertTrue(validateEntries(entry(board, "drive", "network"), entry(board, "terminal", "other"))
            .stream().allMatch(PonderSceneDiagnostic::hasErrors));
    }

    @Test
    public void duplicateScriptSceneIdsAreStillErrors() {
        PonderSceneDiagnostic first = scene("shared", PonderSceneSource.LOCAL_ZS);
        PonderSceneDiagnostic second = scene("shared", PonderSceneSource.LOCAL_JSON);
        assertTrue(PonderDiagnosticRegistry.markDuplicateIds(Arrays.asList(first, second),
            Collections.<String, StoryBoardEntry>emptyMap()).stream()
            .allMatch(PonderSceneDiagnostic::hasErrors));
    }

    @Test
    public void clientEffectiveViewOverridesOnlyLocalScriptScenes() {
        PonderSceneDiagnostic localScript = scene("shared", PonderSceneSource.LOCAL_ZS);
        PonderSceneDiagnostic localJava = scene("java", PonderSceneSource.JAVA_PLUGIN);
        PonderSceneDiagnostic server = scene("shared", PonderSceneSource.SERVER_SNAPSHOT);

        PonderDiagnosticRegistry.ClientViews merged = PonderDiagnosticRegistry.mergeClientViews(
            Arrays.asList(localScript, localJava), Collections.singletonList(server));

        assertTrue(merged.local.get(0).isOverridden());
        assertEquals(PonderSceneSource.SERVER_SNAPSHOT, merged.local.get(0).getOverriddenBy());
        assertTrue(merged.local.get(0).getIssues().stream()
            .map(PonderDiagnosticIssue::getCode).anyMatch("override.server_scene"::equals));
        assertEquals(2, merged.effective.size());
        assertTrue(merged.effective.contains(localJava));
        assertTrue(merged.effective.contains(server));
        assertFalse(merged.effective.contains(localScript));
    }

    @Test
    public void clientEffectiveViewTreatsJsonAsAFirstClassLocalSource() {
        PonderSceneDiagnostic localJson = scene("json_shared", PonderSceneSource.LOCAL_JSON);
        PonderSceneDiagnostic server = scene("json_shared", PonderSceneSource.SERVER_SNAPSHOT);

        PonderDiagnosticRegistry.ClientViews merged = PonderDiagnosticRegistry.mergeClientViews(
            Collections.singletonList(localJson), Collections.singletonList(server));

        assertEquals(PonderSceneSource.LOCAL_JSON, merged.local.get(0).getSource());
        assertTrue(merged.local.get(0).isOverridden());
        assertEquals(Collections.singletonList(server), merged.effective);
    }

    private static StoryBoardEntry entry(PonderStoryBoard board, String component, String structure) {
        return new PonderStoryBoardEntry(board, "test", new ResourceLocation("test", structure),
            new ResourceLocation("test", component)).identifiedBy("shared");
    }

    private static List<PonderSceneDiagnostic> validateEntries(StoryBoardEntry first, StoryBoardEntry second) {
        Map<String, StoryBoardEntry> entries = new LinkedHashMap<String, StoryBoardEntry>();
        entries.put("first", first);
        entries.put("second", second);
        List<PonderSceneDiagnostic> diagnostics = new java.util.ArrayList<PonderSceneDiagnostic>();
        for (Map.Entry<String, StoryBoardEntry> registration : entries.entrySet()) {
            StoryBoardEntry entry = registration.getValue();
            diagnostics.add(new PonderSceneDiagnostic(registration.getKey(), entry.getDeclaredSceneId(),
                entry.getComponent(), entry.getSchematicLocation(), "Shared", PonderSceneSource.JAVA_PLUGIN,
                "test.Plugin", entry.getNamespace(), null, "", Collections.<ResourceLocation>emptyList(),
                1, 20, Collections.<Integer>emptyList(), Collections.<PonderDiagnosticIssue>emptyList(), null));
        }
        return PonderDiagnosticRegistry.markDuplicateIds(diagnostics, entries);
    }

    private static PonderSceneDiagnostic scene(String path, PonderSceneSource source) {
        ResourceLocation id = new ResourceLocation("test", path);
        return new PonderSceneDiagnostic(source.name() + ":" + path, id,
            new ResourceLocation("test", "component"), new ResourceLocation("test", "structure"),
            path, source, "", "test", null, "", Collections.<ResourceLocation>emptyList(),
            1, 20, Collections.<Integer>emptyList(),
            Collections.<PonderDiagnosticIssue>emptyList(), null);
    }
}
