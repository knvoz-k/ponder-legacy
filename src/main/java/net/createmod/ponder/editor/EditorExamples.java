package net.createmod.ponder.editor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.editor.ScenePackIO;

public final class EditorExamples {
    private EditorExamples() {}
    public static void install(File gameDirectory) {
        Path target = gameDirectory.toPath().resolve("scripts/ponder/packs/ponder_editor/examples.ponder.json");
        if (Files.exists(target)) return;
        try (InputStream source = EditorExamples.class.getResourceAsStream("/assets/ponder/editor/examples/animation.ponder.json")) {
            if (source == null) throw new IOException("Missing bundled editor example");
            ScenePackIO.writeAtomically(target, source.readAllBytes());
        } catch (IOException failure) {
            Ponder.LOGGER.warn("Could not install Ponder editor example", failure);
        }
    }
}
