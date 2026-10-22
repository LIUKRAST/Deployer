package net.liukrast.deployer.lib.blockEntity.behaviour.advancedValueSettings;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Represents a row inside the Value Settings Screen
 *
 * @param component the row's {@link Component}
 * @param minValue the minimum value
 * @param maxValue the maximum value
 * @param acceptsDecimalValue whether the {@link ValueEditBox} should accept decimal values
 */
public record RowData(Component component, int minValue, int maxValue, boolean acceptsDecimalValue) {
    public RowData(Component component, int minValue, int maxValue) {
        this(component, minValue, maxValue, false);
    }

    /**
     * Creates a list of rows from a {@link ValueSettingsBoard}
     *
     * @param board the reference board
     * @param minValue the minimum value
     * @return a list of rows
     */
    public static List<RowData> fromBoard(ValueSettingsBoard board, int minValue) {
        return board.rows().stream().map(c -> new RowData(c, minValue, board.maxValue())).toList();
    }
}
