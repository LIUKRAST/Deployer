package net.liukrast.fluid.registry;

import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.liukrast.fluid.TestConstants;
import net.liukrast.fluid.content.energy.Energy;
import net.liukrast.fluid.content.energy.EnergyStack;
import net.liukrast.fluid.content.energy.EnergyStockInventoryType;
import net.liukrast.fluid.content.fluid.FluidStockInventoryType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RegisterStockInventoryTypes {
    private RegisterStockInventoryTypes() {}

    public static final DeferredRegister<StockInventoryType<?,?,?>> STOCK_INVENTORY_TYPES = DeferredRegister.create(DeployerRegistries.STOCK_INVENTORY, TestConstants.MOD_ID);

    public static final DeferredHolder<StockInventoryType<?,?,?>, StockInventoryType<Fluid, FluidStack, IFluidHandler>> FLUID = STOCK_INVENTORY_TYPES.register("fluid", FluidStockInventoryType::new);
    public static final DeferredHolder<StockInventoryType<?,?,?>, StockInventoryType<Energy, EnergyStack, IEnergyStorage>> ENERGY = STOCK_INVENTORY_TYPES.register("energy", EnergyStockInventoryType::new);

    public static void register(IEventBus eventBus) {
        STOCK_INVENTORY_TYPES.register(eventBus);
    }
}
