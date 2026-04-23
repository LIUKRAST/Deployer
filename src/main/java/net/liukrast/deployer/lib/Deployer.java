package net.liukrast.deployer.lib;

import net.liukrast.deployer.lib.helper.Constants;
import net.liukrast.deployer.lib.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@Mod("deployer")
public class Deployer {

    public static boolean PSIC_INSTALLED;

    public static final Constants CONSTANTS = Constants.of("deployer");

    public Deployer(IEventBus bus, ModContainer container) {
        bus.register(this);
        NeoForge.EVENT_BUS.addListener(this::loadLevel);
        DeployerPanelConnections.register(bus);
        DeployerDataComponents.register(bus);
        DeployerItems.register(bus);
        DeployerPartialModels.init();
        DeployerPackets.register();
        container.registerConfig(ModConfig.Type.SERVER, DeployerConfig.Server.SPEC);

        if(ModList.get().isLoaded("psic_compat")) PSIC_INSTALLED = true;
    }

    /* MOD BUS EVENTS */
    @SubscribeEvent
    private void newRegistry(NewRegistryEvent event) {
        DeployerRegistries.init(event);
    }

    /* NEO EVENTS */
    private void loadLevel(LevelEvent.Load event) {
        DeployerExtraPanelConnections.register();
    }
}
