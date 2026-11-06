package net.createmod.ponder.client.projection;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.*;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.*;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

/** Production-jar checks on terrain, including real mouse dispatch and cancel/commit boundaries. */
public final class ProjectionPlacementSmoke {
    private final Minecraft mc;
    private final BiConsumer<Boolean,String> check;
    private final Consumer<String> screenshot;
    private int phase,wait;
    private Future<?> stage;
    private PonderScene example;
    private ProjectionSession session;
    private BlockPos committed;
    private int turns;
    private boolean mirror;
    public ProjectionPlacementSmoke(Minecraft mc,BiConsumer<Boolean,String> check,Consumer<String> screenshot){this.mc=mc;this.check=check;this.screenshot=screenshot;}

    public boolean advance()throws Exception{
        if(wait-->0)return false;
        switch(phase++){
            case 0:
                ProjectionService.clear();mc.displayGuiScreen(null);
                stage=mc.getIntegratedServer().addScheduledTask(()->{
                    var world=mc.getIntegratedServer().getWorld(0);
                    for(int x=14;x<=35;x++)for(int z=12;z<=35;z++){
                        world.setBlockState(new BlockPos(x,69,z),Blocks.GRASS.getDefaultState(),2);
                        for(int y=70;y<=77;y++)world.setBlockToAir(new BlockPos(x,y,z));
                    }
                });wait=4;break;
            case 1:{
                if(!stage.isDone()){phase--;wait=3;break;}stage.get();
                mc.player.setPositionAndRotation(24.5,73,30.5,180,28);mc.player.capabilities.isFlying=true;
                PonderLevel world=new PonderLevel(BlockPos.ORIGIN,null);
                world.setBlockState(new BlockPos(0,1,0),Blocks.STONE.getDefaultState(),2);
                world.setBlockState(new BlockPos(1,1,0),Blocks.STONE.getDefaultState(),2);
                world.setBlockState(new BlockPos(0,2,0),Blocks.CHEST.getDefaultState(),2);world.createBackup();
                example=new PonderScene(world,new PonderLocalization(),"smoke",new ResourceLocation("minecraft:clock"),List.of(),List.of());
                example.setSceneTitle("smoke:world_placement","World placement preview");example.builder().configureBasePlate(0,0,3);example.builder().showBasePlate();
                wait=15;break;
            }
            case 2:
                ProjectionService.project(example,null);session=ProjectionService.current();
                check.accept(mc.currentScreen==null&&ProjectionService.placing(),"projection button enters world placement with the normal camera and movement controls");
                check.accept(session.total==3&&!session.plan.hasBase(),"a declared but nonexistent baseplate cannot hide the lowest machine layer");
                check.accept(session.bounds().getCenter().distanceTo(mc.player.getPositionEyes(1))<10,"new blueprint is placed in view near the player instead of a distant hidden anchor");wait=8;break;
            case 3:screenshot.accept("projection-placement-terrain.png");wait=5;break;
            case 4:{
                BlockPos previous=session.anchor;mc.player.rotationYaw=155;ProjectionService.updateAim();
                check.accept(!session.anchor.equals(previous),"moving the view moves the unfixed blueprint along the crosshair");
                int oldTurns=session.turns;
                check.accept(ProjectionPlacementEvents.handleMouse(1,true,0,false),"world right click is consumed for rotation");ProjectionPlacementEvents.handleMouse(1,false,0,false);
                check.accept(session.turns==Math.floorMod(oldTurns+1,4),"right click rotates the live blueprint by ninety degrees");
                ProjectionPlacementEvents.handleMouse(-1,false,120,false);check.accept(ProjectionService.placementHeight()==1,"mouse wheel adjusts placement height without changing hotbar slots");
                ProjectionPlacementEvents.handleMouse(-1,false,120,true);check.accept(ProjectionService.placementReach()==7,"shift wheel adjusts the placement distance");
                ProjectionPlacementEvents.handleMouse(1,true,0,true);ProjectionPlacementEvents.handleMouse(1,false,0,true);check.accept(session.mirror,"shift right click mirrors the preview in the world");
                ProjectionPlacementEvents.handleMouse(0,true,0,false);ProjectionPlacementEvents.handleMouse(0,false,0,false);
                committed=session.anchor;turns=session.turns;mirror=session.mirror;
                check.accept(!ProjectionService.placing()&&mc.currentScreen==null,"left click fixes the projection and returns immediately to normal gameplay");
                check.accept(session.cells().stream().allMatch(c->mc.world.isAirBlock(session.anchor.add(c.position()))),"world placement never places or breaks actual blocks");
                lookAt(session.bounds().getCenter());wait=8;break;
            }
            case 5:screenshot.accept("projection-placement-fixed.png");wait=5;break;
            case 6:{
                mc.player.rotationYaw+=40;ProjectionService.tick();check.accept(session.anchor.equals(committed),"fixed blueprint stays put when the player turns");lookAt(session.bounds().getCenter());
                check.accept(ProjectionPlacementEvents.handleMouse(2,true,0,true),"sneak middle click grabs the visible projection directly in the world");ProjectionPlacementEvents.handleMouse(2,false,0,true);
                check.accept(ProjectionService.placing(),"grabbing enables world movement without a coordinate screen");
                ProjectionService.scrollPlacement(120,false);ProjectionService.rotatePlacement(false);
                GuiOpenEvent escape=new GuiOpenEvent(new GuiIngameMenu());MinecraftForge.EVENT_BUS.post(escape);
                check.accept(escape.isCanceled()&&!ProjectionService.placing(),"Escape cancels placement instead of opening the pause menu");
                check.accept(ProjectionService.current()==session&&session.anchor.equals(committed)&&session.turns==turns&&session.mirror==mirror,"cancel restores the previously fixed position, rotation and mirror");
                mc.displayGuiScreen(new ProjectionScreen(session,null));wait=5;break;
            }
            case 7:screenshot.accept("projection-transparent-toolbox.png");wait=5;break;
            case 8:
                mc.displayGuiScreen(null);lookAt(session.bounds().getCenter());
                stage=mc.getIntegratedServer().addScheduledTask(()->{
                    var world=mc.getIntegratedServer().getWorld(0);int z=(int)session.bounds().maxZ+1;
                    for(int x=(int)session.bounds().minX-1;x<session.bounds().maxX+2;x++)for(int y=70;y<75;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.STONE.getDefaultState(),2);
                });wait=12;break;
            case 9:
                if(!stage.isDone()){phase--;wait=3;break;}stage.get();screenshot.accept("projection-occluded-outline.png");wait=5;break;
            case 10:
                if(Loader.isModLoaded("ae2")){
                    var nativeScene=PonderIndex.getSceneAccess().compile(new ResourceLocation("ae2:drive")).stream().filter(s->s.getId().equals(new ResourceLocation("ae2s_ponder:first_network"))).findFirst().orElseThrow();
                    nativeScene.seekToTime(Math.max(0,nativeScene.getTotalTicks()-1));ProjectionService.project(nativeScene,null);session=ProjectionService.current();ProjectionService.confirmPlacement();
                    session.anchor=new BlockPos(22,70,17);session.refresh();var center=session.bounds().getCenter();mc.player.setPositionAndRotation(center.x,center.y+4,center.z+9,180,24);lookAt(center);
                    check.accept(session.total==7&&session.rows.stream().anyMatch(r->r.target().item().toString().equals("ae2:terminal"))
                        &&session.cells().stream().anyMatch(c->c.state().getBlock().getRegistryName().toString().equals("ae2:cable_bus")),
                        "AE2S native machines and multipart cables remain available in the world blueprint");wait=12;
                }
                break;
            case 11:if(Loader.isModLoaded("ae2")){screenshot.accept("projection-native-ae2s-world.png");wait=5;}break;
            default:ProjectionService.clear();return true;
        }
        return false;
    }

    private void lookAt(Vec3d target){
        var delta=target.subtract(mc.player.getPositionEyes(1));mc.player.rotationYaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90);
        mc.player.rotationPitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
    }
    public void verifyFrame(String name){
        if(!name.startsWith("projection-placement-")&&!name.equals("projection-transparent-toolbox.png")&&!name.equals("projection-occluded-outline.png")&&!name.equals("projection-native-ae2s-world.png"))return;
        var frame=net.minecraft.util.ScreenShotHelper.createScreenshot(mc.displayWidth,mc.displayHeight,mc.getFramebuffer());int grass=0,cyan=0;
        for(int y=frame.getHeight()/4;y<frame.getHeight()*4/5;y++)for(int x=frame.getWidth()/4;x<frame.getWidth()*4/5;x++){
            int rgb=frame.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
            if(g>r*1.12&&g>b*1.1&&g>35)grass++;
            if(g>r+22&&b>r+25&&r<150&&g>100)cyan++;
        }
        check.accept(grass>150,name+" keeps actual terrain visible behind the projection controls");
        check.accept(cyan>15,name+" contains the world blueprint outline, including occluded geometry");
    }
}
