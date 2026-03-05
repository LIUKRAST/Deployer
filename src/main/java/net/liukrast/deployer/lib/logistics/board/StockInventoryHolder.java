package net.liukrast.deployer.lib.logistics.board;

import com.simibubi.create.content.logistics.BigItemStack;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class StockInventoryHolder<V> extends BigItemStack {
    private final StockInventoryType<?, V, ?> stockInventoryType;
    private final V value;
    public StockInventoryHolder(StockInventoryType<?, V, ?> stockInventoryType, V value, int amount) {
        super(ItemStack.EMPTY, amount);
        this.stockInventoryType = stockInventoryType;
        this.value = value;
    }

    public boolean isValueEmpty() {
        return stockInventoryType.valueHandler().isEmpty(value);
    }

    public void renderAsInput(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean restocker, Font font) {
        var copy = stockInventoryType.valueHandler().copyWithCount(value, count);
        stockInventoryType.packageHandler().renderGaugeSlotInput(graphics, copy, mouseX, mouseY, x, y, restocker, font);
    }

    public void renderAsOutput(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, Font font) {
        var copy = stockInventoryType.valueHandler().copyWithCount(value, count);
        stockInventoryType.packageHandler().renderGaugeSlotOutput(graphics, copy, mouseX, mouseY, x, y, font);
    }

    public StockInventoryType<?, V, ?> getStockInventoryType() {
        return stockInventoryType;
    }

    public String getTitle() {
        return stockInventoryType.packageHandler().getPromiseItemName(value);
    }
}
