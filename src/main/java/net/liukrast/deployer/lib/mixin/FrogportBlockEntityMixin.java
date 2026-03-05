package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FrogportBlockEntity.class)
public abstract class FrogportBlockEntityMixin extends PackagePortBlockEntity {
    public FrogportBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @WrapOperation(
            method = "startAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packagePort/PackagePortTarget;export(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)Z"
            )
    )
    private boolean startAnimation(PackagePortTarget instance, LevelAccessor level, BlockPos blockPos, ItemStack itemStack, boolean b, Operation<Boolean> original) {
        if(!(instance.be(level, blockPos) instanceof ChainConveyorBlockEntity ccbe)) return original.call(instance, level, blockPos, itemStack, b);
        if(ccbe.canAcceptPackagesFor(((PackagePortTarget.ChainConveyorFrogportTarget)instance).connection)) return original.call(instance, level, blockPos, itemStack, b);
        return false;
    }
}
