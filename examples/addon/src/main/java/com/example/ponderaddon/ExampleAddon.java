package com.example.ponderaddon;

import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Host mod demonstrating codec, ServiceLoader and Forge IMC registration. */
@Mod(modid = ExampleAddon.MOD_ID, name = "Ponder Example Addon", version = "1.4.0-cleanroom-alpha.1",
    dependencies = "required-after:forge@[14.23.5.2847,);"
        + "required-after:ponder-legacy@[1.4.0-cleanroom-alpha.1,)")
public final class ExampleAddon {
    public static final String MOD_ID = "ponder_example";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ScriptInstructionCodecs.register(new ExamplePulseCodec());
        com.google.gson.JsonObject action = new com.google.gson.JsonObject();
        action.addProperty("op", "scene.custom");
        action.addProperty("codec", ExamplePulseCodec.ID.toString());
        action.addProperty("payload", "{duration:20,x:2,y:1,z:2,color:\"blue\"}");
        net.createmod.ponder.api.editor.EditorActions.register("ponder_example:pulse", "Pulse", action);
        FMLInterModComms.sendMessage("ponder-legacy", "register_ponder_plugin",
            ExampleImcPonderPlugin.class.getName());
    }
}
