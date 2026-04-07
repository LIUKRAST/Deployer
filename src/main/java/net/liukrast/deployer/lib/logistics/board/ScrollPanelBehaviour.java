package net.liukrast.deployer.lib.logistics.board;


import java.lang.Boolean;
import java.lang.Integer;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ScrollPanelBehaviour extends AbstractPanelBehaviour {
    public Integer value;
    public Component label;
    protected Integer min,max = 1;
    Consumer<Integer> callback;
    Consumer<Integer> clientCallback;
    public Function<Integer, String> formatter;
    @SuppressWarnings({"FieldCanBeLocal", "unused", "FieldMayBeFinal"})
    private Supplier<Boolean> isActive;

    public ScrollPanelBehaviour(Component label, PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
        this.setLabel(label);
        callback = i -> {
        };
        clientCallback = i -> {
        };
        formatter = i -> Integer.toString(i);
        value = 0;
        isActive = () -> true;
    }

    public void between(Integer min, Integer max) {
        this.min = min;
        this.max = max;
    }

    public void withFormatter(Function<Integer, String> formatter) {
        this.formatter = formatter;
    }

    @Override
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, Boolean clientPacket) {
        nbt.putInt("ScrollValue", value);
        super.easyWrite(nbt, registries, clientPacket);
    }

    @Override
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, Boolean clientPacket) {
        value = nbt.contains("ScrollValue") ? nbt.getInt("ScrollValue") : 0;
        super.easyRead(nbt, registries, clientPacket);
    }

    public void setValue(Integer value) {
        value = Mth.clamp(value, min, max);
        if (value == this.value)
            return;
        this.value = value;
        callback.accept(value);
        blockEntity.setChanged();
        blockEntity.sendData();
    }

    public Integer getValue() {
        return value;
    }

    public String formatValue() {
        return formatter.apply(value);
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, Boolean ctrlDown) {
        if (settings.equals(getValueSettings()))
            return;
        setValue(settings.value());
        playFeedbackSound(this);
        checkForRedstoneInput();
    }

    public void setLabel(Component label) {
        this.label = label;
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value);
    }

}
