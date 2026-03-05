package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.liukrast.deployer.lib.logistics.OrderStockTypeData;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.GenericPackageItem;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
import net.liukrast.deployer.lib.registry.DeployerDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PackagerBlockEntity.class)
public abstract class PackagerBlockEntityMixin extends SmartBlockEntity {
    @Shadow
    public ItemStack heldBox;

    @Shadow
    public boolean animationInward;

    @Shadow
    public int animationTicks;

    public PackagerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @WrapOperation(method = "addBehaviours", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    private <E> boolean addBehaviours(List<E> instance, E e, Operation<Boolean> original) {
        var i = PackagerBlockEntity.class.cast(this);
        if (!(i instanceof AbstractPackagerBlockEntity<?, ?, ?>)) return original.call(instance, e);
        return false;
    }

    @WrapOperation(method = {"lazyTick","activate"}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;attemptToSend(Ljava/util/List;)V"))
    private void lazyTick(PackagerBlockEntity instance, List<PackagingRequest> extracted, Operation<Void> original) {
        if(instance instanceof AbstractPackagerBlockEntity<?,?,?> pack) {
            pack.attemptToSendSpecial(null);
            return;
        }
        original.call(instance, extracted);
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

    @Inject(method = "attemptToSend", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageItem;setOrder(Lnet/minecraft/world/item/ItemStack;IIZIZLcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;)V"))
    private void attemptToSend(List<PackagingRequest> queuedRequests, CallbackInfo ci, @Local(name = "createdBox") ItemStack createdBox, @Local(name = "nextRequest") PackagingRequest nextRequest) {
        //noinspection DataFlowIssue
        if(!PRExtension.class.cast(nextRequest).deployer$isFlagged()) return;
        createdBox.set(DeployerDataComponents.ORDER_STOCK_TYPE_DATA.get(), new OrderStockTypeData(0, false));
    }
}
