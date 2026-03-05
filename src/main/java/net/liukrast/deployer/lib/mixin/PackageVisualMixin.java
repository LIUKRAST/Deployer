package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import net.liukrast.deployer.lib.event.PackageVisualEvent;
import net.liukrast.deployer.lib.helper.client.PackageVisualExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(PackageVisual.class)
public abstract class PackageVisualMixin extends AbstractEntityVisual<PackageEntity> {
    @Unique
    private static final List<PackageVisualEvent.EntityFactory> deployer$EXTENSIONS = PackageVisualEvent.dispatchEntity();

    @Unique
    private final List<PackageVisualExtension.Entity> deployer$extensions = new ArrayList<>();

    public PackageVisualMixin(VisualizationContext ctx, PackageEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private void init(VisualizationContext ctx, PackageEntity entity, float partialTick, CallbackInfo ci) {
        deployer$extensions.addAll(deployer$EXTENSIONS.stream()
                .filter(factory -> factory.validForPackage(entity))
                .map(factory -> factory.create(ctx, entity, partialTick))
                .toList())
        ;
    }

    @Inject(method = "beginFrame", at = @At("TAIL"))
    private void beginFrame(DynamicVisual.Context ctx, CallbackInfo ci) {
        deployer$extensions.forEach(ext -> ext.beginFrame(ctx, entity));
    }

    @Inject(method = "animate", at = @At("TAIL"))
    private void animate(float partialTick, CallbackInfo ci,
                         @Local(name = "yaw") float yaw,
                         @Local(name = "x") float x,
                         @Local(name = "y") float y,
                         @Local(name = "z") float z,
                         @Local(name = "xNudge") float xNudge,
                         @Local(name = "yNudge") float yNudge,
                         @Local(name = "zNudge") float zNudge
                         ) {
        deployer$extensions.forEach(e -> {
            TransformedInstance[] instances = e.createBuffer(entity);
            for(var i : instances) {
                i.setIdentityTransform()
                        .translate(x - 0.5 + xNudge, y + yNudge, z - 0.5 + zNudge)
                        .rotateYCenteredDegrees(-yaw - 90)
                        .light(computePackedLight(partialTick))
                        .setChanged();
            }
        });
    }

    @Inject(method = "_delete", at = @At("TAIL"))
    private void _delete(CallbackInfo ci) {
        deployer$extensions.forEach(PackageVisualExtension.Entity::_delete);
    }
}
