package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FactoryPanelBehaviour.class)
public interface FactoryPanelBehaviourAccessor {

    @Invoker("notifyRedstoneOutputs")
    void deployer$invokeNotifyRedstoneOutputs();

    @Invoker("tickRequests")
    void deployer$tickRequests();
}
