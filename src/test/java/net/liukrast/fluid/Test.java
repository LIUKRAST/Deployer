package net.liukrast.fluid;

import net.liukrast.fluid.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.stream.Stream;

@Mod(TestConstants.MOD_ID)
public class Test {
    public Test(IEventBus eventBus) {
        RegisterBlockEntityTypes.register(eventBus);
        RegisterBlocks.register(eventBus);
        RegisterDataComponents.register(eventBus);
        RegisterItems.register(eventBus);
        RegisterStockInventoryTypes.register(eventBus);
        eventBus.register(this);
        RegisterPackageStyles.init();
    }

    @SubscribeEvent
    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        RegisterBlockEntityTypes.registerRenderers(event);
    }

    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RegisterBlockEntityTypes.FLUID_PACKAGER.get(),
                (be, context) -> be.inventory
        );

        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, $) -> new ComponentEnergyStorage(stack, RegisterDataComponents.BATTERY_CONTENTS.get(), 1000, 1000, 1000),
                Stream.concat(RegisterItems.RARE_BATTERIES.stream(), RegisterItems.STANDARD_BATTERIES.stream()).toArray(DeferredItem[]::new)
        );
    }
}
