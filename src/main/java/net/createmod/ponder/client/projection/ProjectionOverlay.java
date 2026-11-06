package net.createmod.ponder.client.projection;

import java.util.*;
import net.createmod.catnip.config.*;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.config.CClient;
import net.createmod.ponder.client.projection.MaterialPanelLayout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.input.Mouse;

/** Live targets render before container controls and tooltips, with saved HUD/inventory positions. */
@SideOnly(Side.CLIENT)
public final class ProjectionOverlay {
    public static final ProjectionOverlay INSTANCE = new ProjectionOverlay();
    private int page, rows, pages, dragX, dragY;
    private Rect bounds = new Rect(0,0,0,0);
    private GuiScreen dragging;
    private int preferredX, preferredY;
    private static CClient config(){return (CClient)ConfigRegistry.get(Ponder.MOD_ID,ConfigType.CLIENT);}
    private static boolean visible(){return ProjectionService.current()!=null&&ProjectionService.current().pinned;}
    public Rect bounds(){return bounds;}
    @SubscribeEvent public void hud(RenderGameOverlayEvent.Post event){
        Minecraft mc=Minecraft.getMinecraft();
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL||mc.currentScreen!=null||mc.gameSettings.hideGUI)return;
        draw(null,event.getResolution().getScaledWidth(),event.getResolution().getScaledHeight(),-1,-1);
    }
    // Container contents and item tooltips are drawn later and remain above the targets.
    @SubscribeEvent public void inventory(GuiScreenEvent.BackgroundDrawnEvent event){
        if(!(event.getGui() instanceof GuiContainer gui))return;
        Minecraft mc=Minecraft.getMinecraft();
        draw(gui,gui.width,gui.height,Mouse.getX()*gui.width/mc.displayWidth,gui.height-Mouse.getY()*gui.height/mc.displayHeight-1);
    }
    @SubscribeEvent public void tooltip(GuiScreenEvent.DrawScreenEvent.Post event){
        if(event.getGui() instanceof GuiContainer&&visible()&&dragging==null)tooltip(event.getGui(),event.getMouseX(),event.getMouseY());
    }
    public void draw(GuiScreen screen,int sw,int sh,int mx,int my){
        if(!visible()){bounds=new Rect(0,0,0,0);return;}
        ProjectionSession s=ProjectionService.current();Minecraft mc=Minecraft.getMinecraft();var font=mc.fontRenderer;
        int width=Math.min(146,sw-8),unknown=s.unknown.isEmpty()?0:12;
        int wanted=27+Math.min(8,s.rows.size())*21+(s.rows.size()>8?15:0)+unknown;
        boolean inventory=screen instanceof GuiContainer;
        CClient c=config();int nx=c==null?-1:(inventory?c.materialInventoryX:c.materialHudX).get(),ny=c==null?-1:(inventory?c.materialInventoryY:c.materialHudY).get();
        int px=nx<0?(inventory?4:sw-width-8):MaterialPanelLayout.restore(nx,sw,width);
        int py=ny<0?8:MaterialPanelLayout.restore(ny,sh,wanted);
        if(dragging==screen&&dragging!=null){px=preferredX;py=preferredY;}
        Rect area=MaterialPanelLayout.place(sw,sh,width,wanted,px,py,obstacles(screen,sw,sh));
        if(area.height()==0){bounds=area;return;}
        rows=Math.min(8,Math.max(0,(area.height()-27-unknown)/21));
        if(s.rows.size()>rows)rows=Math.min(8,Math.max(0,(area.height()-42-unknown)/21));
        pages=Math.max(1,(s.rows.size()+Math.max(1,rows)-1)/Math.max(1,rows));page=Math.min(page,pages-1);
        int count=Math.min(rows,Math.max(0,s.rows.size()-page*Math.max(1,rows)));
        int height=rows==0?Math.min(area.height(),27+unknown):27+count*21+(pages>1?15:0)+unknown;
        bounds=new Rect(area.x(),area.y(),area.width(),height);
        int x=bounds.x(),y=bounds.y(),w=bounds.width();
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GlStateManager.disableDepth();GlStateManager.disableLighting();
            Gui.drawRect(x,y,x+w,y+height,0xee19252e);Gui.drawRect(x,y,x+2,y+height,0xff9fd6d2);
            boolean hover=bounds.contains(mx,my)&&my<y+18;
            Gui.drawRect(x+2,y,x+w,y+18,hover||dragging==screen&&dragging!=null?0xff38535e:0xff293e49);
            for(int i=0;i<3;i++){Gui.drawRect(x+5+i*3,y+5,x+6+i*3,y+7,0xffa8c9cd);Gui.drawRect(x+5+i*3,y+10,x+6+i*3,y+12,0xffa8c9cd);}
            String state=s.projecting?s.complete+"/"+s.total:"";
            font.drawString(font.trimStringToWidth(I18n.format("ponder.projection.materials"),Math.max(20,w-24-font.getStringWidth(state))),x+17,y+5,0xffb4e5d9);
            font.drawString(state,x+w-font.getStringWidth(state)-5,y+5,0xffa5c2d0);
            for(int i=0;i<count;i++){
                var row=s.rows.get(page*Math.max(1,rows)+i);int ry=y+22+i*21;var stack=row.target().stack();
                RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(stack,x+5,ry);RenderHelper.disableStandardItemLighting();GlStateManager.disableDepth();
                font.drawString(font.trimStringToWidth(stack.getDisplayName(),Math.max(20,w-29)),x+25,ry,0xffd1e1e8);
                font.drawString(row.available()+" / "+row.target().count(),x+25,ry+10,row.missing()==0?0xff95e2bb:0xffe8bd8d);
            }
            if(pages>1&&rows>0)font.drawString("‹   "+(page+1)+" / "+pages+"   ›",x+6,y+22+count*21,0xffa9d5d1);
            if(!s.unknown.isEmpty()&&height>=39)font.drawString(font.trimStringToWidth(I18n.format("ponder.projection.unknown",s.unknown.size()),w-8),x+5,y+height-11,0xfff0ad9b);
            if(rows==0&&!s.rows.isEmpty()&&height>=27)font.drawString(font.trimStringToWidth(I18n.format("ponder.projection.missing",s.rows.stream().mapToInt(ProjectionSession.Row::missing).sum()),w-10),x+5,y+18,0xffe8bd8d);
            if(s.rows.isEmpty()&&height>=27)font.drawString(I18n.format("ponder.projection.complete"),x+5,y+18,0xff95e2bb);
        }
    }
    private List<Rect> obstacles(GuiScreen screen,int sw,int sh){
        List<Rect> result=new ArrayList<>();Minecraft mc=Minecraft.getMinecraft();
        if(screen instanceof GuiContainer gui){
            int left=ReflectionHelper.getPrivateValue(GuiContainer.class,gui,"guiLeft","field_147003_i"),top=ReflectionHelper.getPrivateValue(GuiContainer.class,gui,"guiTop","field_147009_r");
            int width=ReflectionHelper.getPrivateValue(GuiContainer.class,gui,"xSize","field_146999_f"),height=ReflectionHelper.getPrivateValue(GuiContainer.class,gui,"ySize","field_147000_g");
            result.add(new Rect(left-4,top-4,width+8,height+8));
            if(gui instanceof IRecipeShownListener recipes&&recipes.func_194310_f().isVisible())result.add(new Rect(left-154,top-4,154,height+8));
            List<GuiButton> buttons=ReflectionHelper.getPrivateValue(GuiScreen.class,screen,"buttonList","field_146292_n");
            for(GuiButton b:buttons)if(b.visible)result.add(new Rect(b.x-2,b.y-2,b.width+4,b.height+4));
            if(mc.player!=null&&!mc.player.getActivePotionEffects().isEmpty())result.add(new Rect(left-126,top,122,Math.min(height,mc.player.getActivePotionEffects().size()*33)));
        }else{
            result.add(new Rect(sw/2-96,sh-62,192,62));
            result.add(new Rect(sw/2-32,sh/2-24,64,48));
            result.add(new Rect(sw/2-100,0,200,52));
            if(mc.player!=null&&!mc.player.getActivePotionEffects().isEmpty())result.add(new Rect(sw-156,0,156,54));
            if(mc.world!=null){
                var board=mc.world.getScoreboard();var objective=board.getObjectiveInDisplaySlot(1);
                if(objective!=null){int width=mc.fontRenderer.getStringWidth(objective.getDisplayName())+12,lines=0;
                    for(var score:board.getSortedScores(objective)){width=Math.max(width,mc.fontRenderer.getStringWidth(score.getPlayerName())+30);lines++;}
                    int height=Math.min(15,lines)*9+18;result.add(new Rect(sw-width-4,sh/2-height/3-9,width+4,height));
                }
            }
            if(screen instanceof ProjectionLayoutScreen)result.add(new Rect(0,sh-56,sw,56));
        }
        return result;
    }
    private void tooltip(GuiScreen gui,int mx,int my){
        if(!bounds.contains(mx,my))return;var s=ProjectionService.current();List<String> lines=new ArrayList<>();
        if(my<bounds.y()+18)lines.add(I18n.format("ponder.projection.drag_hint"));
        else if(rows==0)for(var row:s.rows.stream().limit(12).toList())lines.add(row.target().stack().getDisplayName()+"  "+row.available()+" / "+row.target().count());
        else{int index=(my-bounds.y()-22)/21;if(my>=bounds.y()+22&&index<rows&&page*rows+index<s.rows.size()){var row=s.rows.get(page*rows+index);lines.add(row.target().stack().getDisplayName());lines.add(I18n.format("ponder.projection.missing",row.missing()));}}
        if(!lines.isEmpty())net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(lines,mx,my,gui.width,gui.height,-1,Minecraft.getMinecraft().fontRenderer);
    }
    public boolean mouseDown(GuiScreen gui,int mx,int my,int button){
        if(!visible()||button!=0||!bounds.contains(mx,my))return false;
        if(GuiScreen.isShiftKeyDown()){Minecraft.getMinecraft().displayGuiScreen(new ProjectionScreen(ProjectionService.current(),gui));return true;}
        if(my<bounds.y()+18){dragging=gui;dragX=mx-bounds.x();dragY=my-bounds.y();preferredX=bounds.x();preferredY=bounds.y();}
        else if(rows>0&&pages>1)page=Math.max(0,Math.min(pages-1,page+(mx<bounds.x()+bounds.width()/2?-1:1)));
        else if(rows==0)Minecraft.getMinecraft().displayGuiScreen(new ProjectionScreen(ProjectionService.current(),gui));
        return true;
    }
    public void drag(GuiScreen gui,int mx,int my){if(dragging==gui){preferredX=mx-dragX;preferredY=my-dragY;}}
    public void release(GuiScreen gui){
        if(dragging!=gui||dragging==null)return;CClient c=config();
        if(c!=null){boolean inventory=gui instanceof GuiContainer;(inventory?c.materialInventoryX:c.materialHudX).set(MaterialPanelLayout.normalize(bounds.x(),gui.width,bounds.width()));(inventory?c.materialInventoryY:c.materialHudY).set(MaterialPanelLayout.normalize(bounds.y(),gui.height,bounds.height()));}
        dragging=null;
    }
    public void reset(boolean inventory){CClient c=config();if(c!=null){(inventory?c.materialInventoryX:c.materialHudX).set(-1);(inventory?c.materialInventoryY:c.materialHudY).set(-1);}page=0;}
    public void scroll(int wheel){page=Math.max(0,Math.min(pages-1,page-Integer.signum(wheel)));}
    @SubscribeEvent public void mouse(GuiScreenEvent.MouseInputEvent.Pre event){
        if(!(event.getGui() instanceof GuiContainer gui)||!visible())return;
        Minecraft mc=Minecraft.getMinecraft();int mx=Mouse.getEventX()*gui.width/mc.displayWidth,my=gui.height-Mouse.getEventY()*gui.height/mc.displayHeight-1;
        if(dragging!=null&&dragging!=gui)dragging=null;
        if(dragging==gui){drag(gui,mx,my);if(Mouse.getEventButton()==0&&!Mouse.getEventButtonState())release(gui);event.setCanceled(true);return;}
        if(!bounds.contains(mx,my))return;
        int wheel=Mouse.getEventDWheel();if(wheel!=0){scroll(wheel);event.setCanceled(true);return;}
        if(Mouse.getEventButtonState()&&mouseDown(gui,mx,my,Mouse.getEventButton()))event.setCanceled(true);
    }
}
