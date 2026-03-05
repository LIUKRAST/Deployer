package net.liukrast.deployer.lib.helper.box;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.gui.AllIcons;
import net.liukrast.deployer.lib.mixin.ValueBoxAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidValueBox extends ValueBox {
    FluidStack stack;
    MutableComponent count;

    public FluidValueBox(Component label, AABB bb, BlockPos pos, FluidStack stack, MutableComponent count) {
        super(label, bb, pos);
        this.stack = stack;
        this.count = count;
    }

    @Override
    public AllIcons getOutline() {
        if (!stack.isEmpty())
            return AllIcons.VALUE_BOX_HOVER_6PX;
        return super.getOutline();
    }

    @Override
    public void renderContents(PoseStack ms, MultiBufferSource buffer) {
        super.renderContents(ms, buffer);
        if (count == null)
            return;

        Font font = Minecraft.getInstance().font;
        ms.translate(17.5, -5, 7);
        boolean isEmpty = stack.isEmpty();

        float scale = 1.5f;
        ms.translate(-font.width(count), 0, 0);

        if (isEmpty) {
            ms.translate(-15, -1, -2.75);
            scale = 1.65f;
        } else {
            ms.translate(-7, 10, 0);
        }

        if (count.getString().equals("*"))
            ms.translate(-1, 3, 0);

        ms.scale(scale, scale, scale);
        ValueBoxAccessor.drawString8x(ms, buffer, count, 0, 0, 0xEDEDED);
    }
}
