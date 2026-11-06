package net.createmod.ponder.api.projection;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Construction compares item identity and variant; it does not demand copied machine inventory NBT. */
public record MaterialRequirement(ResourceLocation item,int meta,int count) {
    public static MaterialRequirement of(ItemStack stack){return new MaterialRequirement(stack.getItem().getRegistryName(),stack.getMetadata(),stack.getCount());}
    public boolean matches(ItemStack stack){return !stack.isEmpty()&&item.equals(stack.getItem().getRegistryName())&&(meta==32767||meta==stack.getMetadata());}
    public int available(Iterable<ItemStack> inventory){int n=0;for(ItemStack stack:inventory)if(matches(stack))n+=stack.getCount();return n;}
    public ItemStack stack(){var value=net.minecraft.item.Item.REGISTRY.getObject(item);return value==null?ItemStack.EMPTY:new ItemStack(value,1,meta==32767?0:meta);}
}
