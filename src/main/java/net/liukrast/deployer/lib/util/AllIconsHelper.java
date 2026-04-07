package net.liukrast.deployer.lib.util;


import java.lang.Float;
import java.lang.Integer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.mixin.AllIconsMixin;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class AllIconsHelper {

    public static Integer getIconX(AllIcons allIcons) {
        return ((AllIconsMixin)allIcons).getIconX();
    }

    public static Integer getIconY(AllIcons allIcons) {
        return ((AllIconsMixin)allIcons).getIconY();
    }

    public static void invokeVertex(AllIcons allIcons, VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, Float u, Float v, Integer light) {
        ((AllIconsMixin)allIcons).invokeVertex(builder, matrix, vec, rgb, u, v, light);
    }
}
