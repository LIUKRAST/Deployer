package net.liukrast.deployer.lib.registry;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.liukrast.deployer.lib.Deployer;
import net.neoforged.bus.api.IEventBus;

public class DeployerItems {
    private DeployerItems() {}

    private static final CreateRegistrate REGISTRATE = Deployer.CONSTANTS.registrate();

    static {
        for(PackageStyles.PackageStyle style : DeployerPackages.STYLES) {
            REGISTRATE.item(style.getItemId().getPath(), p -> new PackageItem(p, style))
                    .properties(p -> p.stacksTo(1));
        }
    }

    public static void register(IEventBus eventBus) {
    }
}
