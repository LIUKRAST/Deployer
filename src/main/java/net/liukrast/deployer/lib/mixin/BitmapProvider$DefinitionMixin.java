package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(BitmapProvider.Definition.class)
public class BitmapProvider$DefinitionMixin {
    @Unique private int deployer$shift;

    @SuppressWarnings("DataFlowIssue")
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
    private static MapCodec<BitmapProvider.Definition> clinit(MapCodec<BitmapProvider.Definition> original) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(
                original.forGetter(d -> d),
                Codec.INT.optionalFieldOf("deployer:shift").forGetter(d -> Optional.of(BitmapProvider$DefinitionMixin.class.cast(d).deployer$shift))
        ).apply(inst, (definition, extra) -> {
            BitmapProvider$DefinitionMixin.class.cast(definition).deployer$shift = extra.orElse(0);
            return definition;
        }));
    }

    @ModifyArg(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/providers/BitmapProvider$Glyph;<init>(FLcom/mojang/blaze3d/platform/NativeImage;IIIIII)V"
            ),
            index = 6
    )
    private int load(int advance) {
        return advance + deployer$shift;
    }
}
