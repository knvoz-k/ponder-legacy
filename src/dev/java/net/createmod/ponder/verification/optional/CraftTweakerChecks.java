package net.createmod.ponder.verification.optional;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.script.ScriptInstructionCodec;
import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.script.PonderJsonLoader;
import net.createmod.ponder.script.ScriptSceneBuilder;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.createmod.ponder.script.ScriptWorldBuilder;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/** Shared physical-client/server checks, installed only in verification instances. */
public final class CraftTweakerChecks {
    private static final ResourceLocation CODEC = new ResourceLocation("ponder_crt_smoke:marker");

    private CraftTweakerChecks() {
    }

    public static void prepare() {
        if (System.getProperty("ponder.crafttweaker.smoke") == null) return;
        ScriptInstructionCodecs.register(new ScriptInstructionCodec() {
            @Override public ResourceLocation getId() { return CODEC; }
            @Override public void validate(NBTTagCompound data) {
                if (data.getInteger("ticks") != 3) throw new IllegalArgumentException("Expected fixture payload");
            }
            @Override public void program(NBTTagCompound data, SceneBuilder scene, SceneBuildingUtil util) {
                scene.idle(data.getInteger("ticks"));
            }
        });
    }

    public static List<String> verify() throws Exception {
        List<String> checks = new ArrayList<>();
        String mode = System.getProperty("ponder.crafttweaker.smoke");
        if (mode == null) return checks;
        boolean present = "present".equals(mode);
        check(checks, Loader.isModLoaded("crafttweaker") == present, "expected optional CraftTweaker installation: " + mode);
        // Enumerating all methods resolves signature types, catching accidental IData links.
        check(checks, Arrays.stream(ScriptSceneBuilder.class.getDeclaredMethods())
            .anyMatch(method -> method.getName().equals("custom")) == present,
            "CraftTweaker custom overload is retained only when installed");
        check(checks, Arrays.stream(ScriptWorldBuilder.class.getDeclaredMethods())
            .anyMatch(method -> method.getName().equals("modifyTileNBT")) == present,
            "CraftTweaker IData NBT overload is retained only when installed");

        var javaScene = ScriptSceneRegistry.create("minecraft:stick", "ponder_crt_smoke:java",
            "Java registration without CrT", "ponder:demo/basics");
        javaScene.showBasePlate().idle(5).markAsFinished().register();
        check(checks, ScriptSceneRegistry.localSnapshot(true).stream()
            .anyMatch(scene -> scene.getSceneId().toString().equals("ponder_crt_smoke:java")),
            "Java builder registers using optional source capture and logging");

        SceneDocument document = SceneDocument.create("ponder_crt_smoke:json", "minecraft:stick",
            "JSON registration without CrT", "ponder:demo/basics");
        JsonObject custom = new JsonObject();
        custom.addProperty("op", "scene.custom"); custom.addProperty("codec", CODEC.toString());
        custom.addProperty("payload", "{ticks:3}");
        document.insertStep(1, custom);
        Path path = ScenePackIO.resolveId(Path.of("scripts/ponder/packs"), "ponder_crt_smoke:json", ".ponder.json");
        ScenePackIO.save(path, document);
        check(checks, !document.isDirty() && ScenePackIO.read(path).steps().size() == 4,
            "JSON editor API saves and reads a custom instruction without IData");
        check(checks, PonderJsonLoader.reload().errors == 0, "JSON pack reload succeeds with optional CraftTweaker");
        PonderIndex.reload();
        var compiled = PonderIndex.getSceneAccess().compile(new ResourceLocation("minecraft:stick"));
        for (String id : new String[] {"ponder_crt_smoke:java", "ponder_crt_smoke:json"}) {
            PonderScene scene = compiled.stream().filter(value -> value.getId().toString().equals(id)).findFirst().orElseThrow();
            scene.begin(); scene.seek(scene.getTotalTicks()); scene.restart();
            check(checks, scene.getCurrentTick() == 0, "scene compiles, seeks and restarts: " + id);
        }
        boolean zsRegistered = ScriptSceneRegistry.localSnapshot(true).stream()
            .anyMatch(scene -> scene.getSceneId().toString().equals("ponder_crt_smoke:zenscript"));
        check(checks, zsRegistered == present, "ZenScript fixture executes only when CraftTweaker is installed");
        if (present) {
            PonderScene zs = compiled.stream().filter(scene -> scene.getId().toString()
                .equals("ponder_crt_smoke:zenscript")).findFirst().orElseThrow();
            zs.begin(); zs.seek(zs.getTotalTicks()); zs.restart();
            check(checks, zs.getCurrentTick() == 0, "real ZenScript IData conversion, custom codec and playback succeed");
        }
        var definitions = ScriptSceneRegistry.localSnapshot(false);
        var encoded = ScriptSceneSnapshot.encodeLocal(definitions);
        check(checks, ScriptSceneSnapshot.decodeContent(encoded.bytes, encoded.uncompressedBytes).scenes.size() == definitions.size(),
            "scene synchronization snapshot round trips with optional CraftTweaker");
        return checks;
    }

    private static void check(List<String> checks, boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        checks.add(message);
    }
}
