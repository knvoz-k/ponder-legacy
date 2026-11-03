package net.createmod.ponder.verification.mmce;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.structure.PonderStructureProviders;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Production-jar fixture. This class must also load with MMCE completely absent. */
@Mod(modid = "ponder_mmce_harness", name = "Ponder MMCE verification", version = "1",
    dependencies = "required-after:ponder-legacy;after:modularmachinery", acceptableRemoteVersions = "*")
public final class MMCESmoke {
    private final JsonObject report = new JsonObject();
    private final JsonArray checks = new JsonArray();
    private boolean finished;

    @Mod.EventHandler public void loadComplete(FMLLoadCompleteEvent event) throws Exception {
        String mode = System.getProperty("ponder.mmce.smoke", "absent");
        boolean supported = "present".equals(mode);
        report.addProperty("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
        report.addProperty("mode", mode);
        report.addProperty("side", event.getSide().toString());
        report.addProperty("mmce", Loader.isModLoaded("modularmachinery")
            ? Loader.instance().getIndexedModList().get("modularmachinery").getVersion() : "absent");
        report.add("checks", checks);
        try {
            check(Loader.isModLoaded("modularmachinery") == !"absent".equals(mode), "expected MMCE installation");
            long providers = PonderStructureProviders.snapshot().stream()
                .filter(p -> new ResourceLocation("ponder_mmce:mmce").equals(p.getId())).count();
            check(providers == (supported ? 1 : 0), "optional provider registered exactly once or disabled");
            check(PonderSubjectResolvers.getRegisteredResolverIds().contains(new ResourceLocation("ponder_mmce:blueprint"))
                == supported, "optional blueprint resolver enabled only for supported MMCE");
            // Re-enter the public lifecycle to catch duplicate registration and half-enabled state.
            net.createmod.ponder.compat.PonderOptionalCompat.preInit();
            net.createmod.ponder.compat.PonderOptionalCompat.postInit();
            check(PonderStructureProviders.snapshot().stream()
                .filter(p -> new ResourceLocation("ponder_mmce:mmce").equals(p.getId())).count() == providers,
                "optional lifecycle is idempotent");
            if (supported) MMCEIntegrationChecks.verify(checks);
            else check(PonderIndex.getSceneAccess().getRegisteredEntries().isEmpty(), "no default or MMCE scenes injected");
            write("RUNNING", null);
            MinecraftForge.EVENT_BUS.register(this);
            if (event.getSide().isClient()) MMCEClientSmoke.start(this, supported);
        } catch (Throwable failure) {
            write("FAIL", failure);
            throw new IllegalStateException("MMCE verification failed", failure);
        }
    }

    @Mod.EventHandler public void serverStarted(FMLServerStartedEvent event) throws Exception {
        if (FMLCommonHandler.instance().getSide().isClient()) return;
        write("PASS", null);
        finished = true;
    }

    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) {
        if (finished && event.phase == TickEvent.Phase.END)
            FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
    }

    void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        JsonObject check = new JsonObject(); check.addProperty("check", message); check.addProperty("result", "PASS"); checks.add(check);
    }

    void write(String result, Throwable failure) throws Exception {
        report.addProperty("result", result);
        if (failure != null) {
            report.addProperty("failure", failure.toString());
            Ponder.LOGGER.error("MMCE smoke failed", failure);
        }
        Files.writeString(Path.of("ponder-mmce-report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
