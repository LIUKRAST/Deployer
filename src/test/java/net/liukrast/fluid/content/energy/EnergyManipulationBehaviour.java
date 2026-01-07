package net.liukrast.fluid.content.energy;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyManipulationBehaviour extends CapManipulationBehaviourBase<IEnergyStorage, EnergyManipulationBehaviour> {
    public static final BehaviourType<EnergyManipulationBehaviour> OBSERVE = new BehaviourType<>();
    private BehaviourType<EnergyManipulationBehaviour> behaviourType;

    public EnergyManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
        this(OBSERVE, be, target);
    }

    private EnergyManipulationBehaviour(BehaviourType<EnergyManipulationBehaviour> type, SmartBlockEntity be, InterfaceProvider target) {
        super(be, target);
        this.behaviourType = type;
    }

    @Override
    protected BlockCapability<IEnergyStorage, Direction> capability() {
        return Capabilities.EnergyStorage.BLOCK;
    }

    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }
}
