package net.createmod.ponder.client.projection;

import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.projection.ProjectionPlan;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.opengl.GL11;

/** One active, client-only blueprint. No placement packets or player-world mutation. */
@SideOnly(Side.CLIENT)
public final class ProjectionService {
    private static ProjectionSession current;
    private static final PonderWorldRenderer RENDERER=new PonderWorldRenderer();
    private static Placement placement;
    private static int ticks;
    private static int statusTicks;
    private static final class Placement {
        final ProjectionSession previous;
        final BlockPos anchor;
        final int turns,layer;
        final boolean mirror,base,projecting;
        int height;
        double reach=6;
        Placement(ProjectionSession previous){
            this.previous=previous;anchor=previous==null?BlockPos.ORIGIN:previous.anchor;
            turns=previous==null?0:previous.turns;layer=previous==null?-1:previous.layer;
            mirror=previous!=null&&previous.mirror;base=previous!=null&&previous.includeBase;projecting=previous!=null&&previous.projecting;
        }
    }
    private ProjectionService(){}
    public static ProjectionSession current(){return current;}
    public static void clear(){current=null;placement=null;statusTicks=0;}
    public static boolean placing(){return current!=null&&current.placing&&placement!=null;}
    public static boolean showLockedHint(){return current!=null&&current.projecting&&!placing()&&statusTicks>0;}
    public static BlockPos target(){
        return target(16);
    }
    private static BlockPos target(double reach){
        Minecraft mc=Minecraft.getMinecraft();RayTraceResult hit=mc.player.rayTrace(reach,1);
        if(hit!=null&&hit.typeOfHit==RayTraceResult.Type.BLOCK){
            BlockPos p=hit.getBlockPos();return mc.world.getBlockState(p).getBlock().isReplaceable(mc.world,p)?p:p.offset(hit.sideHit);
        }
        return new BlockPos(mc.player.getPositionEyes(1).add(mc.player.getLookVec().scale(Math.min(6,reach))));
    }
    public static void project(PonderScene scene,GuiScreen parent){
        if(GuiScreen.isShiftKeyDown()){manage(scene,parent);return;}
        try{
            ProjectionPlan plan=ProjectionPlan.capture(scene);ProjectionSession next=new ProjectionSession(plan,target());
            if(placing())cancelPlacement();placement=new Placement(current);current=next;startPlacement();
        }
        catch(RuntimeException failure){error(failure);}
    }
    public static void move(){
        if(current==null||placing())return;
        placement=new Placement(current);startPlacement();
    }
    private static void startPlacement(){
        current.placing=true;current.projecting=true;current.layer=-1;
        Minecraft.getMinecraft().displayGuiScreen(null);updateAim();current.refresh();
        Minecraft.getMinecraft().setIngameFocus();
    }
    public static void updateAim(){
        Minecraft mc=Minecraft.getMinecraft();if(!placing()||mc.currentScreen!=null||mc.player==null)return;
        BlockPos surface=target(16).offset(mc.player.getHorizontalFacing(),(int)placement.reach-6).up(placement.height);
        BlockPos at=current.anchorAt(surface);
        if(!current.anchor.equals(at)){current.anchor=at;current.refresh();}
    }
    public static void confirmPlacement(){
        if(!placing())return;current.placing=false;placement=null;current.refresh();statusTicks=160;
    }
    public static void cancelPlacement(){
        if(!placing())return;ProjectionSession edited=current;Placement state=placement;placement=null;
        current=state.previous;edited.placing=false;
        if(current==edited){current.anchor=state.anchor;current.turns=state.turns;current.layer=state.layer;current.mirror=state.mirror;current.includeBase=state.base;current.projecting=state.projecting;current.rebuild();}
    }
    public static void rotatePlacement(boolean mirror){
        if(!placing())return;if(mirror)current.mirror=!current.mirror;else current.turns=(current.turns+1)%4;
        current.rebuild();updateAim();
    }
    public static void scrollPlacement(int wheel,boolean distance){
        if(!placing()||wheel==0)return;
        if(distance)placement.reach=Math.max(2,Math.min(32,placement.reach+Integer.signum(wheel)));
        else placement.height=Math.max(-64,Math.min(64,placement.height+Integer.signum(wheel)));
        updateAim();
    }
    public static int placementHeight(){return placement==null?0:placement.height;}
    public static double placementReach(){return placement==null?6:placement.reach;}
    public static boolean lookingAtProjection(){
        Minecraft mc=Minecraft.getMinecraft();if(current==null||!current.projecting||mc.player==null)return false;
        Vec3d eye=mc.player.getPositionEyes(1),end=eye.add(mc.player.getLookVec().scale(12));
        return current.bounds().grow(.2).contains(eye)||current.bounds().grow(.2).calculateIntercept(eye,end)!=null;
    }
    public static void pin(PonderScene scene,GuiScreen parent){
        if(GuiScreen.isShiftKeyDown()){manage(scene,parent);return;}
        try{if(placing())cancelPlacement();if(current!=null&&current.plan.id().equals(scene.getId()))current.pinned=!current.pinned;
            else{current=new ProjectionSession(ProjectionPlan.capture(scene),target());current.projecting=false;current.pinned=true;}current.refresh();}
        catch(RuntimeException failure){error(failure);}
    }
    public static void manage(PonderScene scene,GuiScreen parent){
        try{if(placing())cancelPlacement();ProjectionPlan plan=ProjectionPlan.capture(scene);if(current==null||!current.plan.id().equals(plan.id())){current=new ProjectionSession(plan,target());current.projecting=true;current.refresh();}Minecraft.getMinecraft().displayGuiScreen(new ProjectionScreen(current,parent));}
        catch(RuntimeException failure){error(failure);}
    }
    private static void error(RuntimeException failure){if(Minecraft.getMinecraft().player!=null)Minecraft.getMinecraft().player.sendMessage(new TextComponentString(net.minecraft.client.resources.I18n.format("ponder.projection.failed",failure.getMessage())));}
    public static void tick(){if(current==null)return;if(!current.validDimension()){clear();return;}if(statusTicks>0)statusTicks--;updateAim();if(++ticks%10==0)current.refresh();}
    public static void render(Vec3d camera,float partial){
        ProjectionSession s=current;if(s==null||!s.projecting||s.ghost==null||!s.validDimension())return;
        Vec3d local=camera.subtract(new Vec3d(s.anchor));
        try(GlStateGuard ignored=GlStateGuard.capture()){
            RENDERER.renderProjection(s.ghost,local,partial);
            GlStateManager.disableTexture2D();GlStateManager.disableLighting();GlStateManager.disableFog();
            GlStateManager.enableBlend();GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GlStateManager.depthMask(false);
            float line=GL11.glGetFloat(GL11.GL_LINE_WIDTH);GL11.glLineWidth(2);
            try{
                GlStateManager.disableDepth();
                RenderGlobal.drawSelectionBoundingBox(s.bounds().offset(-camera.x,-camera.y,-camera.z).grow(.005),.35f,.94f,1,.6f);
                int count=0;
                for(BlockPos pos:s.visiblePositions()){
                    if(++count>256)break;boolean conflict=s.conflicts.contains(pos);
                    var box=new AxisAlignedBB(s.anchor.add(pos)).offset(-camera.x,-camera.y,-camera.z).grow(.003);
                    RenderGlobal.drawSelectionBoundingBox(box,conflict?1:.35f,conflict?.35f:.9f,conflict?.2f:1,s.placing?.4f:.26f);
                }
                var origin=new AxisAlignedBB(s.anchor).shrink(.32).offset(-camera.x,-camera.y,-camera.z);
                RenderGlobal.drawSelectionBoundingBox(origin,1,.85f,.25f,1);
            }finally{GL11.glLineWidth(line);}
        }
    }
}
