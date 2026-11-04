package net.createmod.ponder.mixin.revoui;

import net.createmod.ponder.compat.revoui.RevoTooltipLayout;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Consume Revo's retained layout, including header, preview and visual-node spacing. */
@Pseudo
@Mixin(targets = "neofontrender.addons.tooltips.TooltipContentLayout", remap = false)
public abstract class RevoTooltipLayoutMixin {
    @Inject(method = "build", at = @At("RETURN"), require = 0, remap = false)
    private static void ponder$captureProgressRow(@Coerce Object layout, FontRenderer font, ItemStack stack,
                                                 CallbackInfoReturnable<Object> callback) {
        RevoTooltipLayout.capture(layout, callback.getReturnValue(), font, stack);
    }
}
