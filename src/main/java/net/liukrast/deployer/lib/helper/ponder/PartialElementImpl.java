package net.liukrast.deployer.lib.helper.ponder;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class PartialElementImpl extends AnimatedSceneElementBase implements PartialElement {
    private final PartialModel model;

    Vec3 location;

    Vec3 prevAnimatedOffset = Vec3.ZERO;
    Vec3 animatedOffset = Vec3.ZERO;
    Vec3 prevAnimatedRotation = Vec3.ZERO;
    Vec3 animatedRotation = Vec3.ZERO;
    Vec3 centerOfRotation = Vec3.ZERO;


    public PartialElementImpl(PartialModel model, Vec3 location) {
        this.model = model;
        this.location = location;
    }

    @Override
    public void reset(PonderScene scene) {
        super.reset(scene);
        prevAnimatedOffset = Vec3.ZERO;
        animatedOffset = Vec3.ZERO;
        prevAnimatedRotation = Vec3.ZERO;
        animatedRotation = Vec3.ZERO;
    }

    @Override
    public void setAnimatedRotation(Vec3 eulerAngles, boolean force) {
        this.animatedRotation = eulerAngles;
        if(force)
            prevAnimatedRotation = animatedRotation;
    }

    @Override
    public Vec3 getAnimatedRotation() {
        return animatedRotation;
    }

    @Override
    public void setAnimatedOffset(Vec3 offset, boolean force) {
        this.animatedOffset = offset;
        if (force)
            prevAnimatedOffset = animatedOffset;
    }

    @Override
    public Vec3 getAnimatedOffset() {
        return animatedOffset;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        prevAnimatedOffset = animatedOffset;
        prevAnimatedRotation = animatedRotation;
    }

    @Override
    public void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(location.x, location.y, location.z);
        transformMS(ms, pt);
        int light = lightCoordsFromFade(fade);
        CachedBuffers.partial(model, Blocks.AIR.defaultBlockState())
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutout()));

        ms.popPose();
    }

    public void transformMS(PoseStack ms, float pt) {

        Vec3 vec = VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset);
        ms.translate(vec.x, vec.y, vec.z);
        if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
            double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
            double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
            double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);

            TransformStack.of(ms)
                    .translate(centerOfRotation)
                    .rotateXDegrees((float) rotX)
                    .rotateYDegrees((float) rotY)
                    .rotateZDegrees((float) rotZ)
                    .translateBack(centerOfRotation);
        }
    }
}