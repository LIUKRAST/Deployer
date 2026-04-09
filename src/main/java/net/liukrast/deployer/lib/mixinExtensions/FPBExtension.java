package net.liukrast.deployer.lib.mixinExtensions;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.logistics.board.connection.ProvidesConnection;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FPBExtension {
    Map<BlockPos, FactoryPanelConnection> deployer$getExtra();
    Set<PanelConnection<?>> deployer$getInputConnections();
    Set<PanelConnection<?>> deployer$getOutputConnections();
    <T> Optional<T> deployer$getConnectionValue(PanelConnection<T> connection);
}
