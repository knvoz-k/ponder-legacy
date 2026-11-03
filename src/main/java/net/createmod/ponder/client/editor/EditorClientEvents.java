package net.createmod.ponder.client.editor;

import org.lwjgl.input.Keyboard;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class EditorClientEvents {
    private static final KeyBinding editor = new KeyBinding("ponder.editor.key_open", Keyboard.KEY_F7, "Ponder");
    private final KeyBinding selection = new KeyBinding("ponder.editor.key_select", Keyboard.KEY_F8, "Ponder");

    public EditorClientEvents() {
        ClientRegistry.registerKeyBinding(editor); ClientRegistry.registerKeyBinding(selection);
    }
    public static boolean isEditorKey(int key) { return key == editor.getKeyCode(); }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        while (editor.isPressed()) if (mc.currentScreen == null) EditorWorkspace.open();
        while (selection.isPressed()) if (mc.currentScreen == null && mc.world != null) EditorWorkspace.toggleSelection();
        EditorWorkspace.tick();
    }

    @SubscribeEvent public void mouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!EditorWorkspace.selecting() || mc.currentScreen != null || mc.world == null) return;
        if (event.getButton() != 0 && event.getButton() != 1) return;
        event.setCanceled(true);
        if (!event.isButtonstate()) return;
        RayTraceResult hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK)
            EditorWorkspace.selectCorner(hit.getBlockPos(), event.getButton() == 1);
    }

    @SubscribeEvent public void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        CuboidSelection selection = EditorWorkspace.worldSelection();
        if (!EditorWorkspace.selecting() || selection == null || mc.getRenderViewEntity() == null) return;
        net.minecraft.entity.Entity camera = mc.getRenderViewEntity();
        float pt = event.getPartialTicks();
        Vec3d position = new Vec3d(camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * pt,
            camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * pt,
            camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * pt);
        AABBOutline outline = new AABBOutline(new AxisAlignedBB(selection.min(), selection.max().add(1, 1, 1)));
        outline.getParams().colored(0x72e3bd).lineWidth(.05f);
        outline.render(position, pt);
    }

    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || !EditorWorkspace.selecting()) return;
        Minecraft mc = Minecraft.getMinecraft();
        CuboidSelection selection = EditorWorkspace.worldSelection();
        mc.fontRenderer.drawStringWithShadow(EditorWidgets.tr("selection_hint"), 8, 8, EditorWidgets.ACCENT);
        if (selection != null) mc.fontRenderer.drawStringWithShadow(selection + " (" + selection.volume() + ")", 8, 21, EditorWidgets.TEXT);
    }

    @SubscribeEvent public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(EditorWorkspace::disconnected);
    }
}
