package net.liukrast.deployer.lib.logistics.board;

import com.simibubi.create.content.logistics.BigItemStack;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class StockInventoryHolder<K> extends BigItemStack {
    private final K value;
    private final GaugeSlot<K, ?> slot;
    public StockInventoryHolder(K value, GaugeSlot<K, ?> slot, int amount) {
        super(ItemStack.EMPTY, amount);
        this.value = value;
        this.slot = slot;
    }

    public boolean isValueEmpty() {
        return slot.isEmpty(value);
    }

    public void renderAsInput(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean restocker, Font font) {
        slot.renderInputSlot(graphics, value, mouseX, mouseY, x, y, restocker, font);
    }

    public void renderAsOutput(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, Font font) {
        slot.renderOutputSlot(graphics, value, mouseX, mouseY, x, y, font);
    }

    public String getTitle() {
        return slot.getTitle(value);
    }
}
