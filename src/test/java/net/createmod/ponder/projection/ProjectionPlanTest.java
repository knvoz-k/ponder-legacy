package net.createmod.ponder.projection;

import java.util.*;
import net.createmod.ponder.api.projection.*;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.createmod.ponder.api.guide.Tutorial.Kind;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import org.junit.*;
import static org.junit.Assert.*;

public class ProjectionPlanTest {
    @BeforeClass public static void bootstrap(){Bootstrap.register();}
    @Test public void allQuarterTurnsAndMirroringKeepCorrectCoordinates(){
        BlockPos p=new BlockPos(2,3,1);
        assertEquals(new BlockPos(-1,3,2),ProjectionPlan.position(p,1,false));
        assertEquals(p,ProjectionPlan.position(p,4,false));
        assertEquals(new BlockPos(1,3,-2),ProjectionPlan.position(p,3,false));
        assertEquals(new BlockPos(-2,3,1),ProjectionPlan.position(p,0,true));
    }
    @Test public void rotatesAe2sMultipartFacesWithoutChangingSourceNbt(){
        NBTTagCompound n=new NBTTagCompound(),part=new NBTTagCompound();n.setString("id","ae2:cable_bus");n.setString("forward","NORTH");part.setString("id","ae2:storage_bus");n.setTag("east",part);
        NBTTagCompound rotated=ProjectionPlan.transformTile(n,1,false);
        assertTrue(rotated.hasKey("south"));assertFalse(rotated.hasKey("east"));assertEquals("EAST",rotated.getString("forward"));
        assertTrue(n.hasKey("east"));assertEquals("NORTH",n.getString("forward"));
    }
    @Test public void planOwnsTileDataAndCanRemoveOnlyItsBaseLayer(){
        NBTTagCompound n=new NBTTagCompound();n.setString("name","source");
        var cell=new ProjectionPlan.Cell(new BlockPos(0,1,0),Blocks.STONE.getDefaultState(),n);n.setString("name","changed");
        var plan=new ProjectionPlan(new ResourceLocation("test:plan"),"Plan",List.of(new ProjectionPlan.Cell(BlockPos.ORIGIN,Blocks.STONE.getDefaultState(),null),cell),true);
        assertEquals(1,plan.transform(0,false,false).size());assertEquals(BlockPos.ORIGIN,plan.transform(0,false,false).get(0).position());assertEquals("source",cell.tile().getString("name"));
        cell.tile().setString("name","outside");assertEquals("source",cell.tile().getString("name"));
    }
    @Test public void inventoryCountsVariantsAndAllMatchingStacks(){
        var wool=new MaterialRequirement(new ResourceLocation("minecraft:wool"),14,8);
        assertEquals(5,wool.available(List.of(new ItemStack(Blocks.WOOL,2,14),new ItemStack(Blocks.WOOL,3,14),new ItemStack(Blocks.WOOL,64,11))));
        assertFalse(wool.matches(ItemStack.EMPTY));
    }
    @Test public void materialResolutionAvoidsDoubleCountingDoorsAndUsesFluidBuckets(){
        var upper=new ProjectionPlan.Cell(BlockPos.ORIGIN,Blocks.OAK_DOOR.getDefaultState().withProperty(net.minecraft.block.BlockDoor.HALF,net.minecraft.block.BlockDoor.EnumDoorHalf.UPPER),null);
        assertTrue(PonderMaterialResolvers.resolve(upper,null).isEmpty());
        var water=new ProjectionPlan.Cell(BlockPos.ORIGIN,Blocks.WATER.getDefaultState(),null);
        assertSame(Items.WATER_BUCKET,PonderMaterialResolvers.resolve(water,null).get(0).getItem());
    }
    @Test public void multimediaAndQuestLinksCompileWithoutOptionalMods(){
        var page=MarkdownTutorialCompiler.compile(new ResourceLocation("test:media"),"# Media\n\n<Video src=\"https://www.bilibili.com/video/BV1B7411m7LV\" />\n\n<Image src=\"local:guide.png\" />\n\n<QuestLink id=\"1234abcd\" title=\"Task\" />\n","test.md");
        assertTrue(page.blocks().stream().anyMatch(b->b.kind()==Kind.VIDEO));assertTrue(page.blocks().stream().anyMatch(b->b.kind()==Kind.QUEST_LINK));
        assertThrows(IllegalArgumentException.class,()->MarkdownTutorialCompiler.compile(new ResourceLocation("test:bad"),"# Unsafe\n\n<Video src=\"local:../private.mp4\" />","bad.md"));
    }
}
