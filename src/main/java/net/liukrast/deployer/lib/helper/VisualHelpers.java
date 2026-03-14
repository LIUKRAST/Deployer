package net.liukrast.deployer.lib.helper;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import net.liukrast.deployer.lib.helper.client.RadiusFluidStreamMesh;
import net.liukrast.deployer.lib.mixin.accessors.FluidMeshAccessor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class VisualHelpers {
    private VisualHelpers() {}

    private static final RendererReloadCache<FluidKey, Model> RADIUS_STREAM = new RendererReloadCache<>(key -> new SingleMeshModel(new RadiusFluidStreamMesh(key.texture(), key.radius()), FluidMeshAccessor.invokeMaterial(key.texture())));

    public static Model radiusStream(TextureAtlasSprite sprite, float radius) {
        return RADIUS_STREAM.get(new FluidKey(sprite, radius));
    }

    private record FluidKey(TextureAtlasSprite texture, float radius) {}
}
