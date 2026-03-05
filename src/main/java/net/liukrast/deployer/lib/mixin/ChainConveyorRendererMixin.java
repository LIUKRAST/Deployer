package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.liukrast.deployer.lib.event.PackageRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mixin(ChainConveyorRenderer.class)
public class ChainConveyorRendererMixin {
    @Unique
    private static final List<PackageRenderEvent.SuperByteBufferFactory> deployer$EXTENSIONS = PackageRenderEvent.dispatchChainConveyor();

    @Definition(id = "SuperByteBuffer", type = SuperByteBuffer.class)
    @Expression("new SuperByteBuffer[]{?,?}")
    @ModifyExpressionValue(method = "renderBox", at = @At("MIXINEXTRAS:EXPRESSION"))
    private SuperByteBuffer[] renderBox(
            SuperByteBuffer[] original,
            @Local(argsOnly = true) ChainConveyorBlockEntity be,
            @Local(argsOnly = true) ChainConveyorPackage box,
            @Local(argsOnly = true) float partialTicks
            ) {
        List<SuperByteBuffer> allAdded = new ArrayList<>();
        for(var ext : deployer$EXTENSIONS) {
            SuperByteBuffer[] buffers = ext.create(be, box, partialTicks);
            if(buffers != null && buffers.length > 0) {
                Collections.addAll(allAdded, buffers);
            }
        }

        SuperByteBuffer[] copy = Arrays.copyOf(original, original.length + allAdded.size());
        for (int i = 0; i < allAdded.size(); i++) {
            copy[original.length + i] = allAdded.get(i);
        }

        return copy;
    }
}
