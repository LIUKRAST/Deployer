package net.liukrast.deployer.lib.logistics.board.connection;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import java.util.function.Supplier;

/**
 * Extension of {@link FactoryPanelSupportBehaviour} that adds support for
 * color-customizable panel connections.
 * <p>
 * Implementors can specify the visual appearance of the connection originating
 * from or pointing toward this behavior, including both a tint color and an
 * optional dotted style.
 *
 * <p>This is used by factory panels to render lines or indicators that reflect
 * the behavior's state, category, or type of data conveyed.
 */
@Deprecated(forRemoval = true, since = "2.1.0")
public abstract class ColoredFactoryPanelSupportBehaviour extends FactoryPanelSupportBehaviour {
    /**
     * Creates a new colored support behavior for a factory panel.
     *
     * @param be          the hosting block entity
     * @param isOutput    supplier indicating whether this side behaves as an output
     * @param outputPower supplier indicating whether the behavior is currently powered
     * @param onNotify    callback invoked when this behavior triggers an update
     */
    public ColoredFactoryPanelSupportBehaviour(SmartBlockEntity be, Supplier<Boolean> isOutput, Supplier<Boolean> outputPower, Runnable onNotify) {
        super(be, isOutput, outputPower, onNotify);
    }

    /**
     * Returns the visual line style used for connections associated with this behavior.
     * <p>
     * The returned {@link Line} defines both the tint color and the presence of
     * dotted markers.
     * Panel rendering systems use this information to determine
     * how the connection should appear in the UI.
     *
     * @param behaviour the panel behavior requesting style information
     * @return a {@link Line} describing the connection's color and pattern
     *
     * @implNote Implementors should avoid expensive computations in this method,
     * as it may be queried frequently during rendering.
     */
    public abstract Line getColor(FactoryPanelBehaviour behaviour);

    /**
     * Describes the visual properties of a panel connection.
     *
     * @param color ARGB color tint applied to the connection
     * @param dots  whether the rendered line should include a dotted pattern
     */
    public record Line(int color, boolean dots) {}
}
