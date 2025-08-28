package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FactoryPanelBehaviour.class)
public interface FactoryPanelBehaviourAccessor {
    @Accessor("timer")
    int timer();

    @Accessor("lastReportedLevelInStorage")
    int lastReportedLevelInStorage();

    @Accessor("lastReportedUnloadedLinks")
    int lastReportedUnloadedLinks();

    @Accessor("lastReportedPromises")
    int lastReportedPromises();
}
