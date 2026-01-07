package net.liukrast.deployer.lib.logistics.packagerLink;

import com.google.common.cache.Cache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.utility.TickBasedCache;
import net.createmod.catnip.data.Pair;
import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.logistics.packager.*;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixin.LogisticsManagerAccessor;
import net.liukrast.deployer.lib.mixinExtensions.LLBExtension;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.IntSupplier;

public class LogisticsGenericManager {

    private static final Map<StockInventoryType<?,?,?>, Cache<UUID, AbstractInventorySummary<?,?>>> ACCURATE_SUMMARIES = new HashMap<>();
    private static final Map<StockInventoryType<?,?,?>, Cache<UUID, AbstractInventorySummary<?,?>>> SUMMARIES = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <K,V,H> Cache<UUID, AbstractInventorySummary<K,V>> getAccurateSummaries(StockInventoryType<K,V,H> type) {
        return (Cache<UUID, AbstractInventorySummary<K,V>>) (Cache<UUID, ?>)ACCURATE_SUMMARIES.computeIfAbsent(type, k -> new TickBasedCache<>(1, false));
    }

    @SuppressWarnings("unchecked")
    public static <K,V,H> Cache<UUID, AbstractInventorySummary<K,V>> getSummaries(StockInventoryType<K,V,H> type) {
        return (Cache<UUID, AbstractInventorySummary<K,V>>) (Cache<UUID, ?>)SUMMARIES.computeIfAbsent(type, k -> new TickBasedCache<>(20, false));
    }

    public static <K,V,H> AbstractInventorySummary<K,V> getSummaryOfNetwork(StockInventoryType<K,V, H> type, UUID freqId, boolean accurate) {
        try {
            return (accurate ? getAccurateSummaries(type) : getSummaries(type)).get(freqId, () -> {
                AbstractInventorySummary<K,V> summaryOfLinks = type.networkHandler().create();
                LogisticallyLinkedBehaviour.getAllPresent(freqId, false)
                        .forEach(link -> {
                            AbstractInventorySummary<K,V> summary = ((LLBExtension)link).deployer$getSummary(type, null);
                            if (summary != type.networkHandler().empty())
                                summaryOfLinks.contributingLinks++;
                            summaryOfLinks.add(summary);
                        });
                return summaryOfLinks;
            });
        } catch (ExecutionException e) {
            DeployerConstants.LOGGER.error("Failed to get summary of network", e);
        }
        return type.networkHandler().empty();
    }

