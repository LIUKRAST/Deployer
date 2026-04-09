package net.liukrast.deployer.lib.logistics.board;

import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface StockSlot<V, A extends StockPanelBehaviour<?, V>> extends GaugeSlot<A> {

    @Override
    default void renderInputSlot(GuiGraphics graphics, A panel, PanelConnection<?> connection, int count, int mouseX, int mouseY, int x, int y, Font font) {
        if(connection != DeployerPanelConnections.STOCK_CONNECTION.get()) {
            GaugeSlot.renderDefaultSlot(graphics, panel, connection, mouseX, mouseY, x, y, font);
            return;
        }
        renderInputSlot(graphics, panel, panel.getStockInventoryType().valueHandler().copyWithCount(panel.getStack(), count), mouseX, mouseY, x, y, font);
    }

    @Override
    default void renderOutputSlot(GuiGraphics graphics, A panel, int count, int mouseX, int mouseY, int offsetX, int offsetY, Font font) {
        renderOutputSlot(graphics, panel, panel.getStockInventoryType().valueHandler().copyWithCount(panel.getStack(), count), mouseX, mouseY, offsetX, offsetY, font);
    }

    void renderInputSlot(GuiGraphics graphics, A panel, V stack, int mouseX, int mouseY, int offsetX, int offsetY, Font font);
    void renderOutputSlot(GuiGraphics graphics, A panel, V stack, int mouseX, int mouseY, int offsetX, int offsetY, Font font);

    int scrollAmount(boolean ctrlDown, boolean shiftDown, boolean altDown);
    default int max() {
        return Integer.MAX_VALUE;
    }

    @Override
    default boolean locksCrafting(A panel) {
        return true;
    }

    @Override
    default int mouseScrolled(A panel, double mouseX, double mouseY, double scrollX, double scrollY, boolean ctrlDown, boolean shiftDown, boolean altDown, int count) {
        int delta = (int)(Math.signum(scrollY) * scrollAmount(ctrlDown, shiftDown, altDown));
        if(delta > 0) return (Integer.MAX_VALUE - count < delta) ? Integer.MAX_VALUE : count + delta;
        else return Math.max(count + delta, 1);
    }
}
