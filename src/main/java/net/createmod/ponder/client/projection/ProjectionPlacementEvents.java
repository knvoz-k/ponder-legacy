package net.createmod.ponder.client.projection;

import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.render.GlStateGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.input.Mouse;

/** Temporary world controls; no global key bindings or building packets are added. */
@SideOnly(Side.CLIENT)
public final class ProjectionPlacementEvents {
    private static int swallowedButtons;

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void mouse(MouseEvent event){
        if(handleMouse(event.getButton(),event.isButtonstate(),event.getDwheel(),GuiScreen.isShiftKeyDown()))event.setCanceled(true);
    }

    public static boolean handleMouse(int button,boolean down,int wheel,boolean shift){
        Minecraft mc=Minecraft.getMinecraft();if(mc.world==null||mc.currentScreen!=null)return false;
        int mask=button<0||button>2?0:1<<button;
        if(!down&&mask!=0&&(swallowedButtons&mask)!=0){swallowedButtons&=~mask;return true;}
        if(ProjectionService.placing()){
            if(wheel!=0){ProjectionService.scrollPlacement(wheel,shift);return true;}
            if(mask==0)return false;
            if(down){
                swallowedButtons|=mask;
                if(button==0){ProjectionService.updateAim();ProjectionService.confirmPlacement();}
                if(button==1)ProjectionService.rotatePlacement(shift);
            }
            releaseVanillaActions();return true;
        }
        if(button==2&&down&&shift&&ProjectionService.lookingAtProjection()){
            swallowedButtons|=mask;ProjectionService.move();releaseVanillaActions();return true;
        }
        return false;
    }

    public static void tick(){
        if(ProjectionService.placing())releaseVanillaActions();
        for(int button=0;button<3;button++)if(!Mouse.isButtonDown(button))swallowedButtons&=~(1<<button);
        if(swallowedButtons!=0)releaseVanillaActions();
    }

    private static void releaseVanillaActions(){
        var mc=Minecraft.getMinecraft();
        for(var binding:List.of(mc.gameSettings.keyBindAttack,mc.gameSettings.keyBindUseItem,mc.gameSettings.keyBindPickBlock))
            KeyBinding.setKeyBindState(binding.getKeyCode(),false);
        if(mc.playerController!=null)mc.playerController.resetBlockRemoving();
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void open(GuiOpenEvent event){
        if(ProjectionService.placing()&&event.getGui() instanceof GuiIngameMenu){ProjectionService.cancelPlacement();releaseVanillaActions();event.setCanceled(true);}
    }

    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event){
        Minecraft mc=Minecraft.getMinecraft();
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL||mc.currentScreen!=null||mc.gameSettings.hideGUI)return;
        if(!ProjectionService.placing()&&!ProjectionService.showLockedHint())return;
        int width=event.getResolution().getScaledWidth(),height=event.getResolution().getScaledHeight();
        var font=mc.fontRenderer;List<String> lines=new ArrayList<>();
        if(ProjectionService.placing()){
            var s=ProjectionService.current();
            lines.add(I18n.format("ponder.projection.placing",s.anchor.getX(),s.anchor.getY(),s.anchor.getZ(),Math.floorMod(s.turns,4)*90));
            lines.addAll(font.listFormattedStringToWidth(I18n.format("ponder.projection.world_controls"),width-24));
            lines.addAll(font.listFormattedStringToWidth(I18n.format("ponder.projection.world_scroll"),width-24));
        }else lines.addAll(font.listFormattedStringToWidth(I18n.format("ponder.projection.locked_hint"),width-24));
        int widest=lines.stream().mapToInt(font::getStringWidth).max().orElse(0),x=(width-widest)/2,y=height-64-lines.size()*11;
        try(GlStateGuard ignored=GlStateGuard.capture()){
            Gui.drawRect(x-6,y-5,x+widest+6,y+lines.size()*11+3,0xd414252d);
            for(String line:lines){font.drawStringWithShadow(line,(width-font.getStringWidth(line))/2f,y,0xffbde7dd);y+=11;}
        }
    }
}
