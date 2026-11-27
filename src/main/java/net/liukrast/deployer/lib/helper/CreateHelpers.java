package net.liukrast.deployer.lib.helper;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.logistics.packager.GenericRepackageHelper;
import net.liukrast.deployer.lib.mixin.accessors.AllIconsAccessor;
import net.liukrast.deployer.lib.mixinExtensions.RBEExtension;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.Consumer;

public class CreateHelpers {
    public static <T extends Block> Consumer<T> withStressCapacity(double value) {
        return block -> BlockStressValues.CAPACITIES.register(block, () -> value);
    }

    public static <T extends Block> Consumer<T> withStressImpact(double value) {
        return block -> BlockStressValues.IMPACTS.register(block, () -> value);
    }

    /**
     * @return the x position of {@code icon} in its atlas, in pixels
     */
    public static int getIconX(AllIcons icon) {
        return ((AllIconsAccessor) icon).getIconX();
    }

    /**
     * @return the y position of {@code icon} in its atlas, in pixels
     */
    public static int getIconY(AllIcons icon) {
        return ((AllIconsAccessor) icon).getIconY();
    }

    /**
     * Adds one vertex of an icon quad, the same way {@link AllIcons} does when rendering in the world.
     */
    public static void iconVertex(AllIcons icon, VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, float u, float v, int light) {
        ((AllIconsAccessor) icon).invokeVertex(builder, matrix, vec, rgb, u, v, light);
    }

    /**
     * @return the helper that repacks Deployer generic packages for this re-packager (or package shelf)
     */
    public static GenericRepackageHelper getGenericRepackageHelper(RepackagerBlockEntity repackager) {
        return ((RBEExtension) repackager).deployer$getGenericRepackageHelper();
    }
}
