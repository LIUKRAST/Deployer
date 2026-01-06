package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.GenericPackageItem;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixinExtensions.PRHExtension;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.compress.utils.Lists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(PackageRepackageHelper.class)
public abstract class PackageRepackageHelperMixin implements PRHExtension {

    @Unique
    private final Map<StockInventoryType<?,?,?>, Map<Integer, List<ItemStack>>> deployer$collectedPackages = new HashMap<>();

    @Inject(method = "clear", at = @At("TAIL"))
    private void clear(CallbackInfo ci) {
        deployer$collectedPackages.values().forEach(Map::clear);
    }

    @Inject(method = "isFragmented", at = @At("RETURN"), cancellable = true)
    private void isFragmented(ItemStack box, CallbackInfoReturnable<Boolean> cir) {
        if(!(box.getItem() instanceof GenericPackageItem generic)) return;
        cir.setReturnValue(box.has(generic.getType().packageHandler().packageOrderData()));
    }

    @ModifyVariable(method = "addPackageFragment", at = @At("STORE"), name = "collectedOrder")
    private List<ItemStack> addPackageFragment(List<ItemStack> value, @Local(argsOnly = true, name = "arg1") ItemStack box, @Local(name = "collectedOrderId") int collectedOrderId) {
        if(!(box.getItem() instanceof GenericPackageItem item)) return value;
        return deployer$collectedPackages
                .computeIfAbsent(item.getType(), $ -> new HashMap<>())
                .computeIfAbsent(collectedOrderId, $ -> Lists.newArrayList());
    }

