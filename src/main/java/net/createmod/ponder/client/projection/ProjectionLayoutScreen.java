package net.createmod.ponder.client.projection;

import java.io.IOException;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.input.Mouse;

/** A cursor over the live world for moving the HUD panel without an extra global shortcut. */
@SideOnly(Side.CLIENT)
public final class ProjectionLayoutScreen extends GuiScreen {
    private final GuiScreen parent;
    public ProjectionLayoutScreen(GuiScreen parent){this.parent=parent;}
    @Override public void initGui(){buttonList.clear();
        addButton(new PonderButton(1,width/2-106,height-29,102,20,null).withLabel(I18n.format("ponder.projection.reset_position")).withCallback(()->ProjectionOverlay.INSTANCE.reset(false)));
        addButton(new PonderButton(2,width/2+4,height-29,102,20,null).withLabel(I18n.format("ponder.guide.back")).withCallback(()->mc.displayGuiScreen(parent)));
    }
    @Override public void drawScreen(int mx,int my,float partial){
        ProjectionOverlay.INSTANCE.draw(this,width,height,mx,my);
        drawCenteredString(fontRenderer,I18n.format("ponder.projection.layout_hint"),width/2,height-43,0xffd4ece6);
        super.drawScreen(mx,my,partial);
    }
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{if(!ProjectionOverlay.INSTANCE.mouseDown(this,mx,my,button))super.mouseClicked(mx,my,button);}
    @Override protected void mouseClickMove(int mx,int my,int button,long elapsed){ProjectionOverlay.INSTANCE.drag(this,mx,my);}
    @Override protected void mouseReleased(int mx,int my,int button){ProjectionOverlay.INSTANCE.release(this);super.mouseReleased(mx,my,button);}
    @Override protected void keyTyped(char character,int key)throws IOException{if(key==org.lwjgl.input.Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);else super.keyTyped(character,key);}
    @Override public void onGuiClosed(){ProjectionOverlay.INSTANCE.release(this);}
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0)ProjectionOverlay.INSTANCE.scroll(wheel);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
