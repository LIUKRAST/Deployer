package net.liukrast.deployer.lib.logistics.board.connection;

import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Represents a connection that can send {@link T} through panels
 */
public class PanelConnection<T> {
    //region Attributes
    private final Map<Block, ConnectionExtra<T>> extraConnections = new HashMap<>();
    //TODO: Implement partial ticks and panel data
    private final Function<T, ConnectionLine> colorProvider;
    //endregion


    public PanelConnection(Function<T, ConnectionLine> colorProvider) {
        this.colorProvider = colorProvider;
    }

    /**
     * Registers a block-specific listener that supplies custom connection logic.
     * The listener will only apply to the provided blocks.
     * Usually, these are registered on a {@link net.neoforged.neoforge.event.level.LevelEvent.Load}.
     * See {@link net.liukrast.deployer.lib.Deployer#loadLevel(LevelEvent.Load)} for reference
     *
     * @param supplier    the connection logic for matching blocks
     * @param validBlocks the blocks supported by this listener
     *
     * @implNote If no blocks are specified, an error is logged and the listener is ignored.
     */
    @SuppressWarnings("JavadocReference")
    public void addListener(ConnectionExtra<T> supplier, Block... validBlocks) {
        if(validBlocks.length == 0) DeployerConstants.LOGGER.error("Registered panel connection listener without any blocks. {}", this);
        for(var block : validBlocks) {
            extraConnections.put(block, supplier);
        }
    }

    /**
     * Determines the logical facing direction of a block, based on its placement properties.
     * This is used to infer panel connection context such as orientation, default I/O direction
     * or implied adjacency behavior.
     *
     * @param state the block state to analyze
     * @return the facing direction derived from the state, or {@code null} if none could be determined
     */
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

    /**
     * Retrieves a registered block-specific listener for the given block.
     *
     * @param block the block being queried
     * @return the associated listener, or {@code null} if the block has no extra logic
     */
    public ConnectionExtra<T> getListener(Block block) {
        return extraConnections.get(block);
    }

    public ConnectionLine getColor(T value) {
        return colorProvider.apply(value);
    }

    /**
     * @return the registry identifier of this connection, or {@code "unregistered"} if not registered
     */
    @Override
    public String toString() {
        var id = DeployerRegistries.PANEL_CONNECTION.getKey(this);
        return id == null ? "unregistered" : id.toString();
    }
}
