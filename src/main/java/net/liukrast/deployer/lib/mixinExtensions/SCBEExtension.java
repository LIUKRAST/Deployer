package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.IdentifiedContainer;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrder;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;

import javax.annotation.Nullable;
import java.util.Map;

@ApiStatus.Internal
public interface SCBEExtension {
    <K,V,H> AbstractInventorySummary<K,V> deployer$getRecentSummary(StockInventoryType<K,V,H> type);
    <K,V,H> AbstractInventorySummary<K,V> deployer$getAccurateSummary(StockInventoryType<K,V,H> type);
    <K,V,H> boolean deployer$broadcastPackageRequest(StockInventoryType<K,V,H> type, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrder<V> order, @Nullable IdentifiedContainer<H> ignoredHandler, String address);
    /**
     * Mandatory: Middle ? in StockInventoryType must match ? in GenericOrderContained
     *
     */
    boolean deployer$broadcastAllPackageRequest(PackageOrderWithCrafts defaultOrder, LogisticallyLinkedBehaviour.RequestType requestType, Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> requests /* TODO: Introduce handler? */, String address);
    <K,V,H> boolean deployer$broadcastPackageRequest(StockInventoryType<K,V,H> type, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrderContained<V> order, @Nullable IdentifiedContainer<H> ignoredHandler, String address);
}
