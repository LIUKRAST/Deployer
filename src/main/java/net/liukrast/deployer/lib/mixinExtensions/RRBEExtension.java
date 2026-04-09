package net.liukrast.deployer.lib.mixinExtensions;

import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;

import java.util.Map;

public interface RRBEExtension {
    <K,V,H> void deployer$setEncodedRequest(StockInventoryType<K,V,H> type, GenericOrderContained<V> encodedRequest);
    <K,V,H> GenericOrderContained<V> deployer$getEncodedRequest(StockInventoryType<K,V,H> type);
    Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> deployer$getAllEncodedRequests();
}
