package net.liukrast.deployer.lib.mixin.compat.cmpackagecouriers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.liukrast.deployer.lib.logistics.packager.GenericPackageItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.kreidev.cmpackagecouriers.plane.CardboardPlane", remap = false)
public abstract class CardboardPlaneMixin {

    @Shadow public ItemStack box;

    @ModifyExpressionValue(
            method = "onReachedTarget",
            at = @At(value = "FIELD", target = "Lcom/kreidev/cmpackagecouriers/plane/CardboardPlane;unpack:Z")
    )
    private boolean deployer$keepGenericPackagesClosed(boolean unpack) {
        return unpack && !(box.getItem() instanceof GenericPackageItem);
    }
}
