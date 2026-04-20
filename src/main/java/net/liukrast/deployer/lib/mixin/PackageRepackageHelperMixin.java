package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import net.liukrast.deployer.lib.logistics.OrderStockTypeData;
import net.liukrast.deployer.lib.registry.DeployerDataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PackageRepackageHelper.class)
public class PackageRepackageHelperMixin {
    @Inject(method = "repack", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageItem;getAddress(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/String;"))
    private void repack(
            int orderId,
            RandomSource r,
            CallbackInfoReturnable<List<BigItemStack>> cir,
            @Share("stock_data") LocalRef<OrderStockTypeData> extraData,
            @Local(name = "box") ItemStack box
    ) {
        var order = box.getOrDefault(DeployerDataComponents.ORDER_STOCK_TYPE_DATA, OrderStockTypeData.EMPTY);
        extraData.set(order);
    }

    @Inject(method = "repack", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageItem;addAddress(Lnet/minecraft/world/item/ItemStack;Ljava/lang/String;)V"))
    private void repack(
            int orderId,
            RandomSource r,
            CallbackInfoReturnable<List<BigItemStack>> cir,
            @Share("stock_data") LocalRef<OrderStockTypeData> extraData,
            @Local(name = "box") BigItemStack box
    ) {
        if(extraData.get() != null) box.stack.set(DeployerDataComponents.ORDER_STOCK_TYPE_DATA, extraData.get());
    }
}
