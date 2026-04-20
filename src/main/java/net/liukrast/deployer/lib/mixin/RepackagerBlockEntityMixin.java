package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.OrderStockTypeData;
import net.liukrast.deployer.lib.logistics.packager.GenericPackageItem;
import net.liukrast.deployer.lib.logistics.packager.GenericRepackageHelper;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.mixinExtensions.RBEExtension;
import net.liukrast.deployer.lib.registry.DeployerDataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RepackagerBlockEntity.class)
public class RepackagerBlockEntityMixin implements RBEExtension {

    @Unique
    private GenericRepackageHelper deployer$genericRepackageHelper = new GenericRepackageHelper();

    @Inject(
            method = "attemptToRepackage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;clear()V"
            )
    )
    private void attemptToRepackage(IItemHandler targetInv, CallbackInfo ci) {
        deployer$genericRepackageHelper.clear();
    }


    // SAVE IN SHARE TO
    @Inject(
            method = "attemptToRepackage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/box/PackageItem;isPackage(Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private void attemptToRepackage(
            IItemHandler targetInv,
            CallbackInfo ci,
            @Local(name = "extracted") ItemStack extracted,
            @Share("use_generic") LocalRef<StockInventoryType<?,?,?>> ref
    ) {
        if(extracted.getItem() instanceof GenericPackageItem packageItem)
            ref.set(packageItem.getType());
        else ref.set(null);
    }

    @WrapOperation(
            method = "attemptToRepackage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;isFragmented(Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean attemptToRepackage(
            PackageRepackageHelper instance,
            ItemStack box,
            Operation<Boolean> original,
            @Share("use_generic") LocalRef<StockInventoryType<?,?,?>> ref
    ) {
        if(ref.get() != null) return deployer$genericRepackageHelper.isFragmented(box);
        return original.call(instance, box);
    }

    @WrapOperation(
            method = "attemptToRepackage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;addPackageFragment(Lnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int attemptToRepackage$1(
            PackageRepackageHelper instance,
            ItemStack box,
            Operation<Integer> original,
            @Share("use_generic") LocalRef<StockInventoryType<?,?,?>> ref
    ) {
        if(ref.get() != null) return deployer$genericRepackageHelper.addPackageFragment(box);
        return original.call(instance, box);
    }

    @WrapOperation(
            method = "attemptToRepackage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;repack(ILnet/minecraft/util/RandomSource;)Ljava/util/List;"
            )
    )
    private List<BigItemStack> attemptToRepackage$2(
            PackageRepackageHelper instance,
            int orderId,
            RandomSource r,
            Operation<List<BigItemStack>> original,
            @Share("use_generic") LocalRef<StockInventoryType<?,?,?>> ref
    ) {
        if(ref.get() != null) return deployer$genericRepackageHelper.repack(ref.get(), orderId, r);
        return original.call(instance, orderId, r);
    }

    @Inject(
            method = "attemptToRepackage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;addPackageFragment(Lnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private void attemptToRepackage(
            IItemHandler targetInv,
            CallbackInfo ci,
            @Local(name = "extracted") ItemStack extracted,
            @Share("typeIndex") LocalIntRef typeIndex
    ) {
        typeIndex.set(extracted.getOrDefault(DeployerDataComponents.ORDER_STOCK_TYPE_DATA, OrderStockTypeData.EMPTY).index());
    }


    @Definition(id = "getOrderId", method = "Lcom/simibubi/create/content/logistics/box/PackageItem;getOrderId(Lnet/minecraft/world/item/ItemStack;)I")
    @Definition(id = "extracted", local = @Local(type = ItemStack.class, name = "extracted"))
    @Definition(id = "completedOrderId", local = @Local(type = int.class, name = "completedOrderId"))
    @Expression("getOrderId(extracted) != completedOrderId")
    @ModifyExpressionValue(method = "attemptToRepackage", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean attemptToRepackage(
            boolean original,
            @Local(name = "extracted") ItemStack extracted,
            @Share("typeIndex") LocalIntRef typeIndex
    ) {
        int index = extracted.getOrDefault(DeployerDataComponents.ORDER_STOCK_TYPE_DATA, OrderStockTypeData.EMPTY).index();
        return index != typeIndex.get() || original;
    }

    @Override
    public void deployer$setGenericRepackageHelper(GenericRepackageHelper genericRepackageHelper) {
        this.deployer$genericRepackageHelper = genericRepackageHelper;
    }

    @Override
    public GenericRepackageHelper deployer$getGenericRepackageHelper() {
        return deployer$genericRepackageHelper;
    }
}
