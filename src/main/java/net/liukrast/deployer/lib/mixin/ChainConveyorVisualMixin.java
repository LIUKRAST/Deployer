package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.helper.client.PackageVisualExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mixin(ChainConveyorVisual.class)
public abstract class ChainConveyorVisualMixin extends SingleAxisRotatingVisual<ChainConveyorBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {

    @Unique
    private final List<PackageVisualExtension.ChainConveyor> deployer$extensions = new ArrayList<>();

    public ChainConveyorVisualMixin(VisualizationContext context, ChainConveyorBlockEntity blockEntity, float partialTick, Model model) {
        super(context, blockEntity, partialTick, model);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(VisualizationContext context, ChainConveyorBlockEntity blockEntity, float partialTick, CallbackInfo ci) {
        deployer$extensions.addAll(ClientRegisterHelpers.getChainVisuals()
                .map(factory -> factory.create(context, blockEntity, partialTick))
                .toList()
        );
    }

    @Inject(method = "beginFrame", at = @At("HEAD"))
    private void beginFrame(DynamicVisual.Context ctx, CallbackInfo ci) {
        deployer$extensions.forEach(PackageVisualExtension.ChainConveyor::beginFrame$start);
    }

    @Inject(method = "beginFrame", at = @At("RETURN"))
    private void beginFrame$1(DynamicVisual.Context ctx, CallbackInfo ci) {
        deployer$extensions.forEach(PackageVisualExtension.ChainConveyor::beginFrame$end);
    }

    @Definition(id = "TransformedInstance", type = TransformedInstance.class)
    @Expression("new TransformedInstance[]{?,?}")
    @ModifyExpressionValue(method = "setupBoxVisual", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private TransformedInstance[] setupBoxVisual(
            TransformedInstance[] original,
            @Local(argsOnly = true) ChainConveyorPackage box,
            @Local(name = "physicsData") ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData,
            @Share("post_processor") LocalRef<PackageVisualExtension.PostProcessor> postProcessor
    ) {

        postProcessor.set(new PackageVisualExtension.PostProcessor());

        List<TransformedInstance> allAdded = new ArrayList<>();

        for (var ext : deployer$extensions) {
            TransformedInstance[] buffers = ext.createBuffer(box, physicsData, postProcessor.get());
            if (buffers != null && buffers.length > 0) {
                Collections.addAll(allAdded, buffers);
            }
        }

        TransformedInstance[] copy = Arrays.copyOf(original, original.length + allAdded.size());
        for (int i = 0; i < allAdded.size(); i++) {
            copy[original.length + i] = allAdded.get(i);
        }

        return copy;
    }

    @Inject(method = "setupBoxVisual", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/instance/TransformedInstance;uncenter()Ldev/engine_room/flywheel/lib/transform/Translate;"))
    private void setupBoxVisual(
            ChainConveyorBlockEntity be,
            ChainConveyorPackage box,
            float partialTicks,
            CallbackInfo ci,
            @Local(name = "buf") TransformedInstance buf,
            @Share("post_processor") LocalRef<PackageVisualExtension.PostProcessor> postProcessor
    ) {
        var post = postProcessor.get();
        if(post == null) return;
        post.consume(buf);
    }

    @Inject(method = "_delete", at = @At("RETURN"))
    private void _delete(CallbackInfo ci) {
        deployer$extensions.forEach(PackageVisualExtension.ChainConveyor::_delete);
    }
}
