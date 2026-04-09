package net.liukrast.deployer.lib.logistics.packager.screen;

import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ProvidesOrder<V> {
    @Nullable
    GenericOrderContained<V> addToSendQueue();
    @NotNull
    StockInventoryType<?,V,?> getType();
}
