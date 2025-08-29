package net.liukrast.deployer.lib.mixinExtension;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.Internal
public interface IFPExtension {
    Map<BlockPos, FactoryPanelConnection> deployer$getExtra();
}
