package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BuiltinScriptInstallerTest {
    private static final String[] FILES = {
        "basics.zs", "storage.zs", "smelting.zs", "piston.zs",
        "redstone.zs", "render_layers.zs", "fluids.zs", "rail.zs"
    };
    private static final String BUILTIN_PATH = "scripts/ponder/builtin";
    private static final String MARKER_PATH = "config/ponder/builtin-zs-generated.properties";

    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void freshStartupCreatesNoDefaultExamples() throws Exception {
        File game = temporary.newFolder("empty-game");
        BuiltinScriptInstaller.retireUnmodifiedExamples(game);
        assertFalse(new File(game, BUILTIN_PATH).exists());
        assertFalse(new File(game, "scripts/ponder/packs/ponder_editor/examples.ponder.json").exists());
        assertFalse(new File(game, MARKER_PATH).exists());
    }

    @Test
    public void retiresUnchangedScriptsAndEditorPackOutsideTheScriptScanRoot() throws Exception {
        File game = temporary.newFolder("upgrade-game");
        BuiltinScriptInstaller.installOnce(game);
        net.createmod.ponder.editor.EditorExamples.install(game);
        BuiltinScriptInstaller.retireUnmodifiedExamples(game);
        for (String name : FILES) {
            assertFalse(new File(game, BUILTIN_PATH + "/" + name).exists());
            assertArrayEquals(read("assets/ponder/scripts/builtin/" + name),
                Files.readAllBytes(new File(game, "config/ponder/removed-examples/" + BUILTIN_PATH + "/" + name).toPath()));
        }
        assertFalse(new File(game, "scripts/ponder/packs/ponder_editor/examples.ponder.json").exists());
        assertTrue(new File(game, "config/ponder/removed-examples/scripts/ponder/packs/ponder_editor/examples.ponder.json").isFile());
        BuiltinScriptInstaller.retireUnmodifiedExamples(game);
        assertFalse(new File(game, BUILTIN_PATH + "/basics.zs").exists());
    }

    @Test
    public void cleanupPreservesEditedAndCustomContentAndExistingBackups() throws Exception {
        File game = temporary.newFolder("edited-game");
        BuiltinScriptInstaller.installOnce(game);
        net.createmod.ponder.editor.EditorExamples.install(game);
        File basics = new File(game, BUILTIN_PATH + "/basics.zs");
        writeText(basics, "my edited scene");
        File custom = new File(game, BUILTIN_PATH + "/my-scene.zs");
        writeText(custom, "my custom scene");
        File editor = new File(game, "scripts/ponder/packs/ponder_editor/examples.ponder.json");
        writeText(editor, "my edited pack");
        File backup = new File(game, "config/ponder/removed-examples/" + BUILTIN_PATH + "/storage.zs");
        writeText(backup, "an older backup");
        BuiltinScriptInstaller.retireUnmodifiedExamples(game);
        assertEquals("my edited scene", readText(basics));
        assertEquals("my custom scene", readText(custom));
        assertEquals("my edited pack", readText(editor));
        assertEquals("an older backup", readText(backup));
        assertArrayEquals(read("assets/ponder/scripts/builtin/storage.zs"),
            Files.readAllBytes(new File(backup.getPath() + ".1").toPath()));
    }

    @Test
    public void cleanupDoesNotFollowSymlinksToUserScripts() throws Exception {
        File game = temporary.newFolder("symlink-game");
        File outside = temporary.newFolder("outside");
        java.nio.file.Path target = outside.toPath().resolve("basics.zs");
        Files.write(target, read("assets/ponder/scripts/builtin/basics.zs"));
        java.nio.file.Path directory = new File(game, BUILTIN_PATH).toPath();
        Files.createDirectories(directory);
        Files.createSymbolicLink(directory.resolve("basics.zs"), target);
        BuiltinScriptInstaller.retireUnmodifiedExamples(game);
        assertTrue(Files.isSymbolicLink(directory.resolve("basics.zs")));
        assertArrayEquals(read("assets/ponder/scripts/builtin/basics.zs"), Files.readAllBytes(target));
    }

    @Test
    public void firstInstallWritesBuiltinScriptsAndFormatTwoMarker() throws Exception {
        File game = temporary.newFolder("game");
        BuiltinScriptInstaller.installOnce(game);

        File builtin = new File(game, BUILTIN_PATH);
        assertBuiltinMatchesResources(builtin);
        assertMarkerFormat(game, "2");
    }

    @Test
    public void markerExistsLeavesBuiltinUntouched() throws Exception {
        File game = temporary.newFolder("game");
        BuiltinScriptInstaller.installOnce(game);

        File builtin = new File(game, BUILTIN_PATH);
        File basics = new File(builtin, "basics.zs");
        writeText(basics, "marker-preserved");

        BuiltinScriptInstaller.installOnce(game);

        assertEquals("marker-preserved", readText(basics));
        assertEquals(8, countScriptFiles(builtin));
        assertMarkerFormat(game, "2");
    }

    @Test
    public void deletingMarkerRestoresFreshScriptsAndKeepsUtcBackup() throws Exception {
        File game = temporary.newFolder("game");
        BuiltinScriptInstaller.installOnce(game);

        File builtin = new File(game, BUILTIN_PATH);
        File basics = new File(builtin, "basics.zs");
        writeText(basics, "restore-me");

        File marker = new File(game, MARKER_PATH);
        assertTrue(marker.delete());

        Supplier<String> originalStampSupplier = BuiltinScriptInstaller.utcStampSupplier;
        BuiltinScriptInstaller.utcStampSupplier = () -> "restore";
        try {
            BuiltinScriptInstaller.installOnce(game);
        } finally {
            BuiltinScriptInstaller.utcStampSupplier = originalStampSupplier;
        }

        File backup = new File(game, BUILTIN_PATH + ".utc-restore");
        assertTrue(backup.isDirectory());
        assertEquals("restore-me", readText(new File(backup, "basics.zs")));
        assertBuiltinMatchesResources(builtin);
        assertMarkerFormat(game, "2");
    }

    @Test
    public void copyFailureRestoresOldBuiltinAndCleansUpStaging() throws Exception {
        File game = temporary.newFolder("game");
        BuiltinScriptInstaller.installOnce(game);

        File builtin = new File(game, BUILTIN_PATH);
        File basics = new File(builtin, "basics.zs");
        writeText(basics, "copy-failure");

        File marker = new File(game, MARKER_PATH);
        assertTrue(marker.delete());

        BuiltinScriptInstaller.ScriptCopier originalCopier = BuiltinScriptInstaller.scriptCopier;
        Supplier<String> originalStampSupplier = BuiltinScriptInstaller.utcStampSupplier;
        BuiltinScriptInstaller.utcStampSupplier = () -> "copy-failure";
        AtomicInteger count = new AtomicInteger();
        BuiltinScriptInstaller.scriptCopier = (resource, target) -> {
            if (count.incrementAndGet() == 2) throw new IOException("copy failed");
            originalCopier.copy(resource, target);
        };
        try {
            BuiltinScriptInstaller.installOnce(game);
        } finally {
            BuiltinScriptInstaller.scriptCopier = originalCopier;
            BuiltinScriptInstaller.utcStampSupplier = originalStampSupplier;
        }

        assertEquals("copy-failure", readText(basics));
        assertEquals(8, countScriptFiles(builtin));
        assertFalse(new File(game, BUILTIN_PATH + ".staging").exists());
        assertFalse(new File(game, BUILTIN_PATH + ".utc-copy-failure").exists());
        assertFalse(marker.exists());
    }

    @Test
    public void markerWriteFailureRestoresOldBuiltinAndCleansUpStaging() throws Exception {
        File game = temporary.newFolder("game");
        BuiltinScriptInstaller.installOnce(game);

        File builtin = new File(game, BUILTIN_PATH);
        File basics = new File(builtin, "basics.zs");
        writeText(basics, "marker-failure");

        File marker = new File(game, MARKER_PATH);
        assertTrue(marker.delete());

        BuiltinScriptInstaller.MarkerWriter originalWriter = BuiltinScriptInstaller.markerWriter;
        Supplier<String> originalStampSupplier = BuiltinScriptInstaller.utcStampSupplier;
        BuiltinScriptInstaller.utcStampSupplier = () -> "marker-failure";
        BuiltinScriptInstaller.markerWriter = markerPath -> {
            Files.createDirectories(markerPath.getParent());
            File temporaryMarker = new File(markerPath.getParent().toFile(),
                markerPath.getFileName().toString() + ".tmp");
            Files.write(temporaryMarker.toPath(), "tmp".getBytes(StandardCharsets.UTF_8));
            Files.write(markerPath, "format=2".getBytes(StandardCharsets.UTF_8));
            throw new IOException("marker failed after write");
        };
        try {
            BuiltinScriptInstaller.installOnce(game);
        } finally {
            BuiltinScriptInstaller.markerWriter = originalWriter;
            BuiltinScriptInstaller.utcStampSupplier = originalStampSupplier;
        }

        assertEquals("marker-failure", readText(basics));
        assertEquals(8, countScriptFiles(builtin));
        assertFalse(new File(game, BUILTIN_PATH + ".staging").exists());
        assertFalse(new File(game, BUILTIN_PATH + ".utc-marker-failure").exists());
        assertFalse(marker.exists());
        assertFalse(new File(game, MARKER_PATH + ".tmp").exists());
    }

    @Test
    public void bundledScriptsUseStaticApiAndThirtyTwoSecondTimeline() throws Exception {
        for (String name : FILES) {
            byte[] bytes = read("assets/ponder/scripts/builtin/" + name);
            String script = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(script.contains("SceneRegistry.create"));
            assertTrue(script.contains("scene.register();"));
            int total = 0;
            java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("scene\\.idle\\((\\d+)\\)").matcher(script);
            while (matcher.find()) total += Integer.parseInt(matcher.group(1));
            assertEquals("Built-in script must schedule exactly 32 seconds: " + name, 640, total);

            java.util.regex.Matcher timeline = java.util.regex.Pattern
                .compile("scene\\.idle\\((\\d+)\\)|scene\\.addKeyframe\\(\\)")
                .matcher(script);
            int elapsed = 0;
            int keyframes = 0;
            int[] expected = {40, 190, 340, 490};
            while (timeline.find()) {
                if (timeline.group(1) != null) {
                    elapsed += Integer.parseInt(timeline.group(1));
                } else {
                    assertTrue("Too many keyframes: " + name, keyframes < expected.length);
                    assertEquals("Unexpected keyframe time: " + name, expected[keyframes], elapsed);
                    keyframes++;
                }
            }
            assertEquals("Built-in script must declare four keyframes: " + name, 4, keyframes);
            assertEquals("Keyframe timeline must end at 640 ticks: " + name, 640, elapsed);
        }
    }

    private static void assertBuiltinMatchesResources(File builtin) throws Exception {
        assertTrue(builtin.isDirectory());
        assertEquals(8, countScriptFiles(builtin));
        for (String name : FILES) {
            assertArrayEquals(read("assets/ponder/scripts/builtin/" + name),
                Files.readAllBytes(new File(builtin, name).toPath()));
        }
    }

    private static int countScriptFiles(File builtin) {
        File[] files = builtin.listFiles((dir, name) -> name.endsWith(".zs"));
        return files == null ? 0 : files.length;
    }

    private static void assertMarkerFormat(File game, String expected) throws Exception {
        File marker = new File(game, MARKER_PATH);
        assertTrue(marker.isFile());
        Properties properties = new Properties();
        try (java.io.InputStream input = Files.newInputStream(marker.toPath())) {
            properties.load(input);
        }
        assertEquals(expected, properties.getProperty("format"));
    }

    private static void writeText(File file, String text) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String readText(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static byte[] read(String path) throws Exception {
        java.io.InputStream stream = BuiltinScriptInstallerTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new AssertionError("Missing " + path);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) output.write(buffer, 0, read);
            return output.toByteArray();
        } finally {
            stream.close();
        }
    }
}
