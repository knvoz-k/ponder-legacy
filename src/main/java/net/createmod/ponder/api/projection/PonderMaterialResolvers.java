package net.createmod.ponder.api.projection;

import java.util.*;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Optional addons can resolve multipart blocks without a hard dependency on the rendering library. */
public final class PonderMaterialResolvers {
    @FunctionalInterface public interface Resolver {
        /** null delegates to the next resolver; an empty list means no item is needed for this cell. */
        List<ItemStack> resolve(IBlockState state,NBTTagCompound tile,World world,BlockPos pos);
    }
    private static final Map<ResourceLocation,Resolver> CUSTOM=new LinkedHashMap<>();
    private PonderMaterialResolvers(){}
    public static synchronized void register(ResourceLocation id,Resolver resolver){if(CUSTOM.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(resolver))!=null)throw new IllegalArgumentException("Duplicate material resolver: "+id);}
    public static List<ItemStack> resolve(ProjectionPlan.Cell cell,World world){
        List<Resolver> custom; synchronized(PonderMaterialResolvers.class){custom=List.copyOf(CUSTOM.values());}
        for(Resolver resolver:custom){List<ItemStack> result=resolver.resolve(cell.state(),cell.tile(),world,cell.position());if(result!=null)return result;}
        IBlockState state=cell.state();var block=state.getBlock();
        if(block instanceof BlockDoor&&state.getValue(BlockDoor.HALF)==BlockDoor.EnumDoorHalf.UPPER
            ||block instanceof BlockBed&&state.getValue(BlockBed.PART)==BlockBed.EnumPartType.HEAD
            ||block instanceof BlockDoublePlant&&state.getValue(BlockDoublePlant.HALF)==BlockDoublePlant.EnumBlockHalf.UPPER)return List.of();
        NBTTagCompound tile=cell.tile();
        if(new ResourceLocation("ae2:cable_bus").equals(block.getRegistryName())&&tile!=null){
            List<ItemStack> parts=new ArrayList<>();
            for(String key:List.of("cable","down","up","north","south","west","east"))if(tile.hasKey(key,10)){
                var part=tile.getCompoundTag(key);Item item=Item.REGISTRY.getObject(new ResourceLocation(part.getString("id")));
                if(item==null||item==net.minecraft.init.Items.AIR)return null;
                parts.add(new ItemStack(item,1,part.getInteger("Damage")));
            }
            return parts.isEmpty()?null:parts;
        }
        if (block==net.minecraft.init.Blocks.WATER || block==net.minecraft.init.Blocks.FLOWING_WATER || block==net.minecraft.init.Blocks.LAVA || block==net.minecraft.init.Blocks.FLOWING_LAVA) {
            if(state.getValue(net.minecraft.block.BlockLiquid.LEVEL)!=0)return List.of();
            return List.of(new ItemStack(block==net.minecraft.init.Blocks.WATER||block==net.minecraft.init.Blocks.FLOWING_WATER?net.minecraft.init.Items.WATER_BUCKET:net.minecraft.init.Items.LAVA_BUCKET));
        }
        try {
            ItemStack picked=block.getPickBlock(state,new net.minecraft.util.math.RayTraceResult(new net.minecraft.util.math.Vec3d(cell.position()),net.minecraft.util.EnumFacing.UP,cell.position()),world,cell.position(),null);
            if(picked!=null&&!picked.isEmpty()){picked=picked.copy();picked.setCount(block instanceof BlockSlab slab&&slab.isDouble()?2:1);return List.of(picked);}
        }catch(RuntimeException ignored){}
        Item item=Item.getItemFromBlock(block);if(item==null||item==net.minecraft.init.Items.AIR)return null;
        int meta=block.damageDropped(state),count=block instanceof BlockSlab slab&&slab.isDouble()?2:1;
        // Use the source state's item form, not inventory contents or a block's mined drops.
        ItemStack stack=new ItemStack(item,count,meta);return List.of(stack);
    }
}
