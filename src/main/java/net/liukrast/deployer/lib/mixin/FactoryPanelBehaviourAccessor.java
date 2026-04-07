package net.liukrast.deployer.lib.mixin;


import java.lang.Integer;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FactoryPanelBehaviour.class)
public interface FactoryPanelBehaviourAccessor {
    @Accessor("timer")
    Integer timer();

    @Accessor("lastReportedLevelInStorage")
    Integer lastReportedLevelInStorage();

    @Accessor("lastReportedUnloadedLinks")
    Integer lastReportedUnloadedLinks();

    @Accessor("lastReportedPromises")
    Integer lastReportedPromises();
}
