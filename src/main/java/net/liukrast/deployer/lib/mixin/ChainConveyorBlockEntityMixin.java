package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(ChainConveyorBlockEntity.class)
public abstract class ChainConveyorBlockEntityMixin extends KineticBlockEntity {

    @Shadow
    public Set<BlockPos> connections;

    public ChainConveyorBlockEntityMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @ModifyExpressionValue(
            method = "canAcceptMorePackages",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int canAcceptMorePackages(int original) {
        int c = 0;
        for(var conn : connections) {
            if(!(level.getBlockEntity(worldPosition.offset(conn)) instanceof ChainConveyorBlockEntity ccbe)) continue;
            c+= ccbe.getTravellingPackages().size();
        }

        return original + c + 1;
    }
}
