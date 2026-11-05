package net.createmod.ponder.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ponderaddon.ExampleAddon;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import net.createmod.ponder.Ponder;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionParser;
import org.junit.Test;

public class ExampleAddonDependencyTest {
    private static String declaration() {
        return Arrays.stream(ExampleAddon.class.getAnnotation(Mod.class).dependencies().split(";"))
            .filter(value -> value.contains(":ponder-legacy@"))
            .findFirst().orElseThrow();
    }

    @Test public void exampleAcceptsThePublishedPonderVersion() {
        ArtifactVersion reference = VersionParser.parseVersionReference(declaration().split(":", 2)[1]);
        assertTrue(reference.containsVersion(new DefaultArtifactVersion(Ponder.MOD_ID, Ponder.VERSION)));
        assertTrue(reference.containsVersion(new DefaultArtifactVersion(Ponder.MOD_ID, "1.4.0-cleanroom-alpha.1")));
        assertFalse(reference.containsVersion(new DefaultArtifactVersion(Ponder.MOD_ID, "1.3.9")));
    }

    @Test public void exampleMetadataMatchesItsAnnotation() throws Exception {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("mcmod.info");
        while (resources.hasMoreElements()) {
            try (var reader = new InputStreamReader(resources.nextElement().openStream(), StandardCharsets.UTF_8)) {
                for (var element : new JsonParser().parse(reader).getAsJsonArray()) {
                    JsonObject metadata = element.getAsJsonObject();
                    if (!metadata.get("modid").getAsString().equals(ExampleAddon.MOD_ID)) continue;
                    assertEquals(ExampleAddon.class.getAnnotation(Mod.class).version(), metadata.get("version").getAsString());
                    assertTrue(metadata.getAsJsonArray("dependencies").asList().stream()
                        .anyMatch(value -> value.getAsString().equals(declaration())));
                    return;
                }
            }
        }
        throw new AssertionError("Example addon mcmod.info missing");
    }
}
