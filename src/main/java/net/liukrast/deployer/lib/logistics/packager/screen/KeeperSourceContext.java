package net.liukrast.deployer.lib.logistics.packager.screen;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.mixin.accessors.StockTickerBlockEntityAccessor;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where a Stock Keeper tab reads its stock from. Deployer builds one for every Stock Keeper screen,
 * other screens (for example a portable stock requester) can implement it to host the same tabs
 * */
public interface KeeperSourceContext {
    <K,V> AbstractInventorySummary<K,V> getLastClientsideSnapshotAsSummary(StockInventoryType<K,V,?> type);
    Map<UUID, List<Integer>> getHiddenCategoriesByPlayer();
    List<ItemStack> getCategories();
    <K,V,H> List<List<V>> getClientStockSnapshot(StockInventoryType<K,V,H> type);
    int getActiveLinks();

    /**
     * @return The player looking at the stock
     * */
    Player getPlayer();

    static KeeperSourceContext of(StockTickerBlockEntity be, Player player) {
        return new KeeperSourceContext() {
            @Override
            public Player getPlayer() {
                return player;
            }

            @Override
            public <K, V> AbstractInventorySummary<K, V> getLastClientsideSnapshotAsSummary(StockInventoryType<K, V, ?> type) {
                return ((StockTickerBlockEntityAccessor)be).deployer$getLastClientsideStockSnapshotAsSummary(type);
            }

            @Override
            public Map<UUID, List<Integer>> getHiddenCategoriesByPlayer() {
                return ((StockTickerBlockEntityAccessor)be).getHiddenCategoriesByPlayer();
            }

            @Override
            public List<ItemStack> getCategories() {
                return ((StockTickerBlockEntityAccessor)be).getCategories();
            }

            @Override
            public <K, V, H> List<List<V>> getClientStockSnapshot(StockInventoryType<K, V, H> type) {
                return ((StockTickerBlockEntityAccessor)be).deployer$getClientStockSnapshot(type);
            }

            @Override
            public int getActiveLinks() {
                return ((StockTickerBlockEntityAccessor)be).getActiveLinks();
            }
        };
    }
}
