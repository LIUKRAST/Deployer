package net.liukrast.deployer.lib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(value = DeployerConstants.MOD_ID, dist = Dist.CLIENT)
public class DeployerClient {
    public DeployerClient(IEventBus eventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, DeployerConfig.CLIENT_SPEC);
    }
}
