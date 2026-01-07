package net.liukrast.fluid.registry;

import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import net.liukrast.fluid.TestConstants;
import net.liukrast.fluid.content.energy.BatteryChargerBlockEntity;
import net.liukrast.fluid.content.fluid.FluidPackagerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RegisterBlockEntityTypes {
    private RegisterBlockEntityTypes() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TestConstants.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPackagerBlockEntity>> FLUID_PACKAGER = BLOCK_ENTITY_TYPES.register("fluid_packager", () -> BlockEntityType.Builder.of(FluidPackagerBlockEntity::new, RegisterBlocks.FLUID_PACKAGER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryChargerBlockEntity>> BATTERY_CHARGER = BLOCK_ENTITY_TYPES.register("battery_charger", () -> BlockEntityType.Builder.of(BatteryChargerBlockEntity::new, RegisterBlocks.BATTERY_CHARGER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FLUID_PACKAGER.get(), PackagerRenderer::new);
    }
}
