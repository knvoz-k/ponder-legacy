package net.createmod.ponder.mmce;

import net.createmod.ponder.api.structure.PonderStructureProviders;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.mmce.structure.MMCEStructureProvider;
import net.createmod.ponder.mmce.subject.MMCEBlueprintResolver;
import net.createmod.ponder.mmce.script.MMCEStructures;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

/**
 * Lifecycle bridge shared by the integrated Ponder jar and the legacy addon.
 * Its public signature intentionally contains no MMCE or Forge event types.
 */
public final class PonderMMCEEntrypoint {
    private static boolean compatibilityEnabled;

    private PonderMMCEEntrypoint() {
    }

    public static synchronized void preInit() {
        if (compatibilityEnabled || !MMCECompatibility.isSupported()) return;
        if (Loader.isModLoaded("crafttweaker")) registerScriptClasses();
        PonderStructureProviders.register(MMCEStructureProvider.INSTANCE);
        PonderSubjectResolvers.register(PonderMMCE.BLUEPRINT_RESOLVER_ID,
            MMCEBlueprintResolver.PRIORITY, MMCEBlueprintResolver.INSTANCE);
        compatibilityEnabled = true;
        PonderMMCE.LOGGER.info("Enabled optional MMCE structure provider and blueprint resolver");
    }

    @Optional.Method(modid = "crafttweaker")
    private static void registerScriptClasses() {
        // Register only after both optional dependencies passed their presence / ABI checks.
        crafttweaker.CraftTweakerAPI.registerClass(MMCEStructureRef.class);
        crafttweaker.CraftTweakerAPI.registerClass(MMCEStructures.class);
    }

    public static synchronized void postInit() {
        if (compatibilityEnabled) MMCEStructureProvider.INSTANCE.invalidate();
    }
}
