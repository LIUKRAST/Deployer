package net.liukrast.deployer.lib.helper.ponder;

import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.minecraft.core.Direction;

public class CreatePartialInstruction extends FadeIntoSceneInstruction<PartialElement> {
    public CreatePartialInstruction(int fadeInTicks, Direction fadeInFrom, PartialElement element) {
        super(fadeInTicks, fadeInFrom, element);
    }

    @Override
    protected Class<PartialElement> getElementClass() {
        return PartialElement.class;
    }
}
