package net.liukrast.deployer.lib.helper.client;

import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import net.createmod.catnip.data.Iterate;
import net.liukrast.deployer.lib.mixin.FluidMeshAccessor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public class RadiusFluidStreamMesh implements QuadMesh {
    private final TextureAtlasSprite texture;
    private final float radius;

    public RadiusFluidStreamMesh(TextureAtlasSprite texture, float radius) {
        this.texture = texture;
        this.radius = radius;
    }

    @Override
    public int vertexCount() {
        return 4 * 2 * 4;
    }

    @Override
    public void write(MutableVertexList vertexList) {
        for (int i = 0; i < vertexCount(); i++) {
            vertexList.r(i, 1);
            vertexList.g(i, 1);
            vertexList.b(i, 1);
            vertexList.a(i, 1);
            vertexList.light(i, 0);
            vertexList.overlay(i, OverlayTexture.NO_OVERLAY);

            vertexList.v(i, 0);
        }

        float textureScale = 1 / 32f;

        float shrink = texture.uvShrinkRatio() * 0.25f * textureScale;
        float centerU = texture.getU0() + (texture.getU1() - texture.getU0()) * 0.5f;

        float left = -radius;
        float right = radius;

        int vertex = 0;

        for (var horizontalDirection : Iterate.horizontalDirections) {
            float x2;
            for (float x1 = left; x1 < right; x1 = x2) {
                float x1floor = Mth.floor(x1);
                x2 = Math.min(x1floor + 1, right);
                float u1 = texture.getU((x1 - x1floor) * 16 * textureScale);
                float u2 = texture.getU((x2 - x1floor) * 16 * textureScale);
                u1 = Mth.lerp(shrink, u1, centerU);
                u2 = Mth.lerp(shrink, u2, centerU);
                FluidMeshAccessor.FluidStreamAccessor.invokePutQuad(vertexList, vertex, horizontalDirection, radius, x1, x2, u1, u2);
                vertex += 4;
            }
        }
    }

    @Override
    public Vector4fc boundingSphere() {
        return new Vector4f(0, 0.5f, 0, 1);
    }
}
