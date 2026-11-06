package net.createmod.ponder.client.projection;

import java.io.IOException;
import org.lwjgl.input.Mouse;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.*;

/** Small transparent toolbox; the world remains visible while changing optional settings. */
@SideOnly(Side.CLIENT)
public final class ProjectionScreen extends GuiScreen {
    private final ProjectionSession session;
    private final GuiScreen parent;
    private boolean advanced;
    private int panelWidth;
    public ProjectionScreen(ProjectionSession session,GuiScreen parent){this.session=session;this.parent=parent;}
    public int panelWidth(){return panelWidth;}
    @Override public void initGui(){
        buttonList.clear();panelWidth=Math.min(174,Math.max(112,width/3));int x=12,w=panelWidth-16,half=(w-4)/2;
        button(x,33,w,"ponder.projection.move",ProjectionService::move);
        addButton(new PonderButton(2,x,57,half,20,null).withLabel("−90°").withCallback(()->{session.turns--;session.rebuild();}));
        addButton(new PonderButton(3,x+half+4,57,half,20,null).withLabel("+90°").withCallback(()->{session.turns++;session.rebuild();}));
        toggle(x,81,half,"ponder.projection.mirror",session.mirror,()->{session.mirror=!session.mirror;session.rebuild();});
        toggle(x+half+4,81,half,"ponder.projection.base_short",session.includeBase,()->{session.includeBase=!session.includeBase;session.rebuild();}).enabled(session.plan.hasBase());
        toggle(x,105,half,"ponder.projection.pin",session.pinned,()->session.pinned=!session.pinned);
        button(x+half+4,105,half,advanced?"ponder.projection.less":"ponder.projection.more",()->{advanced=!advanced;initGui();});
        if(advanced){
            toggle(x,129,half,"ponder.projection.hide_done",session.hideCorrect,()->{session.hideCorrect=!session.hideCorrect;session.refresh();});
            addButton(new PonderButton(buttonList.size()+1,x+half+4,129,half,20,null).withLabel(I18n.format("ponder.projection.layer")+": "+(session.layer<0?"*":session.layer)).withCallback(()->{session.layer=session.layer>=session.maxLayer()?-1:session.layer+1;session.refresh();initGui();}));
            button(x,153,w,"ponder.projection.layout",()->{session.pinned=true;mc.displayGuiScreen(new ProjectionLayoutScreen(this));});
            toggle(x,177,half,"ponder.projection.visible",session.projecting,()->{session.projecting=!session.projecting;session.refresh();});
            button(x+half+4,177,half,"ponder.projection.reset_short",()->{ProjectionOverlay.INSTANCE.reset(false);ProjectionOverlay.INSTANCE.reset(true);});
        }
        int bottom=height-29;
        button(x,bottom,half,"ponder.guide.back",()->mc.displayGuiScreen(parent));
        button(x+half+4,bottom,half,"ponder.projection.clear_short",()->{ProjectionService.clear();mc.displayGuiScreen(null);});
    }
    private void button(int x,int y,int w,String key,Runnable action){addButton(new PonderButton(buttonList.size()+1,x,y,w,20,null).withLabel(I18n.format(key)).withTooltip(key).withCallback(action));}
    private PonderButton toggle(int x,int y,int w,String key,boolean active,Runnable action){
        return addButton(new PonderButton(buttonList.size()+1,x,y,w,20,null).withLabel(I18n.format(key)).withTooltip(key).active(active).withCallback(()->{action.run();initGui();}));
    }
    @Override public void drawScreen(int mx,int my,float partial){
        if(ProjectionService.current()!=session||!session.validDimension()){mc.displayGuiScreen(null);return;}
        Gui.drawRect(6,6,panelWidth+6,height-6,0xcf14212b);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(session.plan.title(),panelWidth-16),12,15,0xffd2e9e4);
        int y=advanced?205:139;
        String pos=session.anchor.getX()+", "+session.anchor.getY()+", "+session.anchor.getZ();
        if(y<height-60)fontRenderer.drawString(fontRenderer.trimStringToWidth(pos,panelWidth-16),12,y,0xffa5c7cf);
        ProjectionOverlay.INSTANCE.draw(this,width,height,mx,my);
        super.drawScreen(mx,my,partial);
        for(var b:buttonList)if(b instanceof PonderButton p&&p.isHoveredButton()&&p.getTooltipKey()!=null)drawHoveringText(I18n.format(p.getTooltipKey()),mx,my);
    }
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{if(!ProjectionOverlay.INSTANCE.mouseDown(this,mx,my,button))super.mouseClicked(mx,my,button);}
    @Override protected void mouseClickMove(int mx,int my,int button,long elapsed){ProjectionOverlay.INSTANCE.drag(this,mx,my);}
    @Override protected void mouseReleased(int mx,int my,int button){ProjectionOverlay.INSTANCE.release(this);super.mouseReleased(mx,my,button);}
    @Override public void onGuiClosed(){ProjectionOverlay.INSTANCE.release(this);}
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0)ProjectionOverlay.INSTANCE.scroll(wheel);}
    @Override protected void keyTyped(char character,int key)throws IOException{if(key==org.lwjgl.input.Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);else super.keyTyped(character,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
