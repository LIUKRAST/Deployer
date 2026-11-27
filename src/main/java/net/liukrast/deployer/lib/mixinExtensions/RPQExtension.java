package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.GenericRequestPromise;

import java.util.List;

@ApiStatus.Internal
public interface RPQExtension {
    <K,V,H> void deployer$add(StockInventoryType<K,V,H> type, GenericRequestPromise<V> value);
    <K,V,H> int deployer$getTotalPromisedAndRemoveExpired(StockInventoryType<K,V,H> type, V stack, int expiryTime);
    <K,V,H> void deployer$forceClear(StockInventoryType<K,V,H> type, V stack);
    <K,V,H> void deployer$genericEnteredSystem(StockInventoryType<K,V,H> type, V stack, int amount);
    <K,V,H> List<GenericRequestPromise<V>> deployer$flatten(StockInventoryType<K,V,H> type, boolean sorted);
}
