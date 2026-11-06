package net.createmod.ponder.guide;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;

/** Copies explicitly selected local media into a portable pack directory without overwriting files. */
public final class GuideMediaFiles {
    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "gif", "apng", "bmp");
    private static final Set<String> VIDEOS = Set.of("mp4", "webm", "mkv", "mov", "avi", "m4v", "mpg", "mpeg", "ogv", "flv", "wmv");
    private GuideMediaFiles() {}

    public static boolean accepts(Path file, MarkdownMedia.Type type) {
        return (type == MarkdownMedia.Type.IMAGE ? IMAGES : VIDEOS).contains(extension(file));
    }

    public static String importFile(Path mediaRoot, Path selected, MarkdownMedia.Type type) throws IOException {
        Path root = mediaRoot.toAbsolutePath().normalize(), source = selected.toAbsolutePath().normalize();
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) || !accepts(source, type))
            throw new IOException("Select a supported " + (type == MarkdownMedia.Type.IMAGE ? "image" : "video") + " file");
        long limit = (type == MarkdownMedia.Type.IMAGE ? 8L : 256L) * 1024 * 1024;
        if (Files.size(source) > limit) throw new IOException("File exceeds " + limit / (1024 * 1024) + " MiB");
        if (Files.isSymbolicLink(root)) throw new IOException("Media symlinks are unsupported");
        Files.createDirectories(root);
        if (source.startsWith(root)) {
            for (Path path = source; path != null && path.startsWith(root); path = path.getParent())
                if (Files.isSymbolicLink(path)) throw new IOException("Media symlinks are unsupported");
            String relative = root.relativize(source).toString().replace('\\', '/');
            if (relative.matches("[a-zA-Z0-9_./ -]+") && !relative.contains("..")) return "local:" + relative;
        }
        String name = source.getFileName().toString();
        String stem = name.substring(0, name.lastIndexOf('.')).replaceAll("[^a-zA-Z0-9_ -]", "_");
        if (stem.isBlank()) stem = type == MarkdownMedia.Type.IMAGE ? "image" : "video";
        if (stem.length() > 80) stem = stem.substring(0, 80);
        for (int suffix = 1; suffix <= 10000; suffix++) {
            Path target = root.resolve(stem + (suffix == 1 ? "" : "-" + suffix) + "." + extension(source));
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) && Files.mismatch(source, target) == -1)
                    return "local:" + target.getFileName();
                continue;
            }
            try {
                Files.copy(source, target);
                return "local:" + target.getFileName();
            } catch (FileAlreadyExistsException collision) {
                // Another import may have claimed this name. Never replace its contents.
            } catch (IOException failure) {
                Files.deleteIfExists(target);
                throw failure;
            }
        }
        throw new IOException("Too many media files with the same name");
    }

    private static String extension(Path file) {
        String name = file.getFileName().toString();
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
