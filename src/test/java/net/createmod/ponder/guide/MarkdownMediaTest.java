package net.createmod.ponder.guide;

import java.io.IOException;
import java.nio.file.*;
import net.createmod.ponder.api.editor.MarkdownDocument;
import net.createmod.ponder.api.guide.Tutorial.Kind;
import net.minecraft.util.ResourceLocation;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class MarkdownMediaTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private static final ResourceLocation ID = new ResourceLocation("test:guide/page");

    @Test public void titlesAndUrlCharactersRoundTripAsLiteralAttributes() {
        String title = "A \"quoted\" <Image /> & {title} \\ path\n中文";
        String source = "https://example.org/image.png?a=1&label=%22sample%22";
        String markdown = MarkdownMedia.create(ID, MarkdownMedia.Type.IMAGE, source, title, "", 0, 100);
        var block = MarkdownTutorialCompiler.compile(ID, markdown, "media.md").blocks().get(0);
        assertEquals(Kind.IMAGE, block.kind()); assertEquals(source, block.target()); assertEquals(title, block.text());
        assertEquals(0, block.media().width()); assertEquals(100, block.media().height());
    }

    @Test public void videoPreservesPosterAndOptionalDimensions() {
        String markdown = MarkdownMedia.create(ID, MarkdownMedia.Type.VIDEO, "local:build.mp4", "Building", "local:poster.png", 180, 0);
        var block = MarkdownTutorialCompiler.compile(ID, markdown, "media.md").blocks().get(0);
        assertEquals(Kind.VIDEO, block.kind()); assertEquals("local:poster.png", block.media().poster());
        assertEquals(180, block.media().width()); assertEquals(0, block.media().height());
        assertTrue(MarkdownMedia.create(ID, MarkdownMedia.Type.VIDEO, "BV1abc123", "", "", 0, 0).contains("BV1abc123"));
    }

    @Test public void invalidSourcesAndDimensionsCannotBeInserted() {
        for (String source : new String[] {"", "local:../outside.png", "media:/outside.mp4", "https://user:pass@example.org/a.png"})
            assertThrows(IllegalArgumentException.class, () -> MarkdownMedia.create(ID, MarkdownMedia.Type.IMAGE, source, "", "", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> MarkdownMedia.create(ID, MarkdownMedia.Type.VIDEO, "local:a.mp4", "", "", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> MarkdownMedia.create(ID, MarkdownMedia.Type.VIDEO, "local:a.mp4", "", "", 0, 2049));
    }

    @Test public void insertionPreservesHeaderAndIsOneUndoStep() {
        String original = "---\nid: test:guide/page\npresentation: guide\n---\n# Guide\n";
        MarkdownDocument document = new MarkdownDocument(original);
        String block = MarkdownMedia.create(ID, MarkdownMedia.Type.IMAGE, "local:photo.png", "Photo", "", 0, 0);
        document.insertBlock(block);
        var tutorial = MarkdownTutorialCompiler.compile(ID, document.text(), "page.md");
        assertEquals(ID, tutorial.id()); assertTrue(tutorial.blocks().stream().anyMatch(b -> b.kind() == Kind.IMAGE));
        document.undo(); assertEquals(original, document.text()); assertEquals(0, document.cursor());
        document.redo(); assertTrue(document.text().contains(block));
        document = new MarkdownDocument("before selected after"); document.select(7, false); document.select(15, true);
        document.insertBlock(block); assertTrue(document.text().startsWith("before \n\n<Image"));
        document.undo(); assertEquals("selected", document.selected());
        document = new MarkdownDocument("---\n---\n# Guide\n"); document.insertBlock(block);
        assertEquals(Kind.IMAGE, MarkdownTutorialCompiler.compile(ID, document.text(), "empty-header.md").blocks().get(0).kind());
    }

    @Test public void externalImportsStayPortableAndNeverOverwriteSameNamedFiles() throws Exception {
        Path root = tmp.newFolder("media").toPath(), external = tmp.newFolder("external").toPath();
        Path first = external.resolve("image.png"); Files.writeString(first, "first");
        assertEquals("local:image.png", GuideMediaFiles.importFile(root, first, MarkdownMedia.Type.IMAGE));
        assertEquals("local:image.png", GuideMediaFiles.importFile(root, first, MarkdownMedia.Type.IMAGE));
        Files.writeString(first, "second");
        assertEquals("local:image-2.png", GuideMediaFiles.importFile(root, first, MarkdownMedia.Type.IMAGE));
        assertEquals("first", Files.readString(root.resolve("image.png")));
        assertEquals("second", Files.readString(first));
        assertEquals("local:image-2.png", GuideMediaFiles.importFile(root, root.resolve("image-2.png"), MarkdownMedia.Type.IMAGE));
    }

    @Test public void nonAsciiFilenamesAreCopiedToValidLocalReferences() throws Exception {
        Path root = tmp.newFolder("media").toPath(), image = tmp.newFile("电路图.PNG").toPath(); Files.writeString(image, "image");
        String reference = GuideMediaFiles.importFile(root, image, MarkdownMedia.Type.IMAGE);
        assertTrue(Files.isRegularFile(root.resolve(reference.substring(6))));
        assertEquals(Kind.IMAGE, MarkdownTutorialCompiler.compile(ID,
            MarkdownMedia.create(ID, MarkdownMedia.Type.IMAGE, reference, "电路图", "", 0, 0), "media.md").blocks().get(0).kind());
    }

    @Test public void unsupportedFilesAndSymlinksAreRejected() throws Exception {
        Path root = tmp.newFolder("media").toPath(), file = tmp.newFile("demo.mp4").toPath();
        assertThrows(IOException.class, () -> GuideMediaFiles.importFile(root, file, MarkdownMedia.Type.IMAGE));
        Path linked = root.resolve("linked.mp4"); Files.createSymbolicLink(linked, file);
        assertThrows(IOException.class, () -> GuideMediaFiles.importFile(root, linked, MarkdownMedia.Type.VIDEO));
        Path linkedRoot = tmp.getRoot().toPath().resolve("linked-root"); Files.createSymbolicLink(linkedRoot, root);
        assertThrows(IOException.class, () -> GuideMediaFiles.importFile(linkedRoot, file, MarkdownMedia.Type.VIDEO));
    }
}
