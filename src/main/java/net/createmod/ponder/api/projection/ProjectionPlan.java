package net.createmod.ponder.api.projection;

import java.util.*;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;

/** Immutable client-authored blueprint. Transforming a plan never writes to the player's world. */
public final class ProjectionPlan {
    public static final int MAX_BLOCKS=8192;
    public record Cell(BlockPos position,IBlockState state,NBTTagCompound tile) {
        public Cell {position=position.toImmutable();tile=tile==null?null:tile.copy();}
        @Override public NBTTagCompound tile(){return tile==null?null:tile.copy();}
    }
    private final ResourceLocation id;
    private final String title;
    private final List<Cell> cells;
    private final BlockPos min,max;
    private final boolean hasBase;
    public ProjectionPlan(ResourceLocation id,String title,List<Cell> cells,boolean hasBase){
        if(cells.isEmpty()||cells.size()>MAX_BLOCKS)throw new IllegalArgumentException("Projection needs 1–"+MAX_BLOCKS+" blocks");
        this.id=id;this.title=title;this.cells=List.copyOf(cells);this.hasBase=hasBase;
        min=new BlockPos(cells.stream().mapToInt(c->c.position().getX()).min().orElse(0),cells.stream().mapToInt(c->c.position().getY()).min().orElse(0),cells.stream().mapToInt(c->c.position().getZ()).min().orElse(0));
        max=new BlockPos(cells.stream().mapToInt(c->c.position().getX()).max().orElse(0),cells.stream().mapToInt(c->c.position().getY()).max().orElse(0),cells.stream().mapToInt(c->c.position().getZ()).max().orElse(0));
    }
    public static ProjectionPlan capture(PonderScene scene){
        List<Cell> cells=new ArrayList<>();var world=scene.getWorld();
        if(world==null)throw new IllegalArgumentException("Scene has no 3D structure");
        for(BlockPos pos:world.getOccupiedPositions()){
            NBTTagCompound nbt=null;var tile=world.getTileEntity(pos);if(tile!=null)nbt=tile.writeToNBT(new NBTTagCompound());
            cells.add(new Cell(pos,world.getBlockState(pos),nbt));
            if(cells.size()>MAX_BLOCKS)throw new IllegalArgumentException("Projection exceeds "+MAX_BLOCKS+" blocks");
        }
        int y=world.getBoundsMin().getY();
        long foundation=cells.stream().filter(c->c.position().getY()==y&&new ResourceLocation("minecraft:concrete").equals(c.state().getBlock().getRegistryName())).count();
        return new ProjectionPlan(scene.getId(),scene.getTitle(),cells,scene.isBasePlateShown()||foundation>=9);
    }
    public ResourceLocation id(){return id;}
    public String title(){return title;}
    public List<Cell> cells(){return cells;}
    public boolean hasBase(){return hasBase;}
    public BlockPos min(){return min;}
    public BlockPos max(){return max;}
    public List<Cell> transform(int turns,boolean mirrored,boolean includeBase){
        List<Cell> result=new ArrayList<>();int rotation=Math.floorMod(turns,4);
        Rotation r=Rotation.values()[rotation]; // NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90
        Mirror m=mirrored?Mirror.FRONT_BACK:Mirror.NONE;
        for(Cell cell:cells){
            if(!includeBase&&cell.position().getY()==min.getY())continue;
            BlockPos local=cell.position().subtract(min);
            if(!includeBase)local=local.down();
            local=position(local,rotation,mirrored);
            result.add(new Cell(local,cell.state().withMirror(m).withRotation(r),transformTile(cell.tile(),rotation,mirrored)));
        }
        return List.copyOf(result);
    }
    public static BlockPos position(BlockPos p,int turns,boolean mirror){
        int x=mirror?-p.getX():p.getX(),z=p.getZ();
        return switch(Math.floorMod(turns,4)){case 1->new BlockPos(-z,p.getY(),x);case 2->new BlockPos(-x,p.getY(),-z);case 3->new BlockPos(z,p.getY(),-x);default->new BlockPos(x,p.getY(),z);};
    }
    private static String facing(String value,int turns,boolean mirror){
        EnumFacing f=EnumFacing.byName(value);if(f==null)return value;
        if(f.getAxis()==EnumFacing.Axis.Y)return f.getName();
        BlockPos p=position(new BlockPos(f.getXOffset(),0,f.getZOffset()),turns,mirror);
        return EnumFacing.getFacingFromVector(p.getX(),0,p.getZ()).getName();
    }
    public static NBTTagCompound transformTile(NBTTagCompound source,int turns,boolean mirror){
        if(source==null)return null;NBTTagCompound result=source.copy();
        for(String key:List.of("forward","up"))if(source.hasKey(key,8))result.setString(key,facing(source.getString(key),turns,mirror).toUpperCase(Locale.ROOT));
        if(source.getString("id").equals("ae2:cable_bus")){
            for(EnumFacing side:EnumFacing.values())result.removeTag(side.getName());
            for(EnumFacing side:EnumFacing.values())if(source.hasKey(side.getName(),10))result.setTag(facing(side.getName(),turns,mirror),source.getCompoundTag(side.getName()).copy());
            for(String part:List.of("cable","down","up","north","south","west","east"))if(result.hasKey(part,10)){
                NBTTagCompound visual=result.getCompoundTag(part).getCompoundTag("visual");
                if(visual.hasKey("connections",9)){NBTTagList list=new NBTTagList(),old=visual.getTagList("connections",8);for(int i=0;i<old.tagCount();i++)list.appendTag(new NBTTagString(facing(old.getStringTagAt(i),turns,mirror)));visual.setTag("connections",list);}
            }
        }
        return result;
    }
}
