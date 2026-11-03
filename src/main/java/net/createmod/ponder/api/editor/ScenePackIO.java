package net.createmod.ponder.api.editor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.createmod.ponder.script.PonderJsonLoader;
import net.createmod.ponder.script.ScriptSceneDefinition;

/** Public JSON API. Parsing and preview do not register or replace live scenes. */
public final class ScenePackIO {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private ScenePackIO() {}

    public static List<ScriptSceneDefinition> parse(Reader reader) {
        return validate(new JsonParser().parse(reader).getAsJsonObject());
    }

    public static List<ScriptSceneDefinition> validate(JsonObject pack) {
        return PonderJsonLoader.parseScenes(pack, "editor/api");
    }

    public static SceneDocument read(Path path) throws IOException {
        if (Files.size(path) > PonderJsonLoader.MAX_FILE_BYTES) throw new IOException("Pack exceeds 1 MiB");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject pack = new JsonParser().parse(reader).getAsJsonObject();
            validate(pack);
            return new SceneDocument(pack);
        }
    }

    public static Path resolveId(Path root, String id, String suffix) throws IOException {
        if (id == null || !ID.matcher(id).matches() || id.contains("..") || id.contains(":/"))
            throw new IllegalArgumentException("Use a resource ID such as mypack:machines/pump");
        Path base = root.toAbsolutePath().normalize();
        Path result = base.resolve(id.replace(':', '/') + suffix).normalize();
        if (!result.startsWith(base)) throw new IOException("Path outside Ponder directory");
        for (Path segment = result; segment != null; segment = segment.getParent())
            if (Files.isSymbolicLink(segment)) throw new IOException("Symlink in Ponder path: " + segment);
        return result;
    }

    public static void save(Path target, SceneDocument document) throws IOException {
        JsonObject pack = document.snapshot();
        validate(pack);
        byte[] encoded = (JSON.toJson(pack) + "\n").getBytes(StandardCharsets.UTF_8);
        if (encoded.length > PonderJsonLoader.MAX_FILE_BYTES) throw new IOException("Pack exceeds 1 MiB");
        writeAtomically(target, encoded);
        document.markSaved();
    }

    public static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path absolute = target.toAbsolutePath().normalize();
        for (Path segment = absolute; segment != null; segment = segment.getParent())
            if (Files.isSymbolicLink(segment)) throw new IOException("Symlink in Ponder path: " + segment);
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), ".ponder-", ".tmp");
        try {
            Files.write(temporary, bytes);
            if (Files.exists(absolute))
                Files.copy(absolute, absolute.resolveSibling(absolute.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unavailable) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
