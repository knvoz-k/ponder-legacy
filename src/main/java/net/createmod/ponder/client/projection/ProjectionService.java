package net.createmod.ponder.client.projection;

import java.nio.ByteBuffer;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.ponder.api.projection.ProjectionPlan;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** One active, client-only blueprint. No placement packets or player-world mutation. */
@SideOnly(Side.CLIENT)
public final class ProjectionService {
    private static ProjectionSession current;
    private static final PonderWorldRenderer RENDERER=new PonderWorldRenderer();
    private static final ByteBuffer STIPPLE=pattern();
    private static int ticks;
    private ProjectionService(){}
    private static ByteBuffer pattern(){ByteBuffer b=BufferUtils.createByteBuffer(128);for(int y=0;y<32;y++)for(int x=0;x<4;x++)b.put((byte)(y%2==0?0x55:0xaa));b.flip();return b;}
    public static ProjectionSession current(){return current;}
    public static void clear(){current=null;}
    public static BlockPos target(){
        Minecraft mc=Minecraft.getMinecraft();RayTraceResult hit=mc.player.rayTrace(32,1);
        return hit!=null&&hit.typeOfHit==RayTraceResult.Type.BLOCK?hit.getBlockPos().offset(hit.sideHit):new BlockPos(mc.player.getPositionVector().add(mc.player.getLookVec().scale(3))).up();
    }
    public static void project(PonderScene scene,GuiScreen parent){
        if(GuiScreen.isShiftKeyDown()){manage(scene,parent);return;}
        try{current=new ProjectionSession(ProjectionPlan.capture(scene),target());current.projecting=true;current.refresh();Minecraft.getMinecraft().displayGuiScreen(null);}
        catch(RuntimeException failure){error(failure);}
    }
    public static void pin(PonderScene scene,GuiScreen parent){
        if(GuiScreen.isShiftKeyDown()){manage(scene,parent);return;}
        try{if(current!=null&&current.plan.id().equals(scene.getId()))current.pinned=!current.pinned;
            else{current=new ProjectionSession(ProjectionPlan.capture(scene),target());current.projecting=false;current.pinned=true;}current.refresh();}
        catch(RuntimeException failure){error(failure);}
    }
    public static void manage(PonderScene scene,GuiScreen parent){
        try{ProjectionPlan plan=ProjectionPlan.capture(scene);if(current==null||!current.plan.id().equals(plan.id()))current=new ProjectionSession(plan,target());Minecraft.getMinecraft().displayGuiScreen(new ProjectionScreen(current,parent));}
        catch(RuntimeException failure){error(failure);}
    }
    private static void error(RuntimeException failure){if(Minecraft.getMinecraft().player!=null)Minecraft.getMinecraft().player.sendMessage(new TextComponentString(net.minecraft.client.resources.I18n.format("ponder.projection.failed",failure.getMessage())));}
    public static void tick(){if(current==null)return;if(!current.validDimension()){clear();return;}if(++ticks%10==0)current.refresh();}
    public static void render(Vec3d camera,float partial){
        ProjectionSession s=current;if(s==null||!s.projecting||s.ghost==null||!s.validDimension())return;
        Vec3d local=camera.subtract(new Vec3d(s.anchor));
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GL11.glPushAttrib(GL11.GL_POLYGON_STIPPLE_BIT|GL11.GL_ENABLE_BIT);
            try{GL11.glEnable(GL11.GL_POLYGON_STIPPLE);STIPPLE.rewind();GL11.glPolygonStipple(STIPPLE);RENDERER.renderScene(s.ghost,local,partial);}
            finally{GL11.glPopAttrib();}
            for(BlockPos pos:s.conflicts.stream().limit(48).toList()){
                AABBOutline outline=new AABBOutline(new AxisAlignedBB(s.anchor.add(pos)).grow(.005));outline.getParams().colored(0xf08070).lineWidth(.025f);outline.render(camera,partial);
            }
        }
    }
}
