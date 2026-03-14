package net.liukrast.deployer.lib.logistics.packager.screen;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.liukrast.deployer.lib.mixin.accessors.StockTickerBlockEntityAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class KeeperTabScreen extends Screen {
    private final ItemStack icon;
    protected final StockTickerBlockEntity blockEntity;
    protected final StockTickerBlockEntityAccessor beAccess;
    protected final StockKeeperRequestMenu menu;

    private int guiLeft,guiTop;

    public KeeperTabScreen(StockTickerBlockEntity blockEntity, StockKeeperRequestMenu menu, Component title, Item icon) {
        super(title);
        this.icon = icon.getDefaultInstance();
        this.blockEntity = blockEntity;
        beAccess = (StockTickerBlockEntityAccessor) blockEntity;
        this.menu = menu;
    }

    public void containerTick() {

    }

    public ItemStack getIcon() {
        return icon;
    }


    public void onSendIt() {

    }

    public void switchFocused() {

    }

    public final int getGuiLeft() {
        return guiLeft;
    }

    public final int getGuiTop() {
        return guiTop;
    }

    public final void setGui(int left, int top) {
        this.guiLeft = left;
        this.guiTop = top;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    /*public abstract int clickAmount(boolean ctrlDown, boolean shiftDown, boolean altDown);

    public abstract boolean matchesModSearch(V stack, String searchValue);
    public abstract boolean matchesTagSearch(V stack, String searchValue);
    public abstract boolean matchesSearch(V stack, String searchValue);
    public abstract void renderCategory(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<V> categoryStacks, List<V> itemsToOrder, AbstractInventorySummary<K, V> forcedEntries, StockInventoryType.CategoryRenderData data);
    public abstract void renderOrderedItems(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<V> itemsToOrder, AbstractInventorySummary<K, V> forcedEntries, StockInventoryType.OrderRenderData data);
    public abstract void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, V entry, Font font, boolean isOrder);

    public boolean shouldRenderSearchBar() {
        return true;
    }*/

}
