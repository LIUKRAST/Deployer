package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.IdentifiedContainer;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.Deployer$MappedInfo;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;

import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public interface STBEExtension {
    <K,V,H> List<List<V>> deployer$getClientStockSnapshot(StockInventoryType<K,V,H> type);
    <K,V,H> AbstractInventorySummary<K,V> deployer$getLastClientsideStockSnapshotAsSummary(StockInventoryType<K,V,H> type);
    /**
     * Mandatory: Middle ? in StockInventoryType must match ? in GenericOrderContained
     *
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean deployer$broadcastAllPackageRequest(PackageOrderWithCrafts defaultOrder, LogisticallyLinkedBehaviour.RequestType requestType, Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> requests, String address);
    @SuppressWarnings("UnusedReturnValue")
    <K,V,H> boolean deployer$broadcastPackageRequest(StockInventoryType<K,V,H> type, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrderContained<V> order, IdentifiedContainer<H> ignoredHandler, String address);
    <K,V,H> AbstractInventorySummary<K,V> deployer$getRecentSummary(StockInventoryType<K,V,H> type);
    <V,K,H> void deployer$receiveStockPacket(StockInventoryType<V,K,H> type, List<K> stacks, boolean endOfTransmission);
    Deployer$MappedInfo deployer$getMappedInfo();
}
