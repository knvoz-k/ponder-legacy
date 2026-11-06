package net.createmod.ponder.client.projection;

import java.util.*;
import net.createmod.ponder.api.projection.*;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.*;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public final class ProjectionSession {
    public record Row(MaterialRequirement target,int available) {public int missing(){return Math.max(0,target.count()-available);}}
    public final ProjectionPlan plan;
    public BlockPos anchor;
    public int turns,layer=-1;
    public boolean mirror,includeBase,hideCorrect=true,projecting,pinned=true;
    private List<ProjectionPlan.Cell> cells=List.of();
    private Map<BlockPos,List<ItemStack>> items=Map.of();
    private Set<BlockPos> visible=Set.of();
    public List<Row> rows=List.of();
    public List<String> unknown=List.of();
    public List<BlockPos> conflicts=List.of();
    public int complete,total;
    public boolean placing;
    public PonderScene ghost;
    private PonderLevel world;
    private final int dimension;
    private net.minecraft.util.math.AxisAlignedBB localBounds=new net.minecraft.util.math.AxisAlignedBB(0,0,0,1,1,1);
    public ProjectionSession(ProjectionPlan plan,BlockPos anchor){this.plan=plan;this.anchor=anchor;includeBase=!plan.hasBase();dimension=Minecraft.getMinecraft().world.provider.getDimension();rebuild();}
    public int maxLayer(){return cells.stream().mapToInt(c->c.position().getY()).max().orElse(0);}
    public List<ProjectionPlan.Cell> cells(){return cells;}
    public Set<BlockPos> visiblePositions(){return visible;}
    public net.minecraft.util.math.AxisAlignedBB bounds(){return localBounds.offset(anchor);}
    /** Aim at the centre of the footprint so a large template stays in view while positioning it. */
    public BlockPos anchorAt(BlockPos surface){
        var box=localBounds;int cx=(int)Math.floor((box.minX+box.maxX-1)/2),cz=(int)Math.floor((box.minZ+box.maxZ-1)/2);
        return new BlockPos(surface.getX()-cx,Math.max(0,Math.min(255-maxLayer(),surface.getY())),surface.getZ()-cz);
    }
    public boolean validDimension(){return Minecraft.getMinecraft().world!=null&&Minecraft.getMinecraft().world.provider.getDimension()==dimension;}
    public void rebuild(){
        cells=plan.transform(turns,mirror,includeBase);
        if(cells.isEmpty()){includeBase=true;cells=plan.transform(turns,mirror,true);}
        BlockPos min=new BlockPos(cells.stream().mapToInt(c->c.position().getX()).min().orElse(0),cells.stream().mapToInt(c->c.position().getY()).min().orElse(0),cells.stream().mapToInt(c->c.position().getZ()).min().orElse(0));
        BlockPos max=new BlockPos(cells.stream().mapToInt(c->c.position().getX()).max().orElse(0),maxLayer(),cells.stream().mapToInt(c->c.position().getZ()).max().orElse(0));
        localBounds=new net.minecraft.util.math.AxisAlignedBB(min,max.add(1,1,1));
        world=new PonderLevel(BlockPos.ORIGIN,null);
        for(var c:cells)world.setBlockState(c.position(),c.state(),2);
        for(var c:cells)if(c.tile()!=null){var nbt=c.tile();nbt.setInteger("x",c.position().getX());nbt.setInteger("y",c.position().getY());nbt.setInteger("z",c.position().getZ());var tile=TileEntity.create(world,nbt);if(tile!=null)world.setTileEntity(c.position(),tile);}
        world.createBackup();Map<BlockPos,List<ItemStack>> resolved=new LinkedHashMap<>();List<String> unknown=new ArrayList<>();
        for(var c:cells){List<ItemStack> materials;
            try{materials=PonderMaterialResolvers.resolve(c,world);}catch(RuntimeException failure){materials=null;}
            if(materials==null){unknown.add(c.state().getBlock().getRegistryName()+" @ "+c.position().getX()+","+c.position().getY()+","+c.position().getZ());continue;}
            List<ItemStack> safe=new ArrayList<>();for(ItemStack material:materials)if(material!=null&&!material.isEmpty()){ItemStack copy=material.copy();copy.setTagCompound(null);safe.add(copy);}
            resolved.put(c.position(),safe);
        }
        this.items=resolved;this.unknown=List.copyOf(unknown);visible=Set.of();ghost=null;refresh();
    }
    public void refresh(){
        Minecraft mc=Minecraft.getMinecraft();if(mc.player==null||mc.world==null)return;
        Map<String,MaterialRequirement> required=new LinkedHashMap<>();Set<BlockPos> show=new LinkedHashSet<>();List<BlockPos> conflicts=new ArrayList<>();
        complete=0;total=cells.size();
        for(var c:cells){boolean matches=projecting&&!placing&&matches(c,anchor.add(c.position()));if(matches)complete++;
            if(layer<0||c.position().getY()==layer){if(!hideCorrect||!matches)show.add(c.position());if(projecting&&!matches&&!mc.world.isAirBlock(anchor.add(c.position())))conflicts.add(c.position());}
            if(matches)continue;
            for(ItemStack stack:items.getOrDefault(c.position(),List.of())){
                MaterialRequirement r=MaterialRequirement.of(stack);String key=r.item()+"@"+r.meta();var old=required.get(key);
                required.put(key,new MaterialRequirement(r.item(),r.meta(),r.count()+(old==null?0:old.count())));
            }
        }
        List<ItemStack> inventory=new ArrayList<>(mc.player.inventory.mainInventory);inventory.addAll(mc.player.inventory.offHandInventory);
        rows=required.values().stream().map(r->new Row(r,r.available(inventory))).sorted(Comparator.<Row>comparingInt(r->r.missing()>0?0:1).thenComparing(r->r.target().item().toString())).toList();
        this.conflicts=List.copyOf(conflicts);
        if(!show.equals(visible)||ghost==null){
            visible=Set.copyOf(show);Selection selection=SelectionImpl.empty();for(BlockPos p:show)selection.add(SelectionImpl.of(p));
            ghost=new PonderScene(world,new PonderLocalization(),plan.id().getNamespace(),plan.id(),List.of(),List.of());
            ghost.builder().world().showSection(selection,EnumFacing.DOWN);ghost.begin();ghost.seekToTime(ghost.getTotalTicks());ghost.setPaused(true);
        }
    }
    private boolean matches(ProjectionPlan.Cell cell,BlockPos at){
        var real=Minecraft.getMinecraft().world;
        if(!real.isBlockLoaded(at)||!real.getBlockState(at).equals(cell.state()))return false;
        NBTTagCompound expected=cell.tile();if(expected==null)return true;
        var actual=real.getTileEntity(at);if(actual==null)return false;
        NBTTagCompound data=actual.writeToNBT(new NBTTagCompound());
        for(String key:List.of("forward","up"))if(expected.hasKey(key,8)&&!expected.getString(key).equalsIgnoreCase(data.getString(key)))return false;
        if(expected.getString("id").equals("ae2:cable_bus"))for(String side:List.of("cable","down","up","north","south","west","east"))
            if(!expected.getCompoundTag(side).getString("id").equals(data.getCompoundTag(side).getString("id")))return false;
        return true;
    }
}
