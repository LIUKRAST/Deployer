package net.liukrast.deployer.lib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import net.liukrast.deployer.lib.event.PackageRenderEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PackageRenderer.class)
public class PackageRendererMixin {
    @Unique
    private final List<PackageRenderEvent.EntityRenderer> deployer$EXTENSIONS = PackageRenderEvent.dispatchEntity();

    @Inject(
            method = "render(Lcom/simibubi/create/content/logistics/box/PackageEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageRenderer;renderBox(Lnet/minecraft/world/entity/Entity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILdev/engine_room/flywheel/lib/model/baked/PartialModel;)V")
    )
    private void render(PackageEntity entity, float yaw, float pt, PoseStack ms, MultiBufferSource buffer, int light, CallbackInfo ci) {
        deployer$EXTENSIONS.forEach(renderer -> renderer.render(entity, yaw, pt, ms, buffer, light));
    }
}
