package net.liukrast.deployer.lib.logistics.board;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.logistics.board.connection.ProvidesConnection;
import net.liukrast.deployer.lib.logistics.board.connection.StockConnection;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.GenericPackagingRequest;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager;
import net.liukrast.deployer.lib.mixin.accessors.LogisticsManagerAccessor;
import net.liukrast.deployer.lib.mixinExtensions.FPCExtension;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@ApiStatus.Internal
public final class PanelRequestHelper {
    private PanelRequestHelper() {}

    public interface EffectSender {
        void send(FactoryPanelPosition from, boolean success);
    }

    public record StockTargets(
            Map<FactoryPanelPosition, FactoryPanelConnection> vanillaTargets,
            Map<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> items,
            Map<StockInventoryType<?, ?, ?>, Map<UUID, Map<?, GenericConnections<?>>>> generic,
            boolean isEmpty
    ) {}

    public record GenericRequests(Map<StockInventoryType<?, ?, ?>, List<Multimap<AbstractPackagerBlockEntity<?, ?, ?>, GenericPackagingRequest<?>>>> byType) {
        public boolean anyPackagerBusy() {
            for (var requests : byType.values())
                for (var request : requests)
                    for (var packager : request.keySet())
                        if (packager.isTooBusyFor(LogisticallyLinkedBehaviour.RequestType.RESTOCK))
                            return true;
            return false;
        }

        public boolean send(int firstIndex) {
            int index = firstIndex;
            boolean anySent = false;
            for (var requests : byType.values()) {
                if (requests.isEmpty()) continue;
                var iterator = requests.iterator();
                while (iterator.hasNext()) {
                    var request = iterator.next();
                    perform(request, index, !iterator.hasNext());
                }
                anySent = true;
                index++;
            }
            return anySent;
        }
    }

    @Nullable
    public static StockTargets collectStockTargets(FactoryPanelBehaviour target) {
        PanelConnection<StockConnection<?>> stock = DeployerPanelConnections.STOCK_CONNECTION.get();
        Map<FactoryPanelPosition, FactoryPanelConnection> vanillaTargets = new HashMap<>();
        Map<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> items = new HashMap<>();
        Map<StockInventoryType<?, ?, ?>, Map<UUID, Map<?, GenericConnections<?>>>> generic = new HashMap<>();
        boolean isEmpty = true;

        for (var entry : target.targetedBy.entrySet()) {
            FactoryPanelConnection connection = entry.getValue();
            FactoryPanelBehaviour source = FactoryPanelBehaviour.at(target.getWorld(), connection);
            if (source == null) return null;

            boolean isVanilla = !(source instanceof AbstractPanelBehaviour);
            FPCExtension link = (FPCExtension) connection;
            if (isVanilla && link.deployer$getLinkMode() == null)
                link.deployer$setLinkMode(stock);

            var mode = ProvidesConnection.getCurrentConnection(connection,
                    () -> ProvidesConnection.getPossibleConnections(source, target).stream().findFirst().orElse(null));
            if (mode != stock) continue;

            isEmpty = false;
            ((ProvidesConnection) source).getConnectionValue(stock)
                    .ifPresent(value -> value.register(items, generic, source, connection));
            if (isVanilla) vanillaTargets.put(entry.getKey(), connection);
        }
        return new StockTargets(vanillaTargets, items, generic, isEmpty);
    }

    public static Map<StockInventoryType<?, ?, ?>, Multimap<UUID, ?>> collectAvailableGenericStock(
            Map<StockInventoryType<?, ?, ?>, Map<UUID, Map<?, GenericConnections<?>>>> generic,
            EffectSender effects,
            Runnable onMissing
    ) {
        Map<StockInventoryType<?, ?, ?>, Multimap<UUID, ?>> toRequest = new HashMap<>();
        for (var entry : generic.entrySet())
            toRequest.put(entry.getKey(), collectAvailableStock(entry.getKey(), entry.getValue(), effects, onMissing));
        return toRequest;
    }

    public static GenericRequests findGenericPackagers(
            Map<StockInventoryType<?, ?, ?>, Multimap<UUID, ?>> toRequest,
            String address,
            Map<UUID, Integer> orderIds
    ) {
        Map<StockInventoryType<?, ?, ?>, List<Multimap<AbstractPackagerBlockEntity<?, ?, ?>, GenericPackagingRequest<?>>>> byType = new HashMap<>();
        for (var entry : toRequest.entrySet())
            byType.put(entry.getKey(), findPackagers(entry.getKey(), entry.getValue(), address, orderIds));
        return new GenericRequests(byType);
    }

    public static int orderIdOf(Multimap<PackagerBlockEntity, PackagingRequest> request) {
        var iterator = request.values().iterator();
        if (!iterator.hasNext()) return randomOrderId();
        PackagingRequest first = iterator.next();
        return first == null ? randomOrderId() : first.orderId();
    }

    public static void flagVanillaRequests(List<Multimap<PackagerBlockEntity, PackagingRequest>> requests) {
        for (var request : requests)
            for (PackagingRequest packagingRequest : request.values())
                ((PRExtension) (Object) packagingRequest).deployer$flag();
    }

    private static int randomOrderId() {
        return LogisticsManagerAccessor.getR().nextInt();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <K, V, H> Multimap<UUID, V> collectAvailableStock(
            StockInventoryType<K, V, H> type,
            Map<UUID, Map<?, GenericConnections<?>>> byNetwork,
            EffectSender effects,
            Runnable onMissing
    ) {
        Multimap<UUID, V> toRequest = HashMultimap.create();
        var values = type.valueHandler();
        for (var entry : byNetwork.entrySet()) {
            UUID network = entry.getKey();
            Map<V, GenericConnections<V>> typed = (Map) entry.getValue();
            AbstractInventorySummary<K, V> summary = LogisticsGenericManager.getSummaryOfNetwork(type, network, true);

            for (GenericConnections<V> connections : typed.values()) {
                boolean available = connections.totalAmount != 0
                        && !values.isEmpty(connections.item)
                        && summary.getCountOf(connections.item) >= connections.totalAmount;
                for (FactoryPanelConnection connection : connections)
                    effects.send(connection.from, available);
                if (!available) {
                    onMissing.run();
                    continue;
                }
                toRequest.put(network, values.copyWithCount(connections.item, connections.totalAmount));
            }
        }
        return toRequest;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <K, V, H> List<Multimap<AbstractPackagerBlockEntity<?, ?, ?>, GenericPackagingRequest<?>>> findPackagers(
            StockInventoryType<K, V, H> type,
            Multimap<UUID, ?> toRequest,
            String address,
            Map<UUID, Integer> orderIds
    ) {
        List<Multimap<AbstractPackagerBlockEntity<?, ?, ?>, GenericPackagingRequest<?>>> requests = new ArrayList<>();
        Map<UUID, Collection<V>> byNetwork = (Map) toRequest.asMap();
        for (var entry : byNetwork.entrySet()) {
            UUID network = entry.getKey();
            var order = type.valueHandler().createContained(new ArrayList<>(entry.getValue()));
            var request = LogisticsGenericManager.findPackagersForRequest(type, network, order, null, address,
                    () -> orderIds.computeIfAbsent(network, $ -> randomOrderId()));
            requests.add((Multimap) request);
        }
        return requests;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void perform(Multimap<AbstractPackagerBlockEntity<?, ?, ?>, GenericPackagingRequest<?>> request, int index, boolean isLast) {
        LogisticsGenericManager.performPackageRequests((Multimap) request, index, isLast);
    }
}
