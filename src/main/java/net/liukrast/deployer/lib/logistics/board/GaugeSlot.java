package net.liukrast.deployer.lib.logistics.board;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface GaugeSlot<K, A extends AbstractPanelBehaviour> {
    K collectData(A behaviour);
    boolean isEmpty(K savedData);
    void renderInputSlot(GuiGraphics graphics, K savedData, int mouseX, int mouseY, int offsetX, int offsetY, boolean restocker, Font font);
    void renderOutputSlot(GuiGraphics graphics, K savedData, int mouseX, int mouseY, int offsetX, int offsetY, Font font);
    default String getTitle(K savedData) {
        return "Value not set";
    }
    default StockInventoryHolder<K> createHolder(A apb) {
        return new StockInventoryHolder<>(collectData(apb), this, 0);
    }
}
