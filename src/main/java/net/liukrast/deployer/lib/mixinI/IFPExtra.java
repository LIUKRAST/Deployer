package net.liukrast.deployer.lib.mixinI;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import net.minecraft.core.BlockPos;

import java.util.Map;

public interface IFPExtra {
    Map<BlockPos, FactoryPanelConnection> deployer$getExtra();
}
