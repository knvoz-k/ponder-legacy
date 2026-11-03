package net.createmod.ponder.verification.mmce;

import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class MMCEClientSmoke {
    private final MMCESmoke owner;
    private final boolean supported;
    private int ticks, phase, next;
    private String screenshot;

    private MMCEClientSmoke(MMCESmoke owner, boolean supported) { this.owner = owner; this.supported = supported; }
    static void start(MMCESmoke owner, boolean supported) { MinecraftForge.EVENT_BUS.register(new MMCEClientSmoke(owner, supported)); }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (++ticks < next || screenshot != null) return;
        try {
            if (ticks > 600) throw new IllegalStateException("MMCE UI timeout");
            if (phase == 0) {
                if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                owner.check(true, "client reached main menu");
                if (!supported) { owner.write("PASS", null); mc.shutdown(); phase = 4; return; }
                mc.gameSettings.guiScale = 2;
                mc.gameSettings.renderDistanceChunks = 2;
                mc.gameSettings.pauseOnLostFocus = false;
                net.minecraft.world.WorldSettings settings = new net.minecraft.world.WorldSettings(1729,
                    net.minecraft.world.GameType.CREATIVE, false, false, net.minecraft.world.WorldType.FLAT);
                settings.setGeneratorOptions("3;minecraft:air;1;");
                settings.enableCommands();
                mc.launchIntegratedServer("ponder-mmce-smoke", "MMCE verification", settings);
                phase = 10;
            } else if (phase == 10) {
                if (mc.world == null || mc.player == null) return;
                mc.displayGuiScreen(PonderUI.of(MMCEIntegrationChecks.blueprint(MMCEIntegrationChecks.STATIC)));
                owner.check(mc.currentScreen instanceof PonderUI, "static blueprint opens Ponder UI");
                phase = 1; next = ticks + 25;
            } else if (phase == 1) {
                screenshot = "mmce-static.png"; phase = 2; next = ticks + 2;
            } else if (phase == 2) {
                mc.displayGuiScreen(PonderUI.of(MMCEIntegrationChecks.blueprint(MMCEIntegrationChecks.DYNAMIC)));
                owner.check(mc.currentScreen instanceof PonderUI, "dynamic blueprint opens Ponder UI");
                phase = 3; next = ticks + 25;
            } else if (phase == 3) {
                screenshot = "mmce-dynamic.png"; phase = 4; next = ticks + 2;
            } else {
                owner.write("PASS", null); mc.shutdown();
            }
        } catch (Throwable failure) { owner.write("FAIL", failure); mc.shutdown(); }
    }

    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || screenshot == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(mc.gameDir, screenshot, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        owner.check(new java.io.File(mc.gameDir, "screenshots/" + screenshot).isFile(), "saved " + screenshot);
        screenshot = null;
    }
}
