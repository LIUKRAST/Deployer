package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import net.createmod.catnip.data.Pair;
import net.liukrast.deployer.lib.blockEntity.behaviour.advancedValueSettings.RowData;

import java.util.List;
import java.util.function.Function;

@ApiStatus.Internal
public interface VSSExtension {
    void deployer$setRowData(List<RowData> rowData);
    void deployer$setFormatter(Function<ValueSettings, Pair<Integer, String>> formatter);
}
