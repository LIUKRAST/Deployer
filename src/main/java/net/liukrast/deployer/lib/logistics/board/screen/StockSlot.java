package net.liukrast.deployer.lib.logistics.board.screen;

import net.liukrast.deployer.lib.logistics.board.StockPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A specialized version of {@link GaugeSlot} for handling stock inventory linked slots.
 * <p>
 * It provides built-in logic for {@code STOCK_CONNECTION} types, allowing easier rendering
 * of specific value types (e.g., ItemStacks or FluidStacks) and standardized scrolling behavior.
 * </p>
 *
 * @param <V> The type of the value being handled (e.g., ItemStack, FluidStack).
 * @param <A> The type of the stock panel behavior.
 */
public abstract class StockSlot<V, A extends StockPanelBehaviour<?, V>> extends OutputSlot<V, A> {

    public StockSlot(A panel, PanelConnection<?> connection) {
        super(panel, connection);
    }

    @Override
    public void renderInputSlot(GuiGraphics graphics, int count, int mouseX, int mouseY, int offsetX, int offsetY) {
        if (!panel.hasInteraction("restocker") && connection != DeployerPanelConnections.STOCK_CONNECTION.get())
            renderDefaultSlot(graphics, panel, connection, mouseX, mouseY, offsetX, offsetY);
        else
            renderInputSlot(graphics, panel.getStockInventoryType().valueHandler().copyWithCount(panel.getStack(), count), mouseX, mouseY, offsetX, offsetY);
    }

    @Override
    public void renderSlot(GuiGraphics graphics, int count, int mouseX, int mouseY, int offsetX, int offsetY) {
        renderOutputSlot(graphics, panel.getStockInventoryType().valueHandler().copyWithCount(panel.getStack(), count), mouseX, mouseY, offsetX, offsetY);
    }

    @Override
    public Component getTitle() {
        return getTitle(panel.getStack());
    }

    public abstract Component getTitle(V stack);

    public abstract void renderInputSlot(GuiGraphics graphics, V stack, int mouseX, int mouseY, int offsetX, int offsetY);
    public abstract void renderOutputSlot(GuiGraphics graphics, V stack, int mouseX, int mouseY, int offsetX, int offsetY);

    public int max() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean locksCrafting() {
        return true;
    }
}
