package net.createmod.ponder.verification.mmce;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.createmod.ponder.mmce.script.MMCEStructures;
import net.createmod.ponder.mmce.structure.MMCEStructureProvider;
import net.createmod.ponder.script.PonderJsonLoader;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

final class MMCEIntegrationChecks {
    static final ResourceLocation STATIC = new ResourceLocation("modularmachinery:ponder_mmce_static_demo");
    static final ResourceLocation DYNAMIC = new ResourceLocation("modularmachinery:ponder_mmce_dynamic_demo");

    static void verify(JsonArray checks) throws Exception {
        record(checks, MachineRegistry.getRegistry().getMachine(STATIC) != null, "MMCE loads real machine JSON");
        MMCEStructureRef fixed = MMCEStructures.machine(STATIC.toString());
        MMCEStructureRef dynamic = MMCEStructures.dynamic(DYNAMIC.toString(), "line", 3, "north", "north");
        PonderScene fixedScene = scene(fixed.component, "ponder_mmce:static_demo");
        PonderScene dynamicScene = scene(dynamic.component, "ponder_mmce:dynamic_demo");
        record(checks, fixed.sizeX > 0 && fixed.controllerX >= 0 && fixed.fingerprint.length() == 64,
            "static structure normalizes negative coordinates and has a stable fingerprint");
        record(checks, fixed.structure.equals(MMCEStructures.machine(STATIC.toString()).structure), "stable repeated structure reference");
        Map<String, List<BlockPos>> fixedGroups = ((PonderLevel) fixedScene.getWorld()).getStructureGroups();
        for (String name : new String[] {"mmce:all", "mmce:controller", "mmce:tag/item_input", "mmce:tag/preview_nbt"})
            record(checks, fixedGroups.containsKey(name) && !fixedGroups.get(name).isEmpty(), "static group " + name);
        for (List<BlockPos> group : fixedGroups.values())
            for (BlockPos p : group)
                if (p.getX() < 0 || p.getY() < 0 || p.getZ() < 0) throw new IllegalStateException("negative normalized position " + p);
        record(checks, fixedScene.getWorld().loadedTileEntityList.stream().anyMatch(tile ->
            "Ponder MMCE Preview".equals(tile.writeToNBT(new NBTTagCompound()).getString("CustomName"))), "preview NBT survives structure placement");
        Map<String, List<BlockPos>> dynamicGroups = ((PonderLevel) dynamicScene.getWorld()).getStructureGroups();
        for (String name : new String[] {"mmce:dynamic/line/segment/0/frame", "mmce:dynamic/line/segment/1/frame",
                "mmce:dynamic/line/segment/2/frame", "mmce:dynamic/line/end/output"})
            record(checks, dynamicGroups.containsKey(name) && !dynamicGroups.get(name).isEmpty(), "dynamic group " + name);
        for (ResourceLocation machine : new ResourceLocation[] {STATIC, DYNAMIC, new ResourceLocation("modularmachinery:ponder_mmce_unconfigured")}) {
            ResolvedPonderSubject subject = PonderSubjectResolvers.resolve(blueprint(machine));
            record(checks, subject.isHandled() && new ResourceLocation("ponder_mmce:blueprint").equals(subject.getResolverId())
                && MMCEStructureRef.componentId(machine).equals(subject.getComponent()), "blueprint mapping " + machine);
            record(checks, PonderIndex.getSceneAccess().doScenesExistForId(subject.getComponent()) == !machine.getPath().endsWith("unconfigured"),
                "blueprints only offer author-provided scenes " + machine);
        }
        record(checks, !MMCEStructureProvider.INSTANCE.find(new ResourceLocation("minecraft:unrelated")).isFound(), "unrelated structures pass to other providers");
        MMCEStructureRef stale = fixed.resolved("0".repeat(64), fixed.sizeX, fixed.sizeY, fixed.sizeZ,
            fixed.controllerX, fixed.controllerY, fixed.controllerZ);
        boolean rejected = false;
        try { MMCEStructureProvider.INSTANCE.find(stale.asResourceLocation()); }
        catch (IOException expected) { rejected = expected.getMessage().contains("fingerprint mismatch"); }
        record(checks, rejected, "stale structure fingerprint is rejected");
        MMCEStructureProvider.INSTANCE.invalidate();
        record(checks, MMCEStructureProvider.INSTANCE.find(dynamic.asResourceLocation()).isFound(), "cache invalidation reloads valid dynamic structures");

        // The same generated structure reference is accepted by JSON packs and the editor document API.
        SceneDocument document = SceneDocument.create("ponder_mmce:json_fixture", fixed.component, "MMCE JSON fixture", fixed.structure);
        ScriptSceneDefinition definition = PonderJsonLoader.parseScenes(document.snapshot(), "mmce-json-fixture").get(0);
        PonderStoryBoardEntry entry = new PonderStoryBoardEntry(definition.asStoryBoard(), "ponder_mmce", definition.getStructure(), definition.getComponent());
        entry.identifiedBy(definition.getSceneId());
        PonderScene jsonScene = new PonderSceneRegistry(new PonderLocalization()).compileEntry(entry);
        record(checks, ((PonderLevel) jsonScene.getWorld()).getStructureGroups().containsKey("mmce:controller"), "JSON and editor API compile MMCE structures");
        for (PonderScene scene : new PonderScene[] {fixedScene, dynamicScene, jsonScene}) {
            scene.begin(); scene.seek(scene.getTotalTicks()); scene.restart();
            record(checks, scene.getCurrentTick() == 0, "scene seek and replay " + scene.getId());
        }
    }

    static ItemStack blueprint(ResourceLocation machine) {
        ItemStack stack = new ItemStack(new ItemBlueprint());
        ItemBlueprint.setAssociatedMachine(stack, machine);
        return stack;
    }

    private static PonderScene scene(String component, String sceneId) {
        List<PonderScene> scenes = PonderIndex.getSceneAccess().compile(new ResourceLocation(component));
        if (scenes.size() != 1 || !new ResourceLocation(sceneId).equals(scenes.get(0).getId()))
            throw new IllegalStateException("ZenScript scene missing: " + sceneId);
        return scenes.get(0);
    }

    static void record(JsonArray checks, boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        JsonObject check = new JsonObject(); check.addProperty("check", message); check.addProperty("result", "PASS"); checks.add(check);
    }
}
