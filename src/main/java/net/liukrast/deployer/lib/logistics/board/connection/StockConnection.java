package net.liukrast.deployer.lib.logistics.board.connection;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.liukrast.deployer.lib.logistics.board.GenericConnections;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface StockConnection {
    void registerStockOrders(ItemsToOrderProvider provider);

    class ItemsToOrderProvider {
        private final FactoryPanelConnection connection;
        private final FactoryPanelBehaviour source;
        private final Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>> consolidated;

        public ItemsToOrderProvider(Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>> consolidated, FactoryPanelBehaviour source, FactoryPanelConnection connection) {
            this.consolidated = consolidated;
            this.source = source;
            this.connection = connection;
        }

        public <K, V, H> void add(StockInventoryType<K, V, H> type, V item) {
            Map<UUID, Map<?, GenericConnections<?>>> consolidated =
                    this.consolidated.computeIfAbsent(type, k -> new HashMap<>());
            @SuppressWarnings({"unchecked", "rawtypes"})
            Map<V, GenericConnections<V>> networkItemCounts =
                    (Map<V, GenericConnections<V>>) (Map) consolidated.computeIfAbsent(
                            source.network,
                            $ -> new Object2ObjectOpenCustomHashMap<>(
                                    type.valueHandler().hashStrategy()
                            )
                    );
            networkItemCounts.computeIfAbsent(item, $ -> new GenericConnections<>(item));
            GenericConnections<V> existingConnections = networkItemCounts.get(item);
            existingConnections.add(connection);
            existingConnections.totalAmount += connection.amount;
        }


    }
}