    public static <K,V,H> int getStockOf(StockInventoryType<K,V,H> type, UUID freqId, V stack, @Nullable IdentifiedContainer<H> ignoredHandler) {
        int sum = 0;
        for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(freqId, false))
            sum += ((LLBExtension)link).deployer$getSummary(type, ignoredHandler)
                    .getCountOf(stack);
        return sum;
    }

    public static <K,V,H> boolean broadcastPackageRequest(StockInventoryType<K,V,H> type, UUID freqId, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrderContained<V> order, @Nullable IdentifiedContainer<H> ignoredHandler, String address, int index, boolean isFinal) {
        return broadcastPackageRequest(type, freqId, requestType, order, ignoredHandler, address, () -> LogisticsManagerAccessor.getR().nextInt(), index, isFinal);
    }

    public static <K,V,H> boolean broadcastPackageRequest(StockInventoryType<K,V,H> type, UUID freqId, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrderContained<V> order, @Nullable IdentifiedContainer<H> ignoredHandler, String address, IntSupplier id, int index, boolean isFinal) {
        if (order.isEmpty())
            return false;

        Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> requests = findPackagersForRequest(type, freqId, order, ignoredHandler, address, id);

        // Check if packagers have accumulated too many packages already
        for (AbstractPackagerBlockEntity<K,V,H> packager : requests.keySet())
            if (packager != null && packager.isTooBusyFor(requestType))
                return false;

        // Actually perform package creation
        performPackageRequests(requests, index, isFinal);
        return true;
    }

    /**
     *
     * */
    @SuppressWarnings("unchecked")
    public static void broadcastAllPackageRequest(PackageOrderWithCrafts defaultOrder, UUID freqId, LogisticallyLinkedBehaviour.RequestType type, Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> rq, String address) {
        if(defaultOrder.isEmpty() && rq.values().stream().allMatch(GenericOrderContained::isEmpty))
            return;
        Multimap<PackagerBlockEntity, PackagingRequest> requests = LogisticsManager.findPackagersForRequest(freqId, defaultOrder, null, address);


        // Check if packagers have accumulated too many packages already
        for (PackagerBlockEntity packager : requests.keySet())
            if (packager.isTooBusyFor(type))
                return;
        var vals = requests.values();
        int id = vals.isEmpty() ? LogisticsManagerAccessor.getR().nextInt() :
                Optional.ofNullable(vals.iterator().next()).map(PackagingRequest::orderId)
                        .orElseGet(() -> LogisticsManagerAccessor.getR().nextInt());
        if(!rq.isEmpty()) {
            int index = vals.isEmpty() ? 0 : 1;
            Iterator<Map.Entry<StockInventoryType<?, ?, ?>, GenericOrderContained<?>>> it = rq.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> entry = it.next();
                boolean isLast = !it.hasNext();
                broadcastPackageRequest((StockInventoryType<Object, Object, Object>) entry.getKey(), freqId, type, (GenericOrderContained<Object>) entry.getValue(), null, address, () -> id, index, isLast);
                index++;
            }
            requests.values().forEach(pr -> PRExtension.class.cast(pr).deployer$flag());
        }

        // Actually perform package creation
        LogisticsManager.performPackageRequests(requests);
    }

    public static <K,V,H> Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> findPackagersForRequest(StockInventoryType<K,V,H> type, UUID freqId, GenericOrderContained<V> order, @javax.annotation.Nullable IdentifiedContainer<H> ignoredHandler, String address, IntSupplier id) {
        List<V> stacks = new ArrayList<>();

        for (V stack : order.stacks())
            if (!type.valueHandler().isEmpty(stack) && type.valueHandler().getCount(stack) > 0)
                stacks.add(stack);

        Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> requests = HashMultimap.create();

        // Packages need to track their index and successors for successful defrag
        Iterable<LogisticallyLinkedBehaviour> availableLinks = LogisticallyLinkedBehaviour.getAllPresent(freqId, true);
        List<LogisticallyLinkedBehaviour> usedLinks = new ArrayList<>();
        MutableBoolean finalLinkTracker = new MutableBoolean(false);

        // First box needs to carry the order specifics for successful defrag
        GenericOrderContained<V> context = order;

        // Packages from future orders should not be merged in the packager queue
        int orderId = id.getAsInt();

        for (int i = 0; i < stacks.size(); i++) {
            V entry = stacks.get(i);
            int remainingCount = type.valueHandler().getCount(entry);
            boolean finalEntry = i == stacks.size() - 1;

            for (LogisticallyLinkedBehaviour link : availableLinks) {
                int usedIndex = usedLinks.indexOf(link);
                int linkIndex = usedIndex == -1 ? usedLinks.size() : usedIndex;
                MutableBoolean isFinalLink = new MutableBoolean(false);
                if (linkIndex == usedLinks.size() - 1)
                    isFinalLink = finalLinkTracker;

                // Only send context and craftingContext with first package
                Pair<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> request = ((LLBExtension)link).deployer$processRequests(type, entry, remainingCount,
                        address, linkIndex, isFinalLink, orderId, context, ignoredHandler);
                if (request == null)
                    continue;

                requests.put(request.getFirst(), request.getSecond());

                int processedCount = request.getSecond()
                        .getCount();
                if (processedCount > 0 && usedIndex == -1) {
                    context = null;
                    usedLinks.add(link);
                    finalLinkTracker = isFinalLink;
                }

                remainingCount -= processedCount;
                if (remainingCount > 0)
                    continue;
                if (finalEntry)
                    finalLinkTracker.setTrue();
                break;
            }
        }
        return requests;
    }

    public static <K,V,H> void performPackageRequests(Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> requests, int index, boolean isFinal) {
        Map<AbstractPackagerBlockEntity<K,V,H>, Collection<GenericPackagingRequest<V>>> asMap = requests.asMap();
        for (Map.Entry<AbstractPackagerBlockEntity<K,V,H>, Collection<GenericPackagingRequest<V>>> entry : asMap.entrySet()) {
            ArrayList<GenericPackagingRequest<V>> queuedRequests = new ArrayList<>(entry.getValue());
            AbstractPackagerBlockEntity<K,V,H> packager = entry.getKey();

            if (!queuedRequests.isEmpty())
                packager.flashLink();
            for (int i = 0; i < 100; i++) {
                if (queuedRequests.isEmpty())
                    break;
                packager.attemptToSendSpecial(queuedRequests, index, isFinal);
            }

            packager.triggerStockCheck();
            packager.notifyUpdate();
        }
    }
}
