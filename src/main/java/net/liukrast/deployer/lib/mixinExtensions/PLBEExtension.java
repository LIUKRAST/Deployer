package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import net.createmod.catnip.data.Pair;
import net.liukrast.deployer.lib.logistics.packager.*;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;

@ApiStatus.Internal
public interface PLBEExtension {
    <K,V,H> AbstractInventorySummary<K,V> deployer$fetchSummaryFromPackager(StockInventoryType<K,V,H> type, @Nullable IdentifiedContainer<H> ignoredHandler);
    <K,V,H> Pair<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> deployer$processRequest(StockInventoryType<K,V,H> type, V stack, int amount, String address, int linkIndex, MutableBoolean finalLink, int orderId, @Nullable GenericOrderContained<V> context, @Nullable IdentifiedContainer<H> ignoreHandler);
    <K,V,H> AbstractPackagerBlockEntity<K,V,H> deployer$getPackager(StockInventoryType<K,V,H> type);

}
