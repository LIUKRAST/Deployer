package net.liukrast.deployer.lib.helper.ponder;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.foundation.instruction.AnimateElementInstruction;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class AnimatePartialInstruction extends AnimateElementInstruction<PartialElement> {
    public static AnimatePartialInstruction rotate(ElementLink<PartialElement> link, Vec3 rotation, int ticks) {
        return new AnimatePartialInstruction(link, rotation, ticks,
                (wse, v) -> wse.setAnimatedRotation(v, ticks == 0), PartialElement::getAnimatedRotation);
    }

    public static AnimatePartialInstruction move(ElementLink<PartialElement> link, Vec3 offset, int ticks) {
        return new AnimatePartialInstruction(link, offset, ticks, (wse, v) -> wse.setAnimatedOffset(v, ticks == 0), PartialElement::getAnimatedOffset);
    }

    protected AnimatePartialInstruction(ElementLink<PartialElement> link, Vec3 totalDelta, int ticks, BiConsumer<PartialElement, Vec3> setter, Function<PartialElement, Vec3> getter) {
        super(link, totalDelta, ticks, setter, getter);
    }

}
