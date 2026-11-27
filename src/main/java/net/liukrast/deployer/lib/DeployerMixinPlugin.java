package net.liukrast.deployer.lib;

import net.liukrast.compat.DefaultMixinPlugin;
import net.neoforged.fml.loading.LoadingModList;

public class DeployerMixinPlugin extends DefaultMixinPlugin {
    private static final String WITHOUT_FL = "net.liukrast.deployer.lib.mixin.withoutfl.";
    private static final String FL_MOD_ID = "create_factory_logistics";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if(mixinClassName.startsWith(WITHOUT_FL)) return !isFactoryLogisticsLoaded();
        return super.shouldApplyMixin(targetClassName, mixinClassName);
    }

    private static boolean isFactoryLogisticsLoaded() {
        var mods = LoadingModList.get();
        return mods != null && mods.getModFileById(FL_MOD_ID) != null;
    }
}
