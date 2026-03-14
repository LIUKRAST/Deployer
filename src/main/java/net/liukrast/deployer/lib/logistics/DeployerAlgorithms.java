package net.liukrast.deployer.lib.logistics;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeployerAlgorithms {
    private DeployerAlgorithms() {}

    public static boolean isOrderComplete(List<ItemStack> collectedPackagesForOrderId) {
        Map<Integer, Map<Integer, PackageItem.PackageOrderData>> dataMap = new HashMap<>();
        for(ItemStack box : collectedPackagesForOrderId) {
            var data = box.get(AllDataComponents.PACKAGE_ORDER_DATA);
            if(data == null) throw new IllegalStateException("All items in the list must be valid packages. Are you sure this is the right method you're calling?");
            dataMap
                    .computeIfAbsent(data.linkIndex(), k -> new HashMap<>())
                    .put(data.fragmentIndex(), data);
        }

        boolean finalLinkReached = false;
        for(int linkIndex = 0; !finalLinkReached; linkIndex++) {
            Map<Integer, PackageItem.PackageOrderData> fragments = dataMap.get(linkIndex);
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
