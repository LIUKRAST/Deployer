package net.liukrast.deployer.lib.helper.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import net.minecraft.client.renderer.MultiBufferSource;

@Deprecated
@FunctionalInterface
public interface ChainConveyorPackageRenderExtension {
    void render(ChainConveyorBlockEntity be, ChainConveyorPackage box, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay);
}
