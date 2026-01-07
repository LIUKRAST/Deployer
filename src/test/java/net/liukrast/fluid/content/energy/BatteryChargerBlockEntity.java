package net.liukrast.fluid.content.energy;

import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.fluid.registry.RegisterBlockEntityTypes;
import net.liukrast.fluid.registry.RegisterStockInventoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryChargerBlockEntity extends AbstractPackagerBlockEntity<Energy, EnergyStack, IEnergyStorage> {
    public BatteryChargerBlockEntity(BlockPos pos, BlockState state) {
        super(RegisterBlockEntityTypes.BATTERY_CHARGER.get(), pos, state);
    }

    @Override
    protected CapManipulationBehaviourBase<IEnergyStorage, ? extends CapManipulationBehaviourBase<?, ?>> createTargetInventory() {
        return new EnergyManipulationBehaviour(this, CapManipulationBehaviourBase.InterfaceProvider.oppositeOfBlockFacing())
                .withFilter(this::supportsBlockEntity);
    }

    @Override
    public StockInventoryType<Energy, EnergyStack, IEnergyStorage> getStockType() {
        return RegisterStockInventoryTypes.ENERGY.get();
    }
}
