package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FactoryPanelBehaviour.class)
public interface FactoryPanelBehaviourAccessor {
    @Accessor("timer")
    int deployer$getTimer();

    @Accessor("timer")
    void deployer$setTimer(int value);

    @Accessor("lastReportedLevelInStorage")
    int deployer$getLastReportedLevelInStorage();

    @Accessor("lastReportedLevelInStorage")
    void deployer$setLastReportedLevelInStorage(int value);

    @Accessor("lastReportedUnloadedLinks")
    int deployer$getLastReportedUnloadedLinks();

    @Accessor("lastReportedUnloadedLinks")
    void deployer$setLastReportedUnloadedLinks(int value);

    @Accessor("lastReportedPromises")
    int deployer$getLastReportedPromises();

    @Accessor("lastReportedPromises")
    void deployer$setLastReportedPromises(int value);

    @Invoker("tickStorageMonitor")
    void deployer$invokeTickStorageMonitor();

    @Invoker("getPromiseExpiryTimeInTicks")
    int deployer$invokeGetPromiseExpiryTimeInTicks();

    @Invoker("notifyRedstoneOutputs")
    void deployer$invokeNotifyRedstoneOutputs();
}
