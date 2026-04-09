package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.liukrast.deployer.lib.DeployerClient;
import net.liukrast.deployer.lib.mixinExtensions.ACPExtension;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ElytraLayer.class)
public class ElytraLayerMixin {
    @Definition(id = "playerskin", local = @Local(type = PlayerSkin.class))
    @Definition(id = "capeTexture", method = "Lnet/minecraft/client/resources/PlayerSkin;capeTexture()Lnet/minecraft/resources/ResourceLocation;")
    @Expression("playerskin.capeTexture() != null")
    @ModifyExpressionValue(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean render(boolean original, @Local AbstractClientPlayer player) {
        var cape = ((ACPExtension)player).deployer$getCape();
        if(cape == -1) return original;
        var capeTexture = DeployerClient.getCape(cape);
        return capeTexture != null || original;
    }

    @ModifyExpressionValue(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/PlayerSkin;capeTexture()Lnet/minecraft/resources/ResourceLocation;")
    )
    private ResourceLocation render(ResourceLocation original, @Local AbstractClientPlayer player) {
        var cape = ((ACPExtension)player).deployer$getCape();
        if(cape == -1) return original;
        var capeTexture = DeployerClient.getCape(cape);
        return capeTexture == null ? original : capeTexture;
    }
}
