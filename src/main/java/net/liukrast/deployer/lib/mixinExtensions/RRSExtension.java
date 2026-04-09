package net.liukrast.deployer.lib.mixinExtensions;

import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packager.screen.RequesterTabScreen;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface RRSExtension {
    @Nullable RequesterTabScreen<?> deployer$getTab();
    void deployer$receiveData(Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> dataMap);
    boolean deployer$mouseClicked(double mouseX, double mouseY, int button);
    void deployer$insertStack(ItemStack stack);
}
