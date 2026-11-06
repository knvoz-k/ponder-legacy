package net.createmod.ponder.metadata;

import static org.junit.Assert.*;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.createmod.ponder.PonderMod;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.toposort.ModSorter;
import net.minecraftforge.fml.common.toposort.ModSortingException;
import net.minecraftforge.fml.common.versioning.*;
import org.junit.Test;
import org.junit.BeforeClass;

/** Exercise Forge's sorting graph with both sides of the optional integration installed. */
public class OptionalIntegrationSortingTest {
    @BeforeClass public static void initializeForgeApiDiscovery() {
        ModAPIManager.INSTANCE.registerDataTableAndParseAPI(new ASMDataTable());
    }
    private static String ponderRules() { return PonderMod.class.getAnnotation(Mod.class).dependencies(); }

    @Test public void ftbQuestsCanLoadAfterPonderAlongsideAe2sTutorials() {
        var order = sort(ponderRules(), "required-after:ftblib;required-after:itemfilters;after:media_api;after:ponder-legacy");
        assertTrue(order.indexOf("ponder-legacy") < order.indexOf("ftbquests"));
        assertTrue(order.indexOf("ponder-legacy") < order.indexOf("ae2s-ponder-legacy"));
    }

    @Test public void previousBidirectionalDeclarationReproducesTheReportedCycle() {
        assertThrows(ModSortingException.class, () -> sort(ponderRules() + ";after:ftbquests", "after:ponder-legacy"));
    }

    @Test public void olderFtbQuestsWithoutPonderOrderingStillSorts() {
        assertTrue(sort(ponderRules(), "required-after:ftblib;after:media_api").contains("ftbquests"));
    }

    @Test public void ftbQuestsRemainsOptional() {
        assertTrue(sort(ponderRules(), null).contains("ponder-legacy"));
    }

    @Test public void packagedMetadataDoesNotReintroduceReverseOrdering() throws Exception {
        boolean found = false;
        var resources = getClass().getClassLoader().getResources("mcmod.info");
        while (resources.hasMoreElements()) try (var stream = resources.nextElement().openStream()) {
            for (var entry : new JsonParser().parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray()) {
                var mod = entry.getAsJsonObject();
                if (!mod.get("modid").getAsString().equals("ponder-legacy")) continue;
                found = true;
                for (var dependency : mod.getAsJsonArray("dependencies"))
                    assertFalse("FTB Quests chooses its own optional ordering", dependency.getAsString().contains(":ftbquests"));
            }
        }
        assertTrue("Packaged Ponder metadata is on the test classpath", found);
    }

    private static List<String> sort(String ponder, String quests) {
        List<ModContainer> mods = new ArrayList<>();
        for (String id : List.of("cleanroom", "forge", "mixinbooter", "ftblib", "itemfilters", "media_api", "ae2"))
            mods.add(container(id, ""));
        mods.add(container("ponder-legacy", ponder));
        mods.add(container("ae2s-ponder-legacy", "required-after:ae2;after:ponder-legacy@[1.5.0,)"));
        if (quests != null) mods.add(container("ftbquests", quests));
        Map<String,ModContainer> byId = new LinkedHashMap<>();
        for (var mod : mods) byId.put(mod.getModId(), mod);
        try {
            var field = Loader.class.getDeclaredField("namedMods"); field.setAccessible(true);
            Object previous = field.get(Loader.instance());
            field.set(Loader.instance(), byId);
            try { return new ModSorter(mods, byId).sort().stream().map(ModContainer::getModId).toList(); }
            finally { field.set(Loader.instance(), previous); }
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static ModContainer container(String id, String rules) {
        ModMetadata metadata = new ModMetadata(); metadata.modId = id; metadata.name = id; metadata.version = "1.5.1";
        List<ArtifactVersion> after = new ArrayList<>(), before = new ArrayList<>();
        for (String rule : rules.split(";")) {
            if (rule.isEmpty()) continue;
            String[] parts = rule.split(":", 2);
            (parts[0].endsWith("before") ? before : after).add(VersionParser.parseVersionReference(parts[1]));
        }
        return new DummyModContainer(metadata) {
            @Override public boolean isImmutable() { return false; }
            @Override public String getSortingRules() { return rules; }
            @Override public List<ArtifactVersion> getDependencies() { return after; }
            @Override public List<ArtifactVersion> getDependants() { return before; }
        };
    }
}
