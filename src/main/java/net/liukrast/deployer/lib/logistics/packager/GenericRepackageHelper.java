package net.liukrast.deployer.lib.logistics.packager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.OrderStockTypeData;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.registry.DeployerDataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericRepackageHelper {
    protected Map<StockInventoryType<?,?,?>, Map<Integer, List<ItemStack>>> collectedPackages = new HashMap<>();

    public void clear() {
        collectedPackages.clear();
    }

    public boolean isFragmented(ItemStack box) {
        if(!(box.getItem() instanceof GenericPackageItem generic)) throw new IllegalArgumentException("GenericRepackageHelper must be used for generic stacks");
        return box.has(generic.getType().packageHandler().packageOrderData());
    }

    public int addPackageFragment(ItemStack box) {
        if(!(box.getItem() instanceof GenericPackageItem generic)) throw new IllegalArgumentException("GenericRepackageHelper must be used for generic stacks");
        int collectedOrderId = PackageItem.getOrderId(box);
        if(collectedOrderId == -1)
            return -1;

        List<ItemStack> collectedOrder = collectedPackages.computeIfAbsent(generic.getType(), $-> new HashMap<>())
                .computeIfAbsent(collectedOrderId, $ -> new ArrayList<>());
        collectedOrder.add(box);

        if(!isOrderComplete(generic.getType(), collectedOrderId))
            return -1;
        return collectedOrderId;
    }

    public <K,V,H> List<BigItemStack> repack(StockInventoryType<K,V,H> type, int orderId, RandomSource r) {
        List<BigItemStack> exportingPackages = new ArrayList<>();
        String address = "";
        OrderStockTypeData typeData = null;
        GenericOrderContained<V> orderContext = null;
        AbstractInventorySummary<K, V> summary = type.networkHandler().createSummary();
        var li = collectedPackages.computeIfAbsent(type, $ -> new HashMap<>()).get(orderId);
        if (li != null) {
            for (ItemStack box : li) {
                address = PackageItem.getAddress(box);
                var c = box.get(DeployerDataComponents.ORDER_STOCK_TYPE_DATA);
                if (c != null) typeData = c;
                var comp = type.packageHandler().packageOrderData();
                if (box.has(comp)) {
                    var compGot = box.get(comp);
                    if (compGot != null) {
                        GenericOrderContained<V> context = compGot.orderContext();
                        if (context != null && !context.isEmpty())
                            orderContext = context;
                    }
                }

                H contents = type.packageHandler().getContents(box);

                for (int slot = 0; slot < type.storageHandler().getSlots(contents); slot++)
                    summary.add(type.storageHandler().getStackInSlot(contents, slot));
            }
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
        }
        //Note:
        // in the future a new key instead of V might be necessary here if a library decides to use some kind of stack that has limited count
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
                    if (!type.valueHandler().hashStrategy().equals(entry, targetedEntry))
                        continue;
                }

                while (targetAmount > 0) {
                    int removedAmount = Math.min(Math.min(targetAmount, type.storageHandler().maxCountPerSlot()), type.valueHandler().getCount(entry));
                    if (removedAmount == 0)
                        continue ItemSearch;

                    V output = type.valueHandler().copyWithCount(entry, removedAmount);
                    targetAmount -= removedAmount;
                    if (targetedEntry != null)
                        type.valueHandler().setCount(targetedEntry, targetAmount);
                    type.valueHandler().setCount(entry, type.valueHandler().getCount(entry) - removedAmount);
                    outputSlots.add(output);
                }

                continue Repack;
            }
        }

        int maxSlots = type.storageHandler().getMaxPackageSlots();
        int currentSlot = 0;
        H target = type.storageHandler().create(maxSlots);

        for (V item : outputSlots) {
            type.storageHandler().setInSlot(target, currentSlot++, item, false);
            if (currentSlot < maxSlots)
                continue;
            exportingPackages.add(new BigItemStack(type.packageHandler().containing(target), 1));
            target = type.storageHandler().create(maxSlots);
            currentSlot = 0;
        }

        for (int slot = 0; slot < type.storageHandler().getSlots(target); slot++)
            if (!type.valueHandler().isEmpty(type.storageHandler().getStackInSlot(target, slot))) {
                exportingPackages.add(new BigItemStack(type.packageHandler().containing(target), 1));
                break;
            }


        for (BigItemStack box : exportingPackages) {
            PackageItem.addAddress(box.stack, address);
            if (typeData != null) box.stack.set(DeployerDataComponents.ORDER_STOCK_TYPE_DATA, typeData);
        }

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

    private <K,V,H> boolean isOrderComplete(StockInventoryType<K,V,H> type, int orderId) {
        Map<Integer, Map<Integer, GenericPackageOrderData<V>>> dataMap = new HashMap<>();
        for(ItemStack box : collectedPackages.computeIfAbsent(type, $ -> new HashMap<>()).get(orderId)) {
            var data = box.get(type.packageHandler().packageOrderData());
            assert data != null;
            dataMap
                    .computeIfAbsent(data.linkIndex(), k -> new HashMap<>())
                    .put(data.fragmentIndex(), data);
        }

        boolean finalLinkReached = false;
        for(int linkIndex = 0; !finalLinkReached; linkIndex++) {
            Map<Integer, GenericPackageOrderData<V>> fragments = dataMap.get(linkIndex);
            if(fragments == null) return false;
            boolean finalFragReached = false;
            for(int fragIndex = 0; !finalFragReached; fragIndex++) {
                var data = fragments.get(fragIndex);
                if(data == null) return false;
                if(data.isFinal()) finalFragReached = true;
                if(data.isFinalLink()) finalLinkReached = true;
            }
        }
        return true;
    }
}
