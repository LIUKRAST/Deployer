package net.liukrast.deployer.lib.logistics.board.connection;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A panel connection. Register it through {@link }
 * */
public class PanelConnection<T> {
    private final Map<Block, ConnectionExtra<T>> extraConnections = new HashMap<>();
    private final Function<FactoryPanelBehaviour, T> defaultProvider;

    public PanelConnection(Function<FactoryPanelBehaviour, T> defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public void addListener(ConnectionExtra<T> supplier, Block... validBlocks) {
        if(validBlocks.length == 0) DeployerConstants.LOGGER.error("Registered panel connection listener without any blocks. {}", this);
        for(var block : validBlocks) {
            extraConnections.put(block, supplier);
        }
    }

    public static Direction makeContext(BlockState state) {
        if(state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            var attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
            return switch (attachFace) {
                case CEILING -> Direction.DOWN;
                case FLOOR -> Direction.UP;
                case WALL -> state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            };
        } else if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        else if(state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING);
        return null;
    }

    public ConnectionExtra<T> getListener(Block block) {
        return extraConnections.get(block);
    }

    public T getDefault(FactoryPanelBehaviour behaviour) {
        return defaultProvider.apply(behaviour);
    }

    @Override
    public String toString() {
        var id = DeployerRegistries.PANEL_CONNECTION.getKey(this);
        return id == null ? "unregistered" : id.toString();
    }
}
