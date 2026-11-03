package net.createmod.ponder.compat.cleanroom;

import com.cleanroommc.common.CleanroomVersion;
import java.util.Map;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/** CR 0.4.0 and 0.4.0-alpha call the client-only Kirino entry point during server discovery. */
@IFMLLoadingPlugin.Name("PonderCleanroom040ServerCompatibility")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("net.createmod.ponder.compat.cleanroom.")
public final class Cleanroom040ServerPlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() {
        // Call the accessor: reading the constant VERSION would inline the compilation SDK's version.
        String version = CleanroomVersion.getVersion();
        return FMLLaunchHandler.side().isServer() && ("0.4.0".equals(version) || "0.4.0-alpha".equals(version))
            ? new String[]{Cleanroom040ServerTransformer.class.getName()} : new String[0];
    }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getAccessTransformerClass() { return null; }
}
