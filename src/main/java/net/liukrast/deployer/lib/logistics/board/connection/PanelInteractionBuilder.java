package net.liukrast.deployer.lib.logistics.board.connection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

public class PanelInteractionBuilder {
    private final Map<String, TriPredicate<Level, BlockPos, BlockState>> map;

    public PanelInteractionBuilder(Map<String, TriPredicate<Level, BlockPos, BlockState>> map) {
        this.map = map;
    }

    public void register(String key, TriPredicate<Level, BlockPos, BlockState> predicate) {
        map.put(key, predicate);
    }

    public void registerState(String key, Predicate<BlockState> predicate) {
        register(key, (level, pos, state) -> predicate.test(state));
    }

    public void registerEntity(String key, Predicate<@Nullable BlockEntity> predicate) {
        register(key, (level, pos, state) -> {
            var be = level.getBlockEntity(pos);
            return predicate.test(be);
        });
    }

    public void registerEntity(String key, BlockEntityType<?> type) {
        registerEntity(key, blockEntity -> {
            if(blockEntity == null) return false;
            return blockEntity.getType() == type;
        });
    }

    public void registerBlock(String key, Block block) {
        registerState(key, state -> state.is(block));
    }
}
