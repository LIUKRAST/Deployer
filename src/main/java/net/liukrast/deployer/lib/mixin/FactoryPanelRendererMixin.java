package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.factoryBoard.*;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.ColoredFactoryPanelSupportBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FactoryPanelRenderer.class)
public class FactoryPanelRendererMixin {

    /* Allows abstract panels to have their own render system and decides whether a bulb should be rendered or not */
    @ModifyExpressionValue(
            method = "renderSafe(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;getAmount()I")
    )
    private int renderSafe(int original, @Local(name = "behaviour") FactoryPanelBehaviour behaviour, @Local(argsOnly = true) float partialTicks, @Local(argsOnly = true)PoseStack ms, @Local(argsOnly = true)MultiBufferSource buffer, @Local(argsOnly = true, ordinal = 0) int light, @Local(argsOnly = true, ordinal = 1) int overlay) {
        if (behaviour instanceof AbstractPanelBehaviour abstractPanel) {
            ms.pushPose();
            ClientRegisterHelpers.getPanelRenderers().forEach(renderer -> renderer.render(abstractPanel, partialTicks, ms, buffer, light, overlay));
            ms.popPose();
            return abstractPanel.shouldRenderBulb(original > 0) ? 1 : 0;
        } else return original;
    }

    /* Render extra connections */
    @Inject(
            method = "renderSafe(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;", ordinal = 0)
    )
    private void renderSafe(FactoryPanelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci, @Local(name = "behaviour") FactoryPanelBehaviour behaviour) {
        for(FactoryPanelConnection connection : ((FPBExtension)behaviour).deployer$getExtra().values())
            FactoryPanelRenderer.renderPath(behaviour, connection, partialTicks, ms, buffer, light, overlay);
    }

    /* Render paths */
    @ModifyArg(method = "renderPath", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperByteBuffer;color(I)Lnet/createmod/catnip/render/SuperByteBuffer;"))
    private static int renderPath(int color, @Local(argsOnly = true) FactoryPanelBehaviour behaviour, @Local(argsOnly = true) FactoryPanelConnection connection, @Local(name = "sbe") FactoryPanelSupportBehaviour support, @Local(name = "redstoneLinkMode") boolean redstoneLinkMode, @Local(name = "dots") LocalBooleanRef dots) {
        if(redstoneLinkMode && behaviour instanceof AbstractPanelBehaviour ab) {
            var opt = ab.getConnectionValue(DeployerPanelConnections.REDSTONE.get());
            if(opt.isPresent()) {
                return opt.orElse(0)==0?0x580101:0xEF0000;
            }
        }
        if(support instanceof ColoredFactoryPanelSupportBehaviour colored) {
            var line = colored.getColor(behaviour);
            dots.set(line.dots());
            return line.color();
        }
        if(support != null) return color;
        var other = FactoryPanelBehaviour.at(behaviour.getWorld(), connection);
        if(behaviour instanceof AbstractPanelBehaviour ab) {
            return other != null ? ab.calculatePath(other, color) : ab.calculateExtraPath(connection.from.pos());
        } else if(other instanceof AbstractPanelBehaviour ab) {
            for(PanelConnection<?> c : ab.getConnections()) {
                if(c == DeployerPanelConnections.STOCK_CONNECTION.get()) return color;
                if(c == DeployerPanelConnections.ITEM_STACK.get()) return color;
                if(c == DeployerPanelConnections.INTEGER.get()) return 0x006496;
                if(c == DeployerPanelConnections.REDSTONE.get()) return DeployerPanelConnections.getConnectionValue(ab, DeployerPanelConnections.REDSTONE).orElse(0)==0?0x580101:0xEF0000;
                if(c == DeployerPanelConnections.STRING.get()) return 0xFFFFFFFF;
            }
            return 0x888898;
        }
        return color;
    }
}
