package net.liukrast.deployer.lib;

import com.mojang.blaze3d.vertex.PoseStack;
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

@Mod(DeployerConstants.MOD_ID)
public class Deployer {

    public Deployer(IEventBus bus, ModContainer container) {
        bus.register(this);
        NeoForge.EVENT_BUS.addListener(this::loadLevel);
        DeployerPanelConnections.register(bus);
        DeployerDataComponents.register(bus);
        DeployerItems.register(bus);
        DeployerPartialModels.init();
        DeployerPackets.register();
        container.registerConfig(ModConfig.Type.SERVER, DeployerConfig.Server.SPEC);

        if(ModList.get().isLoaded("psic_compat")) DeployerConstants.PSIC_INSTALLED = true;
    }

    public static void test(PoseStack ms) {
        ms.translate(8/16f,0,0.5);
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
