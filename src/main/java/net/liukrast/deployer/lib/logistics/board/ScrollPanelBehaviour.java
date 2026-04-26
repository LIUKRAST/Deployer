package net.liukrast.deployer.lib.logistics.board;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A base panel behavior representing a scrollable numeric value.
 * Provides methods to configure a minimum and maximum range, format the
 * displayed value, and handle callbacks when the value changes.
 */
public abstract class ScrollPanelBehaviour extends AbstractPanelBehaviour {
    protected int min,max = 1;
    public int value;
    public Component label;
    Consumer<Integer> callback;
    public Function<Integer, String> formatter;

    /**
     * Constructs a new scroll panel behavior.
     *
     * @param label the display label of the panel
     * @param type the type of the panel
     * @param be the factory panel block entity
     * @param slot the slot to assign the panel to
     */
    public ScrollPanelBehaviour(Component label, PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
        this.setLabel(label);
        callback = i -> {
        };
        formatter = i -> Integer.toString(i);
        value = 0;
    }

    /**
     * Sets the minimum and maximum value for this scroll panel.
     *
     * @param min the minimum value
     * @param max the maximum value
     */
    public void between(int min, int max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Sets a custom formatter for converting the value to a string for display.
     *
     * @param formatter a function that converts an integer to a string
     */
    public void withFormatter(Function<Integer, String> formatter) {
        this.formatter = formatter;
    }

    /**
     * Saves the panel's scroll value to NBT.
     *
     * @param nbt the NBT tag to write to
     * @param registries holder registries
     * @param clientPacket whether this writing is for a client packet
     */
    @Override
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        nbt.putInt("ScrollValue", value);
        super.easyWrite(nbt, registries, clientPacket);
    }

    /**
     * Reads the panel's scroll value from NBT.
     *
     * @param nbt the NBT tag to read from
     * @param registries holder registries
     * @param clientPacket whether this read is from a client packet
     */
    @Override
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        value = nbt.getInt("ScrollValue");
        super.easyRead(nbt, registries, clientPacket);
    }

    /**
     * Sets the scroll value, clamped between {@link #min} and {@link #max}.
     * Triggers the callback and sends data updates.
     *
     * @param value the new scroll value
     */
    public void setValue(int value) {
        value = Mth.clamp(value, min, max);
        if (value == this.value)
            return;
        this.value = value;
        callback.accept(value);
        blockEntity.setChanged();
        blockEntity.sendData();
    }

    /**
     * Gets the current scroll value.
     *
     * @return the current value
     */
    public int getValue() {
        return value;
    }

    /**
     * Formats the current value using the configured formatter.
     *
     * @return the formatted value as a string
     */
    public String formatValue() {
        return formatter.apply(value);
    }

    /**
     * Sets the scroll value based on {@link ValueSettings}.
     * Plays feedback sound and checks for redstone input.
     *
     * @param player the player updating the value
     * @param settings the new value settings
     * @param ctrlDown whether the control key is pressed
     */
    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        if (settings.equals(getValueSettings()))
            return;
        setValue(settings.value());
        playFeedbackSound(this);
        checkForRedstoneInput();
    }

    /**
     * Sets the label component for the panel.
     *
     * @param label the new label
     */
    public void setLabel(Component label) {
        this.label = label;
    }

    /**
     * Determines whether the icon or text value should be rendered inside the panel's ValueBox.
     * Can be overridden by subclasses to conditionally or permanently disable rendering.
     *
     * @return {@code true} if the icon or text should be rendered, {@code false} otherwise
     */
    public boolean renderIcon() {
        return true;
    }

    /**
     * Determines whether the hover outline and tooltip overlay should be rendered when a player looks at the panel.
     * Can be overridden by subclasses to conditionally or permanently disable the overlay.
     *
     * @return {@code true} if the hover overlay should be rendered, {@code false} otherwise
     */
    public boolean renderHoverOverlay() {
        return true;
    }

    /**
     * Returns the {@link ValueSettings} representing the current scroll value.
     *
     * @return the current value settings
     */
    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value);
    }

    @Override
    public abstract ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult);
}
