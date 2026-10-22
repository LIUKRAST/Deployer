package net.liukrast.deployer.lib.blockEntity.behaviour.advancedValueSettings;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.Function;

/**
 * <h1>AdvancedValueSettingsBehaviour</h1>
 * This can be implemented on a class extending {@link com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour}
 */
public interface AdvancedValueSettingsBehaviour extends ValueSettingsBehaviour {
    default boolean valueEditBoxEnabled() {
        return true;
    }

    /**
     * @return a formatter function that gets applied on the initial {@link ValueSettings}
     * when enabling the ValueEditBox.
     */
    default Function<ValueSettings, Pair<Integer, String>> getFormatter() {
        return vs -> Pair.of(vs.row(), Integer.toString(vs.value()));
    }

    /**
     * @return a list of {@link RowData} representing the rows
     */
    List<RowData> getRowData();

    /**
     * Saves the Value Settings set by the Player
     *
     * @param player the Player
     * @param row the row
     * @param value the value
     * @param ctrlDown whether control was held
     */
    void setValueSettings(Player player, int row, double value, boolean ctrlDown);
}