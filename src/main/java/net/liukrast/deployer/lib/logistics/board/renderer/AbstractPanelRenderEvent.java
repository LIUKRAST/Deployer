package net.liukrast.deployer.lib.logistics.board.renderer;


import java.lang.Float;
import java.lang.Integer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.Event;

public class AbstractPanelRenderEvent extends Event {
    public final AbstractPanelBehaviour behaviour;
    public final Float partialTicks;
    public final PoseStack poseStack;
    public final MultiBufferSource bufferSource;
    public final Integer light, overlay;

    public AbstractPanelRenderEvent(AbstractPanelBehaviour behaviour, Float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, Integer light, Integer overlay) {
        this.behaviour = behaviour;
        this.partialTicks = partialTicks;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.light = light;
        this.overlay = overlay;
    }


}
