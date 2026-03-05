package net.liukrast.deployer.lib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ValueBox.class)
public interface ValueBoxAccessor {
    @Invoker("drawString8x")
    static void drawString8x(PoseStack ms, MultiBufferSource buffer, Component text, float x, float y, int color) {
        throw new AssertionError("Mixin Injection failed");
    }
}
