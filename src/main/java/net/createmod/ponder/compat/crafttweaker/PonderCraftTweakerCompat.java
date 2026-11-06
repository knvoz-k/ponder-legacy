package net.createmod.ponder.compat.crafttweaker;

import net.createmod.ponder.Ponder;
import net.minecraftforge.fml.common.Loader;

/** Optional script diagnostics; core registration never links to CraftTweaker. */
public final class PonderCraftTweakerCompat {
    private static boolean enabled;

    private PonderCraftTweakerCompat() {
    }

    public static void initialize() {
        enabled = Loader.isModLoaded("crafttweaker");
    }

    public static String captureSource() {
        return enabled ? Bridge.captureSource() : null;
    }

    public static void logInfo(String message) {
        if (enabled) Bridge.logInfo(message);
        else Ponder.LOGGER.info(message);
    }

    public static void logError(String message) {
        if (enabled) Bridge.logError(message);
        else Ponder.LOGGER.error(message);
    }

    /** Loaded only after the optional mod has been detected in pre-init. */
    private static final class Bridge {
        private static String captureSource() {
            try {
                return crafttweaker.CraftTweakerAPI.getScriptFileAndLine();
            } catch (RuntimeException failure) {
                // Java addon registration has no active ZenScript source location.
                return null;
            }
        }

        private static void logInfo(String message) {
            crafttweaker.CraftTweakerAPI.logInfo(message);
        }

        private static void logError(String message) {
            crafttweaker.CraftTweakerAPI.logError(message);
        }
    }
}
