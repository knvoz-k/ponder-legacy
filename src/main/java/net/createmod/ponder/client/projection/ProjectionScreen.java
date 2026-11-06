package net.createmod.ponder.client.projection;

import java.io.IOException;
import org.lwjgl.input.Mouse;
import net.createmod.ponder.foundation.ui.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public final class ProjectionScreen extends AbstractPonderScreen {
    private final ProjectionSession session;
    private final GuiScreen parent;
    private int scroll;
    public ProjectionScreen(ProjectionSession session,GuiScreen parent){this.session=session;this.parent=parent;}
    @Override public void initGui(){buttonList.clear();int w=Math.min(420,width-16),x=(width-w)/2,bw=(w-12)/4;
        button(x,42,bw,"−90°",()->{session.turns--;session.rebuild();});button(x+bw+4,42,bw,"+90°",()->{session.turns++;session.rebuild();});
        button(x+2*(bw+4),42,bw,I18n.format("ponder.projection.mirror")+(session.mirror?" ✓":""),()->{session.mirror=!session.mirror;session.rebuild();initGui();});
        button(x+3*(bw+4),42,bw,I18n.format("ponder.projection.here"),()->{session.anchor=ProjectionService.target();session.refresh();});
        int y=68;
        for(int axis=0;axis<3;axis++){final int a=axis;String name=new String[]{"X","Y","Z"}[axis];int bx=x+axis*(w/3);button(bx,y,40,name+" −",()->move(a,-1));button(bx+44,y,40,name+" +",()->move(a,1));}
        button(x,94,bw,I18n.format("ponder.projection.base")+(session.includeBase?" ✓":""),()->{session.includeBase=!session.includeBase;session.rebuild();initGui();});
        button(x+bw+4,94,bw,I18n.format("ponder.projection.layer")+": "+(session.layer<0?"*":session.layer),()->{session.layer=session.layer>=session.maxLayer()?-1:session.layer+1;session.refresh();initGui();});
        button(x+2*(bw+4),94,bw,I18n.format("ponder.projection.hide_done")+(session.hideCorrect?" ✓":""),()->{session.hideCorrect=!session.hideCorrect;session.refresh();initGui();});
        button(x+3*(bw+4),94,bw,I18n.format("ponder.projection.pin")+(session.pinned?" ✓":""),()->{session.pinned=!session.pinned;initGui();});
        button(x,118,w/2-2,I18n.format("ponder.projection.layout"),()->{session.pinned=true;mc.displayGuiScreen(new ProjectionLayoutScreen(this));});
        button(x+w/2+2,118,w/2-2,I18n.format("ponder.projection.reset_position"),()->{ProjectionOverlay.INSTANCE.reset(false);ProjectionOverlay.INSTANCE.reset(true);});
        button(x,height-29,bw,I18n.format("ponder.projection.project"),()->{session.projecting=true;session.refresh();mc.displayGuiScreen(null);});
        button(x+bw+4,height-29,bw,I18n.format("ponder.projection.stop"),()->{session.projecting=false;session.refresh();initGui();});
        button(x+2*(bw+4),height-29,bw,I18n.format("ponder.guide.back"),()->mc.displayGuiScreen(parent));
        button(x+3*(bw+4),height-29,bw,I18n.format("ponder.projection.clear"),()->{ProjectionService.clear();mc.displayGuiScreen(parent);});
    }
    private void button(int x,int y,int w,String title,Runnable action){addButton(new PonderButton(buttonList.size()+1,x,y,w,20,null).withLabel(title).withCallback(action));}
    private void move(int axis,int amount){session.anchor=session.anchor.add(axis==0?amount:0,axis==1?amount:0,axis==2?amount:0);session.refresh();}
    @Override protected void renderWindow(int mx,int my,float partial){
        fontRenderer.drawString(fontRenderer.trimStringToWidth(session.plan.title(),width-24),12,13,0xffd2e9e4);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(I18n.format("ponder.projection.position",session.anchor.getX(),session.anchor.getY(),session.anchor.getZ(),Math.floorMod(session.turns,4)*90),width-24),12,146,0xffa5c7cf);
        int y=164;int count=Math.max(0,(height-209)/18);scroll=Math.max(0,Math.min(scroll,Math.max(0,session.rows.size()-count)));
        for(int i=scroll;i<Math.min(session.rows.size(),scroll+count);i++){
            var row=session.rows.get(i);String text=row.target().stack().getDisplayName();String amount=row.available()+" / "+row.target().count()+"  "+I18n.format("ponder.projection.missing",row.missing());
            fontRenderer.drawString(fontRenderer.trimStringToWidth(text,Math.max(20,width-180)),14,y,0xffd6e0e5);fontRenderer.drawString(amount,width-fontRenderer.getStringWidth(amount)-14,y,row.missing()==0?0xff9fe2c3:0xffedc995);y+=18;
        }
        String note=session.unknown.isEmpty()?I18n.format("ponder.projection.inventory_note"):I18n.format("ponder.projection.unknown",session.unknown.size());
        fontRenderer.drawString(fontRenderer.trimStringToWidth(note,width-24),12,height-43,session.unknown.isEmpty()?0xff91abb7:0xfff0a79c);
    }
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0)scroll-=Integer.signum(wheel)*2;}
}
