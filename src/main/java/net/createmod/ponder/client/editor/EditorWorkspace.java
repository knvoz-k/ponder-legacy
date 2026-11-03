package net.createmod.ponder.client.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.editor.EditorNetwork;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Keeps the unsaved editor session alive while the player builds or selects blocks. */
@SideOnly(Side.CLIENT)
public final class EditorWorkspace {
    private static EditorScreen screen;
    private static boolean selecting;
    private static BlockPos first, second;
    private static long pendingRequest;
    private static long pendingUntil;
    private static String pendingStructure;
    private static EditorScreen pendingOwner;
    private static String pendingScene;
    private EditorWorkspace() {}

    public static Path packs() { return Minecraft.getMinecraft().gameDir.toPath().resolve("scripts/ponder/packs"); }
    public static Path structures() { return Minecraft.getMinecraft().gameDir.toPath().resolve("scripts/ponder/structures"); }

    public static void open() {
        if (screen == null) {
            String id = "local:scene_" + Long.toString(System.currentTimeMillis(), 36);
            screen = new EditorScreen(SceneDocument.create(id, "minecraft:book", "New Ponder scene", "ponder:demo/basics"), null);
        }
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }

    static void open(SceneDocument document, Path path) {
        screen = new EditorScreen(document, path); open();
    }

    /** Opens the local source of a JSON scene currently displayed by the Ponder viewer. */
    public static void openScene(ResourceLocation sceneId) {
        if (screen != null && screen.document().scene().get("id").getAsString().equals(sceneId.toString())) { open(); return; }
        if (screen != null && screen.document().isDirty()) {
            message(EditorWidgets.tr("draft_open")); open(); return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(packs())) {
            for (Path path : (Iterable<Path>)paths.filter(p -> p.toString().endsWith(".ponder.json") && !Files.isSymbolicLink(p)).sorted()::iterator) {
                SceneDocument document;
                try { document = ScenePackIO.read(path); }
                catch (RuntimeException | IOException invalid) { continue; }
                for (int i = 0; i < document.sceneCount(); i++) {
                    document.selectScene(i);
                    if (document.scene().get("id").getAsString().equals(sceneId.toString())) { open(document, path); return; }
                }
            }
            message(EditorWidgets.tr("no_json_source", sceneId));
        } catch (IOException failure) { message(failure.getMessage()); }
    }

    public static boolean selecting() { return selecting; }
    public static void toggleSelection() {
        selecting = !selecting;
        message(EditorWidgets.tr(selecting ? "selection_enabled" : "selection_disabled"));
    }

    static void selectInWorld() {
        selecting = true;
        Minecraft.getMinecraft().displayGuiScreen(null);
        message(EditorWidgets.tr("selection_enabled"));
    }

    public static void selectCorner(BlockPos pos, boolean end) {
        if (pos == null) return;
        if (end) second = pos.toImmutable(); else first = pos.toImmutable();
        message(EditorWidgets.tr(end ? "corner_b" : "corner_a", pos.getX(), pos.getY(), pos.getZ()));
    }

    public static CuboidSelection worldSelection() {
        return first == null || second == null ? null : new CuboidSelection(first, second);
    }

    static void capture(String structure) throws IOException {
        CuboidSelection selection = worldSelection();
        if (selection == null) throw new IllegalArgumentException(EditorWidgets.tr("select_both"));
        selection.checkCaptureSize();
        ScenePackIO.resolveId(structures(), structure, ".nbt");
        if (pendingStructure != null && System.currentTimeMillis() < pendingUntil)
            throw new IllegalArgumentException(EditorWidgets.tr("capture_wait"));
        pendingRequest = UUID.randomUUID().getMostSignificantBits();
        pendingStructure = structure; pendingUntil = System.currentTimeMillis() + 15_000;
        pendingOwner = screen;
        pendingScene = screen == null ? null : screen.document().scene().get("id").getAsString();
        EditorNetwork.CHANNEL.sendToServer(new EditorNetwork.CaptureRequest(pendingRequest, selection));
    }

    public static void receiveCapture(long request, byte[] bytes, String error) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (request != pendingRequest || pendingStructure == null) return;
            String structure = pendingStructure;
            EditorScreen owner = pendingOwner;
            String scene = pendingScene;
            pendingStructure = null; pendingOwner = null; pendingScene = null;
            try {
                if (!error.isEmpty()) throw new IOException(error);
                Path path = ScenePackIO.resolveId(structures(), structure, ".nbt");
                ScenePackIO.writeAtomically(path, bytes);
                PonderStructureLoader.invalidateCaches();
                selecting = false;
                if (owner != null) {
                    owner.setStructure(scene, structure);
                    owner.setStatus(EditorWidgets.tr("captured", bytes.length), false);
                }
                message(EditorWidgets.tr("captured", bytes.length));
            } catch (IOException | RuntimeException failure) {
                message(failure.getMessage());
                Ponder.LOGGER.warn("Ponder structure capture failed", failure);
                if (owner != null) owner.setStatus(failure.getMessage(), true);
            }
        });
    }

    public static void tick() {
        if (pendingStructure != null && System.currentTimeMillis() > pendingUntil) {
            pendingStructure = null;
            pendingOwner = null; pendingScene = null;
            message(EditorWidgets.tr("capture_timeout"));
        }
    }

    public static void disconnected() {
        selecting = false; first = null; second = null; pendingStructure = null;
        pendingOwner = null; pendingScene = null;
    }

    public static void message(String text) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) mc.player.sendMessage(new TextComponentString("[Ponder] " + text));
    }
}
