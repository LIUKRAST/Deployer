package net.liukrast.deployer.lib.logistics.packagerLink;

import com.google.common.cache.Cache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.utility.TickBasedCache;
import net.createmod.catnip.data.Pair;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.logistics.packager.*;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixin.accessors.LogisticsManagerAccessor;
import net.liukrast.deployer.lib.mixinExtensions.LLBExtension;
import net.liukrast.deployer.lib.mixinExtensions.PLBEExtension;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

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
                AbstractInventorySummary<K,V> summaryOfLinks = type.networkHandler().createSummary();
                Set<Object> processedInventories = new HashSet<>();
                LogisticallyLinkedBehaviour.getAllPresent(freqId, false)
                        .forEach(link -> {
                            Object currentInventoryId = getInventoryKey(type, link);
                            if(currentInventoryId != null && !processedInventories.add(currentInventoryId))
                                return;

                            AbstractInventorySummary<K,V> summary = ((LLBExtension)link).deployer$getSummary(type, null);
                            var empty = type.networkHandler().empty();
                            if (summary != empty) {
                                summaryOfLinks.contributingLinks++;
                                summaryOfLinks.add(summary);
                            }
                        });
                return summaryOfLinks;
            });
        } catch (ExecutionException e) {
            Deployer.CONSTANTS.getLogger().error("Failed to get summary of network", e);
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
    public static boolean broadcastAllPackageRequest(PackageOrderWithCrafts defaultOrder, UUID freqId, LogisticallyLinkedBehaviour.RequestType type, Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> rq, String address) {
        rq = rq.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        boolean stockOrdersEmpty = rq.values().stream().allMatch(GenericOrderContained::isEmpty);
        if(defaultOrder.isEmpty() && stockOrdersEmpty)
            return false;
        Multimap<PackagerBlockEntity, PackagingRequest> requests = LogisticsManager.findPackagersForRequest(freqId, defaultOrder, null, address);


        // Check if packagers have accumulated too many packages already
        for (PackagerBlockEntity packager : requests.keySet())
            if (packager.isTooBusyFor(type))
                return false;
        var vals = requests.values();
        int id = vals.isEmpty() ? LogisticsManagerAccessor.getR().nextInt() :
                Optional.ofNullable(vals.iterator().next()).map(PackagingRequest::orderId)
                        .orElseGet(() -> LogisticsManagerAccessor.getR().nextInt());
        if(!rq.isEmpty() && !stockOrdersEmpty) {
            int index = vals.isEmpty() ? 0 : 1;
            Iterator<Map.Entry<StockInventoryType<?, ?, ?>, GenericOrderContained<?>>> it = rq.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> entry = it.next();
                boolean isLast = !it.hasNext();
                if(broadcastPackageRequest((StockInventoryType<Object, Object, Object>) entry.getKey(), freqId, type, (GenericOrderContained<Object>) entry.getValue(), null, address, () -> id, index, isLast))
                    index++;
            }
            //noinspection DataFlowIssue
            requests.values().forEach(pr -> PRExtension.class.cast(pr).deployer$flag());
        }

        // Actually perform package creation
        LogisticsManager.performPackageRequests(requests);
        return true;
    }

    public static <K,V,H> Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> findPackagersForRequest(StockInventoryType<K,V,H> type, UUID freqId, GenericOrderContained<V> order, @javax.annotation.Nullable IdentifiedContainer<H> ignoredHandler, String address, IntSupplier id) {
        List<V> stacks = new ArrayList<>();

        for (V stack : order.stacks())
            if (!type.valueHandler().isEmpty(stack) && type.valueHandler().getCount(stack) > 0)
                stacks.add(stack);

        Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> requests = HashMultimap.create();

        // Packages need to track their index and successors for successful defrag
        List<LogisticallyLinkedBehaviour> availableLinks = oneLinkPerInventory(type, LogisticallyLinkedBehaviour.getAllPresent(freqId, true));
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

    private static <K,V,H> List<LogisticallyLinkedBehaviour> oneLinkPerInventory(StockInventoryType<K,V,H> type, Iterable<LogisticallyLinkedBehaviour> links) {
        List<LogisticallyLinkedBehaviour> all = new ArrayList<>();
        Map<Object, List<LogisticallyLinkedBehaviour>> byInventory = new HashMap<>();
        for (LogisticallyLinkedBehaviour link : links) {
            all.add(link);
            Object id = getInventoryKey(type, link);
            if (id != null)
                byInventory.computeIfAbsent(id, k -> new ArrayList<>()).add(link);
        }
        Set<LogisticallyLinkedBehaviour> skipped = new HashSet<>();
        for (List<LogisticallyLinkedBehaviour> group : byInventory.values()) {
            if (group.size() < 2)
                continue;
            LogisticallyLinkedBehaviour selected = group.get(LogisticsManagerAccessor.getR().nextInt(group.size()));
            for (LogisticallyLinkedBehaviour link : group)
                if (link != selected)
                    skipped.add(link);
        }
        if (!skipped.isEmpty())
            all.removeIf(skipped::contains);
        return all;
    }

    @Nullable
    private static <K,V,H> Object getInventoryKey(StockInventoryType<K,V,H> type, LogisticallyLinkedBehaviour link) {
        if (!(link.blockEntity instanceof PackagerLinkBlockEntity plbe))
            return null;
        AbstractPackagerBlockEntity<K,V,H> packager = ((PLBEExtension) plbe).deployer$getPackager(type);
        if (packager == null || packager.getLevel() == null || !packager.targetInventory.hasInventory())
            return null;
        InventoryIdentifier identifier = InventoryIdentifier.get(packager.getLevel(), packager.targetInventory.getTarget().getOpposite());
        return identifier != null ? identifier : packager.targetInventory.getInventory();
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
