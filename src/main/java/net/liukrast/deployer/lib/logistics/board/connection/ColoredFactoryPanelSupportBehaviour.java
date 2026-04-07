package net.liukrast.deployer.lib.logistics.board.connection;


import java.lang.Boolean;
import java.lang.Integer;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import java.util.function.Supplier;

/**
 * A normal factory support behaviour, but with the possibility to define color and texture of the connection pointing to id/from it
 * */
public abstract class ColoredFactoryPanelSupportBehaviour extends FactoryPanelSupportBehaviour {
    public ColoredFactoryPanelSupportBehaviour(SmartBlockEntity be, Supplier<Boolean> isOutput, Supplier<Boolean> outputPower, Runnable onNotify) {
        super(be, isOutput, outputPower, onNotify);
    }

    /**
     * should return the color and dots value of your connection
     * */
    public abstract Line getColor(FactoryPanelBehaviour behaviour);

    public record Line(Integer color, Boolean dots) {}
}
