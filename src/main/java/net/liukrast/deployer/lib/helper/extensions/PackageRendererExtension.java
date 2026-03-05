package net.liukrast.deployer.lib.helper.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Deprecated
public interface PackageRendererExtension {
    void render(PackageEntity entity, float yaw, float pt, PoseStack ms, MultiBufferSource buffer, int light);
}
