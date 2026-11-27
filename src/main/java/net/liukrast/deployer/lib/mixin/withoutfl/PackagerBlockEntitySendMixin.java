package net.liukrast.deployer.lib.mixin.withoutfl;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.liukrast.deployer.lib.logistics.OrderStockTypeData;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
import net.liukrast.deployer.lib.registry.DeployerDataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PackagerBlockEntity.class)
public class PackagerBlockEntitySendMixin {
    @Inject(
            method = "attemptToSend",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/box/PackageItem;setOrder(Lnet/minecraft/world/item/ItemStack;IIZIZLcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;)V"
            )
    )
    private void deployer$flagMixedOrderBox(List<PackagingRequest> queuedRequests, CallbackInfo ci, @Local(name = "createdBox") ItemStack createdBox, @Local(name = "nextRequest") PackagingRequest nextRequest) {
        //noinspection DataFlowIssue
        if(!PRExtension.class.cast(nextRequest).deployer$isFlagged()) return;
        createdBox.set(DeployerDataComponents.ORDER_STOCK_TYPE_DATA.get(), new OrderStockTypeData(0, false));
    }
}
