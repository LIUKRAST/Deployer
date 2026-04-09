package net.liukrast.deployer.lib.logistics.packager.screen;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RequesterTabScreen<V> extends Screen implements ProvidesOrder<V>, TabData {
    private final ItemStack icon;
    protected final StockInventoryType<?, V, ?> type;
    protected final RedstoneRequesterMenu container;
    protected GenericOrderContained<V> orderData;

    public RequesterTabScreen(RedstoneRequesterMenu container, Component title, Item icon, StockInventoryType<?, V, ?> type, GenericOrderContained<V> orderData) {
        super(title);
        this.container = container;
        this.icon = icon.getDefaultInstance();
        this.type = type;
        this.orderData = orderData;
    }

    public RequesterTabScreen(RedstoneRequesterMenu container, Item icon, StockInventoryType<?, V, ?> type, GenericOrderContained<V> orderData) {
        this(
                container,
                Component.translatable(
                        "stock_inventory_type."
                                + Objects.requireNonNull(DeployerRegistries.STOCK_INVENTORY.getKey(type)).getNamespace()
                                + "." + Objects.requireNonNull(DeployerRegistries.STOCK_INVENTORY.getKey(type)).getPath()),
                icon,
                type,
                orderData
        );
    }

    @Override
    public @Nullable GenericOrderContained<V> addToSendQueue() {
        return orderData;
    }

    @Override
    public @NotNull StockInventoryType<?, V, ?> getType() {
        return type;
    }

    @Override
    public ItemStack getIcon() {
        return icon;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    public void quickMoveItemEvent(ItemStack stack) {

    }
}
