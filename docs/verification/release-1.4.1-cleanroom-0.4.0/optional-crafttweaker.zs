import mods.ponder.SceneRegistry;
import mods.ponder.Selection;

val scene = SceneRegistry.create("minecraft:stick", "ponder_crt_smoke:zenscript",
    "Optional CraftTweaker fixture", "ponder:demo/basics");
scene.showBasePlate();
scene.world.showSection(Selection.everywhere(), "down");
scene.world.modifyTileNBT(Selection.position(3, 1, 2), {Lock: "optional_crt"}, false, false);
scene.custom("ponder_crt_smoke:marker", {ticks: 3});
scene.idle(5);
scene.markAsFinished();
scene.register();
