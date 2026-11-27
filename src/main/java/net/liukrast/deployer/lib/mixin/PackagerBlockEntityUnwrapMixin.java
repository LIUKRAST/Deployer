package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.GenericPackageItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Priority above 1000: applied after Create Factory Logistics, which overwrites some of these methods
@Mixin(value = PackagerBlockEntity.class, priority = 1100)
public abstract class PackagerBlockEntityUnwrapMixin extends SmartBlockEntity {
    @Shadow public ItemStack heldBox;
    @Shadow public boolean animationInward;
    @Shadow public int animationTicks;

    public PackagerBlockEntityUnwrapMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "unwrapBox", at = @At("HEAD"), cancellable = true)
    private void unwrapBox(ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if(!(box.getItem() instanceof GenericPackageItem)) return;
        if(heldBox.isEmpty()) {
            heldBox = box;
            animationInward = false;
            animationTicks = 20;
            notifyUpdate();
        }
        cir.setReturnValue(false);
        cir.cancel();
    }
}
