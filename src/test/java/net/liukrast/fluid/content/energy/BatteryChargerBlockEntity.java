package net.liukrast.fluid.content.energy;

import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryChargerBlockEntity extends AbstractPackagerBlockEntity<Energy, EnergyStack, IEnergyStorage> {
    public BatteryChargerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    protected CapManipulationBehaviourBase<IEnergyStorage, ? extends CapManipulationBehaviourBase<?, ?>> createTargetInventory() {
        return null;
    }

    @Override
    public StockInventoryType<Energy, EnergyStack, IEnergyStorage> getStockType() {
        return null;
    }
}
