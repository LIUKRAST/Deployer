package net.liukrast.deployer.lib.logistics.packager.screen;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StockRequesterPage<V> implements ProvidesOrder<V> {
    private final ItemStack icon;
    protected final StockInventoryType<?, V, ?> type;
    protected final RedstoneRequesterMenu container;
    private final Component title;

    public StockRequesterPage(RedstoneRequesterMenu container, Component title, Item icon, StockInventoryType<?, V, ?> type) {
        this.container = container;
        this.title = title;
        this.icon = icon.getDefaultInstance();
        this.type = type;
    }

    public StockRequesterPage(RedstoneRequesterMenu container, Item icon, StockInventoryType<?, V, ?> type) {
        this(
                container,
                Component.translatable(
                        "stock_inventory_type."
                                + Objects.requireNonNull(DeployerRegistries.STOCK_INVENTORY.getKey(type)).getNamespace()
                                + "." + Objects.requireNonNull(DeployerRegistries.STOCK_INVENTORY.getKey(type)).getPath()),
                icon,
                type
        );
    }

    @Override
    public @Nullable GenericOrderContained<V> addToSendQueue() {
        return null;
    }

    @Override
    public @NotNull StockInventoryType<?, V, ?> getType() {
        return null;
    }

    @Override
    public List<Component> getWarnTooltip() {
        return null;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public Component getTitle() {
        return title;
    }
}
