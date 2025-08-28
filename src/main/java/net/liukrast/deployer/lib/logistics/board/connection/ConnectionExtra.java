package net.liukrast.deployer.lib.logistics.board.connection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Represent a connection with a normal block that doesn't have a proper smart block entity.
 * */
@FunctionalInterface
public interface ConnectionExtra<T> {
    Optional<T> invalidate(Level level, BlockState state, BlockPos pos, BlockEntity be);
}
