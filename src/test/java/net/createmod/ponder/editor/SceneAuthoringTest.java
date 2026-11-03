package net.createmod.ponder.editor;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.createmod.ponder.api.editor.EditorActions;
import net.createmod.ponder.api.editor.EditorAnimations;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.structure.PonderStructure;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockDoor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SceneAuthoringTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    private SceneDocument document() {
        return SceneDocument.create("author_test:demo", "minecraft:book", "Authoring test", "ponder:demo/basics");
    }
    private JsonObject idle(int ticks) {
        JsonObject step = new JsonObject(); step.addProperty("op", "scene.idle"); step.addProperty("ticks", ticks); return step;
    }

    @Test public void multiSceneExampleIsAValidPackWithDistinctScenesForOneItem() throws Exception {
        SceneDocument example = ScenePackIO.read(Path.of("examples/json/multi-scene.ponder.json"));
        assertEquals(2, example.sceneCount());
        JsonObject first = example.scene();
        example.selectScene(1);
        assertEquals(first.get("component"), example.scene().get("component"));
        assertNotEquals(first.get("id"), example.scene().get("id"));
    }

    @Test public void undoAndRedoRestoreMetadataAndSavedState() {
        SceneDocument document = document();
        document.editScene(scene -> scene.addProperty("title", "Edited"));
        document.insertStep(1, idle(45));
        assertTrue(document.isDirty());
        document.markSaved(); assertFalse(document.isDirty());
        document.undo(); assertTrue(document.isDirty()); assertEquals(3, document.steps().size());
        document.undo(); assertEquals("Authoring test", document.scene().get("title").getAsString());
        document.redo(); document.redo(); assertFalse(document.isDirty());
        assertEquals(45, document.steps().get(1).getAsJsonObject().get("ticks").getAsInt());
    }

    @Test public void reorderingRetainsThePackEnvelopeAndOtherScenes() {
        SceneDocument document = document();
        document.edit(pack -> {
            JsonObject second = pack.getAsJsonArray("scenes").get(0).deepCopy().getAsJsonObject();
            second.addProperty("id", "author_test:second"); pack.getAsJsonArray("scenes").add(second);
            JsonObject text = new JsonObject(); text.addProperty("author_test:shared", "Kept"); pack.add("sharedText", text);
        });
        document.insertStep(1, idle(50));
        document.moveStep(1, 2);
        assertEquals(20, document.steps().get(1).getAsJsonObject().get("ticks").getAsInt());
        assertEquals(50, document.steps().get(2).getAsJsonObject().get("ticks").getAsInt());
        assertEquals(2, document.sceneCount());
        assertEquals("Kept", document.snapshot().getAsJsonObject("sharedText").get("author_test:shared").getAsString());
        document.selectScene(1); assertEquals(3, document.steps().size());
    }

    @Test public void onlyIdleAdvancesTheStoryboardCursor() {
        SceneDocument document = document();
        JsonObject text = new JsonObject(); text.addProperty("op", "overlay.show_text"); text.addProperty("duration", 200);
        document.insertStep(1, text); document.insertStep(2, idle(30));
        assertEquals(0, document.stepStartTick(1)); assertEquals(0, document.stepStartTick(2));
        assertEquals(30, document.stepStartTick(3)); assertEquals(50, document.stepStartTick(4));
    }

    @Test public void snapshotsAndInsertedObjectsCannotMutateDocumentWithoutHistory() {
        SceneDocument document = document(); JsonObject step = idle(30);
        document.insertStep(1, step); step.addProperty("ticks", 999);
        document.snapshot().addProperty("format", 99);
        assertEquals(30, document.steps().get(1).getAsJsonObject().get("ticks").getAsInt());
        assertEquals(1, document.snapshot().get("format").getAsInt());
    }

    @Test public void saveRoundTripsAndRejectsInvalidDraftWithoutReplacingGoodFile() throws Exception {
        SceneDocument document = document(); Path path = temporary.newFolder().toPath().resolve("demo.ponder.json");
        ScenePackIO.save(path, document); byte[] good = Files.readAllBytes(path);
        assertEquals(document.snapshot(), ScenePackIO.read(path).snapshot());
        document.replaceStep(1, idle(-1));
        try { ScenePackIO.save(path, document); fail("negative wait accepted"); }
        catch (IllegalArgumentException expected) { assertArrayEquals(good, Files.readAllBytes(path)); }
        assertTrue(document.isDirty());
        document.undo(); document.editScene(scene -> scene.addProperty("title", "Saved again"));
        ScenePackIO.save(path, document);
        assertArrayEquals(good, Files.readAllBytes(path.resolveSibling("demo.ponder.json.bak")));
    }

    @Test public void parseAndPreviewDoNotRegisterLiveScenes() {
        int before = ScriptSceneRegistry.localSnapshot(true).size();
        assertEquals(1, ScenePackIO.validate(document().snapshot()).size());
        assertEquals(before, ScriptSceneRegistry.localSnapshot(true).size());
    }

    @Test public void bundledAnimationExamplePassesTheRealInstructionValidator() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(
            "/assets/ponder/editor/examples/animation.ponder.json"), StandardCharsets.UTF_8)) {
            assertEquals(1, ScenePackIO.parse(reader).size());
        }
    }

    @Test public void pathsRejectTraversalAndNormalizeNamespaces() throws Exception {
        Path root = temporary.newFolder().toPath();
        assertEquals(root.resolve("mypack/machines/pump.nbt"), ScenePackIO.resolveId(root, "mypack:machines/pump", ".nbt"));
        for (String bad : new String[]{"../escape", "pack:../escape", "pack:/absolute", "pack:folder/../../escape"}) {
            try { ScenePackIO.resolveId(root, bad, ".nbt"); fail(bad); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void cuboidUsesInclusiveCornersAndNormalizesNegativeCoordinates() {
        CuboidSelection box = new CuboidSelection(new BlockPos(2, 65, 4), new BlockPos(-1, 64, 2));
        assertEquals(24, box.volume());
        CuboidSelection local = box.relativeTo(box.min());
        assertEquals(BlockPos.ORIGIN, local.min()); assertEquals(new BlockPos(3, 1, 2), local.max());
        assertEquals("from_to", local.toJson().get("type").getAsString());
    }

    @Test public void extremeSelectionSaturatesRatherThanWrappingVolume() {
        CuboidSelection box = new CuboidSelection(new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE),
            new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, box.volume());
        try { box.checkCaptureSize(); fail("oversized selection accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void quickAnimationOverlapsTransformsAndUndoesAsOneEdit() {
        SceneDocument document = document(); JsonObject before = document.snapshot();
        CuboidSelection box = new CuboidSelection(BlockPos.ORIGIN, new BlockPos(1, 1, 1));
        EditorAnimations.insert(document, 2, "assembly", box, new Vec3d(0, 1, 0), new Vec3d(0, 90, 0), 20, true);
        assertEquals(8, document.steps().size());
        assertEquals("world.make_independent", document.steps().get(2).getAsJsonObject().get("op").getAsString());
        assertEquals(document.stepStartTick(4), document.stepStartTick(5));
        assertEquals(40, document.stepStartTick(7));
        ScenePackIO.validate(document.snapshot());
        document.undo(); assertEquals(before, document.snapshot()); document.redo();
        EditorAnimations.insert(document, 7, "assembly", box, new Vec3d(0, -1, 0), Vec3d.ZERO, 10, true);
        long definitions = java.util.stream.StreamSupport.stream(document.steps().spliterator(), false)
            .filter(e -> e.getAsJsonObject().get("op").getAsString().equals("world.make_independent")).count();
        assertEquals(1, definitions); ScenePackIO.validate(document.snapshot());
    }

    @Test public void builtInActionTemplatesMatchLegacyEnums() {
        for (EditorActions.Action action : EditorActions.all(null)) {
            if (!action.id().equals("effects.redstone_particles") && !action.id().equals("effects.particles")
                && !action.id().equals("world.create_minecart") && !action.id().equals("world.create_parrot")) continue;
            SceneDocument document = document();
            document.insertStep(2, action.template());
            ScenePackIO.validate(document.snapshot());
        }
    }

    @Test public void captureRetainsLegacyDoorHalfProperty() throws Exception {
        PonderLevel source = new PonderLevel(BlockPos.ORIGIN, null);
        source.setBlockState(new BlockPos(0, 64, 0), Blocks.OAK_DOOR.getDefaultState()
            .withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER), 0);
        byte[] bytes = StructureCapture.capture(source, new CuboidSelection(new BlockPos(0, 64, 0), new BlockPos(0, 64, 0)));
        PonderStructure structure = new PonderStructureLoader().load(new ByteArrayInputStream(bytes), new ResourceLocation("author_test:door"));
        PonderLevel target = new PonderLevel(BlockPos.ORIGIN, null); structure.place(target);
        assertEquals(BlockDoor.EnumDoorHalf.UPPER, target.getBlockState(BlockPos.ORIGIN).getValue(BlockDoor.HALF));
    }

    @Test public void captureKeepsLegacyStatesTileNbtAndNormalizesTheOrigin() throws Exception {
        PonderLevel source = new PonderLevel(BlockPos.ORIGIN, null);
        BlockPos origin = new BlockPos(-3, 64, -5);
        source.setBlockState(origin, Blocks.CHEST.getDefaultState(), 0);
        TileEntityChest chest = new TileEntityChest(); chest.setCustomName("Editor Chest");
        chest.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 3)); source.setTileEntity(origin, chest);
        source.setBlockState(origin.add(1, 0, 0), Blocks.PISTON.getDefaultState().withProperty(BlockPistonBase.FACING, EnumFacing.EAST), 0);
        byte[] bytes = StructureCapture.capture(source, new CuboidSelection(origin.add(1, 0, 0), origin));
        PonderStructure structure = new PonderStructureLoader().load(new ByteArrayInputStream(bytes), new ResourceLocation("author_test:capture"));
        assertEquals(new BlockPos(2, 1, 1), structure.getSize()); assertTrue(structure.getDiagnostics().toString(), structure.getDiagnostics().isEmpty());
        PonderLevel target = new PonderLevel(BlockPos.ORIGIN, null); structure.place(target);
        assertEquals(Blocks.PISTON, target.getBlockState(new BlockPos(1, 0, 0)).getBlock());
        assertEquals(EnumFacing.EAST, target.getBlockState(new BlockPos(1, 0, 0)).getValue(BlockPistonBase.FACING));
        TileEntityChest restored = (TileEntityChest) target.getTileEntity(BlockPos.ORIGIN);
        assertEquals("Editor Chest", restored.getName()); assertEquals(3, restored.getStackInSlot(0).getCount());
        assertEquals(Items.DIAMOND, restored.getStackInSlot(0).getItem());
    }
}
