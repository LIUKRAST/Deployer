package net.liukrast.fluid.registry;

import net.liukrast.fluid.TestConstants;
import net.liukrast.fluid.content.energy.BatteryChargerBlock;
import net.liukrast.fluid.content.fluid.FluidPackagerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RegisterBlocks {
    private RegisterBlocks() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TestConstants.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TestConstants.MOD_ID);

    public static final DeferredBlock<FluidPackagerBlock> FLUID_PACKAGER = BLOCKS.register("fluid_packager", () -> new FluidPackagerBlock(BlockBehaviour.Properties.of()));
    public static final DeferredBlock<BatteryChargerBlock> BATTERY_CHARGER = BLOCKS.register("battery_charger", () -> new BatteryChargerBlock(BlockBehaviour.Properties.of()));

    static {
        ITEMS.register("fluid_packager", () -> new BlockItem(FLUID_PACKAGER.get(), new Item.Properties()));
        ITEMS.register("battery_charger", () -> new BlockItem(BATTERY_CHARGER.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
