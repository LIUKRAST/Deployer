package net.liukrast.deployer.lib;

import net.liukrast.deployer.lib.registry.DeployerExtraPanelConnections;
import net.liukrast.deployer.lib.registry.DeployerPackets;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@Mod(DeployerConstants.MOD_ID)
public class Deployer {

    public Deployer(IEventBus bus, ModContainer container) {
        bus.register(this);
        NeoForge.EVENT_BUS.addListener(this::loadLevel);


        DeployerPanelConnections.register(bus);
        DeployerPackets.register();

        if(ModList.get().isLoaded("psic_compat")) DeployerConstants.PSIC_INSTALLED = true;
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
