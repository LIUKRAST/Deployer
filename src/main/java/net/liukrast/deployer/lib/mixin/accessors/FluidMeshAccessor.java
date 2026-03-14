package net.liukrast.deployer.lib.mixin.accessors;

import com.simibubi.create.content.fluids.FluidMesh;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FluidMesh.class)
public interface FluidMeshAccessor {
    @Invoker("material")
    static SimpleMaterial invokeMaterial(TextureAtlasSprite sprite) {
        throw new AssertionError("Mixin injection failed");
    }

    @Mixin(FluidMesh.FluidStreamMesh.class)
    interface FluidStreamAccessor {
        @Invoker("putQuad")
        static void invokePutQuad(MutableVertexList vertexList, int i, Direction horizontal, float radius, float p0, float p1, float u0, float u1) {
            throw new AssertionError("Mixin injection failed");
        }
    }
}
