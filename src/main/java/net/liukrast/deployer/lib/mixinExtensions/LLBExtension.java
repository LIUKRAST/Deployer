package net.liukrast.deployer.lib.mixinExtensions;

import net.createmod.catnip.data.Pair;
import net.liukrast.deployer.lib.logistics.packager.*;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point: implement it on a {@link com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour}
 * (for example with a mixin on another mod's behaviour) to let it provide and receive Deployer generic stock.
 * Deployer implements it on every logistically linked behaviour.
 */
public interface LLBExtension {
    <K,V,H> Pair<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> deployer$processRequests(StockInventoryType<K,V,H> type, V stack, int amount, String address, int linkIndex, MutableBoolean finalLink, int orderId, @javax.annotation.Nullable GenericOrderContained<V> context, @javax.annotation.Nullable IdentifiedContainer<H> ignoredHandler);
    <K,V,H> AbstractInventorySummary<K,V> deployer$getSummary(StockInventoryType<K,V,H> type, @Nullable IdentifiedContainer<H> ignoredHandler);
    <K,V,H> void deployer$deductFromAccurateSummary(StockInventoryType<K,V,H> type, H handler);
}
