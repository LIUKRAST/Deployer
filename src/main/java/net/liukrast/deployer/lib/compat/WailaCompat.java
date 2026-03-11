package net.liukrast.deployer.lib.compat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.minecraft.world.phys.Vec3;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@SuppressWarnings("unused")
@WailaPlugin
public class WailaCompat implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addRayTraceCallback((hitResult, accessor, originalAccessor) -> {
            if(accessor instanceof BlockAccessor blockAccessor && blockAccessor.getBlock() == AllBlocks.FACTORY_GAUGE.get()) {
                Vec3 location = hitResult.getLocation();
                var slot = FactoryPanelBlock.getTargetedSlot(blockAccessor.getPosition(), blockAccessor.getBlockState(), location);
                if(blockAccessor.getBlockEntity() instanceof FactoryPanelBlockEntity fpbe) {
                    if(fpbe.panels.get(slot) instanceof AbstractPanelBehaviour apb)
                        return registration.blockAccessor().from(blockAccessor).fakeBlock(apb.getItem().getDefaultInstance()).build();
                }
            }
            return accessor;
        });
    }
}
