package net.createmod.ponder.foundation.ui;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Localized catalog labels, overridable by any resource pack without changing registry identities. */
@SideOnly(Side.CLIENT)
final class PonderModNames {
    private PonderModNames() {}

    static String localized(String namespace) {
        for (String key : new String[] {"ponder.mod_name." + namespace, "mod." + namespace + ".name"}) {
            if (!I18n.hasKey(key)) continue;
            String value = I18n.format(key).trim();
            if (!value.isEmpty() && !value.equals(key)) return value;
        }
        return original(namespace);
    }

    static String original(String namespace) {
        if ("minecraft".equals(namespace)) return "Minecraft";
        ModContainer mod = Loader.instance().getIndexedModList().get(namespace);
        String name = mod == null ? "" : mod.getName();
        return name == null || name.isBlank() ? namespace : name;
    }
}
