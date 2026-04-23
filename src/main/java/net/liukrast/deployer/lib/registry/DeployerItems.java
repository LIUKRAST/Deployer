package net.liukrast.deployer.lib.registry;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import net.liukrast.deployer.lib.Deployer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DeployerItems {
    private DeployerItems() {}

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Deployer.CONSTANTS.getModId());

    static {
        for(PackageStyles.PackageStyle style : DeployerPackages.STYLES) {
            ITEMS.register(style.getItemId().getPath(), () -> new PackageItem(new Item.Properties().stacksTo(1), style));
        }
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