    @WrapOperation(method = "addPackageFragment", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;isOrderComplete(I)Z"))
    private boolean addPackageFragment(PackageRepackageHelper instance, int orderId, Operation<Boolean> original, @Local(argsOnly = true, name = "arg1") ItemStack box) {
        if(!(box.getItem() instanceof GenericPackageItem item)) return original.call(instance, orderId);
        return deployer$isOrderComplete(item.getType(), orderId);
    }

    @Inject(method = "repack", at = @At("RETURN"))
    private void repack(int orderId, RandomSource r, CallbackInfoReturnable<List<BigItemStack>> cir) {
        var list = cir.getReturnValue();
        for(StockInventoryType<?,?,?> type : deployer$collectedPackages.keySet()) {
            list.addAll(deployer$repack(type, orderId, r));
        }
        //TODO: Create composite packages here
    }

    @Override
    public <K, V, H> List<BigItemStack> deployer$repack(StockInventoryType<K, V, H> type, int orderId, RandomSource r) {
        List<BigItemStack> exportingPackages = new ArrayList<>();
        String address = "";
        GenericOrderContained<V> orderContext = null;
        AbstractInventorySummary<K, V> summary = type.networkHandler().create();

        for (ItemStack box : deployer$collectedPackages.get(type).get(orderId)) {
            address = PackageItem.getAddress(box);
            var comp = type.packageHandler().packageOrderData();
            if (box.has(comp)) {
                @SuppressWarnings("DataFlowIssue")
                GenericOrderContained<V> context = box.get(comp).orderContext();
                if (context != null && !context.isEmpty())
                    orderContext = context;
            }

            H contents = type.packageHandler().getContents(box);

            for (int slot = 0; slot < type.storageHandler().getSlots(contents); slot++)
                summary.add(type.storageHandler().getStackInSlot(contents, slot));
        }

        List<V> orderedStacks = new ArrayList<>();
        if (orderContext != null) {
            // Currently, Deployer does not support any "ordering"
            // system like recipes for items, but it will be implemented in a future version
            List<BigItemStack> packagesSplitByRecipe = List.of(); //repackBasedOnRecipes(summary, orderContext, address, r);
            exportingPackages.addAll(packagesSplitByRecipe);

            //noinspection ConstantValue
            if (packagesSplitByRecipe.isEmpty())
                for (V stack : orderContext.stacks())
                    orderedStacks.add(type.valueHandler().copy(stack));
            //noinspection ConstantValue
            if(packagesSplitByRecipe.isEmpty())
                for(V stack : orderContext.stacks())
                    orderedStacks.add(type.valueHandler().copy(stack));
        }

        List<V> allItems = summary.getStacks();
        List<V> outputSlots = new ArrayList<>();

        Repack:
        while (true) {
            allItems.removeIf(e -> type.valueHandler().getCount(e) == 0);
            if (allItems.isEmpty())
                break;

            V targetedEntry = null;
            if (!orderedStacks.isEmpty())
                targetedEntry = orderedStacks.removeFirst();

            ItemSearch:
            for (V entry : allItems) {
                int targetAmount = type.valueHandler().getCount(entry);
                if (targetAmount == 0)
                    continue;
                if (targetedEntry != null) {
                    targetAmount = type.valueHandler().getCount(targetedEntry);
                    if(!type.valueHandler().equalsIgnoreCount(entry, targetedEntry))
                        continue;
                }

                while (targetAmount > 0) {
                    //TODO: Check if maxCountPerSlot is the thing
                    int removedAmount = Math.min(Math.min(targetAmount, type.storageHandler().maxCountPerSlot()), type.valueHandler().getCount(entry));
                    if (removedAmount == 0)
                        continue ItemSearch;

                    V output = type.valueHandler().copyWithCount(entry, removedAmount);
                    targetAmount -= removedAmount;
                    if(targetedEntry != null)
                        type.valueHandler().setCount(targetedEntry, targetAmount);
                    type.valueHandler().setCount(entry, type.valueHandler().getCount(entry) - removedAmount);
                    outputSlots.add(output);
                }

                continue Repack;
            }
        }

        int currentSlot = 0;
        H target = type.storageHandler().create(type.storageHandler().getMaxPackageSlots());

        for (V item : outputSlots) {
            type.storageHandler().setInSlot(target, currentSlot++, item, false);
            if (currentSlot < PackageItem.SLOTS)
                continue;
            exportingPackages.add(new BigItemStack(type.packageHandler().containing(target), 1));
            target = type.storageHandler().create(type.storageHandler().getMaxPackageSlots());
            currentSlot = 0;
        }

        for (int slot = 0; slot < type.storageHandler().getSlots(target); slot++)
            if (!type.valueHandler().isEmpty(type.storageHandler().getStackInSlot(target, slot))) {
                exportingPackages.add(new BigItemStack(type.packageHandler().containing(target), 1));
                break;
            }


        for (BigItemStack box : exportingPackages)
            PackageItem.addAddress(box.stack, address);

        for (int i = 0; i < exportingPackages.size(); i++) {
            BigItemStack box = exportingPackages.get(i);
            boolean isFinal = i == exportingPackages.size() - 1;
            GenericOrderContained<V> outboundOrderContext = isFinal && orderContext != null ? orderContext : null;
            if (PackageItem.getOrderId(box.stack) == -1)
                //PackageItem.setOrder(box.stack, orderId, 0, true, 0, true, outboundOrderContext);
                type.packageHandler().setOrder(box.stack, orderId, 0, true, 0, true, outboundOrderContext);
        }

        return exportingPackages;
    }

    @Override
    public <K,V,H> boolean deployer$isOrderComplete(StockInventoryType<K, V, H> type, int orderId) {
        boolean finalLinkReached = false;
        Links:
        for (int linkCounter = 0; linkCounter < 1000; linkCounter++) {
            if (finalLinkReached)
                break;
            Packages:
            for (int packageCounter = 0; packageCounter < 1000; packageCounter++) {
                for (ItemStack box : deployer$collectedPackages.computeIfAbsent(type, $ -> new HashMap<>()).get(orderId)) {
                    GenericPackageOrderData<V> data = box.get(type.packageHandler().packageOrderData());
                    //noinspection DataFlowIssue
                    if (linkCounter != data.linkIndex())
                        continue;
                    if (packageCounter != data.fragmentIndex())
                        continue;
                    finalLinkReached = data.isFinalLink();
                    if (data.isFinal())
                        continue Links;
                    continue Packages;
                }
                return false;
            }
        }
        return true;
    }

    @ModifyExpressionValue(method = "repackBasedOnRecipes", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/stockTicker/PackageOrder;stacks()Ljava/util/List;", ordinal = 1))
    private List<BigItemStack> repackBasedOnRecipes(List<BigItemStack> original) {
        List<ItemStack> copied = original.stream().map(big -> big.stack.copy()).toList();
        List<ItemStack> result = NonNullList.withSize(copied.size(), ItemStack.EMPTY);
        for(var stack : copied) {
            for(int i = 0; i < result.size(); i++) {
                var slot = result.get(i);
                if(slot.isEmpty()) {
                    result.set(i, stack);
                    break;
                } else if(ItemStack.isSameItemSameComponents(stack, slot)) {
                    int canPut = slot.getMaxStackSize() - slot.getCount();
                    slot.setCount(slot.getCount() + Mth.clamp(stack.getCount(),0,canPut));
                    stack.setCount(Math.max(stack.getCount()-canPut, 0));
                    if(stack.getCount() == 0) break;
                }
            }
        }
        return result.stream().map(BigItemStack::new).toList();
    }

    @WrapOperation(method = "repackBasedOnRecipes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;", ordinal = 0))
    private ItemStack repackBasedOnRecipes(ItemStack instance, int i, Operation<ItemStack> original) {
        return instance.copy();
    }
}
