package net.createmod.ponder.compat.revoui;

import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

/** Optional mod targets are queued only after Forge has discovered their jars. */
public final class PonderRevoMixinLoader implements ILateMixinLoader {
    @Override public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.ponder.revo_ui.json");
    }

    @Override public boolean shouldMixinConfigQueue(Context context) {
        return FMLLaunchHandler.side().isClient() && context.isModPresent("neofontrender_ui_enhancements");
    }
}
