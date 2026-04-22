package net.liukrast.deployer.lib.helper.ponder;

import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.minecraft.world.phys.Vec3;

public interface PartialElement extends AnimatedSceneElement {
    void setAnimatedRotation(Vec3 eulerAngles, boolean force);
    Vec3 getAnimatedRotation();

    void setAnimatedOffset(Vec3 offset, boolean force);
    Vec3 getAnimatedOffset();
}
