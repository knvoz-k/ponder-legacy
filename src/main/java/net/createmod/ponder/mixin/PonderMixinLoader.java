package net.createmod.ponder.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.io.File;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.script.BuiltinScriptInstaller;
import net.createmod.ponder.script.PonderJsonLoader;
import zone.rong.mixinbooter.IEarlyMixinLoader;

/** Registers Ponder's client hooks after MixinBooter has placed this jar on the launch classpath. */
@IFMLLoadingPlugin.Name("PonderMixinLoader")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(0)
@IFMLLoadingPlugin.TransformerExclusions({"net.createmod.ponder.mixin.PonderMixinLoader", "net.createmod.ponder.compat.cleanroom."})
public final class PonderMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.ponder.json");
    }

    @Override public String[] getASMTransformerClass() { return new net.createmod.ponder.compat.cleanroom.Cleanroom040ServerPlugin().getASMTransformerClass(); }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override
    public void injectData(Map<String, Object> data) {
        // Register the exact-version server transformer before Loader is resolved.
        // Minecraft-backed directory initialization runs in normal mod preInit,
        // after FML has installed the deobfuscating transformer.
    }
    @Override public String getAccessTransformerClass() { return null; }
}
