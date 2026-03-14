package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockCheckingBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.IdentifiedContainer;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrder;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixinExtensions.SCBEExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(StockCheckingBlockEntity.class)
public abstract class StockCheckingBlockEntityMixin extends SmartBlockEntity implements SCBEExtension {

    @Shadow public LogisticallyLinkedBehaviour behaviour;

    public StockCheckingBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public <K,V,H> AbstractInventorySummary<K, V> deployer$getRecentSummary(StockInventoryType<K,V,H> type) {
        return LogisticsGenericManager.getSummaryOfNetwork(type, behaviour.freqId, false);
    }

    @Override
    public <K,V,H> AbstractInventorySummary<K, V> deployer$getAccurateSummary(StockInventoryType<K,V,H> type) {
        return LogisticsGenericManager.getSummaryOfNetwork(type, behaviour.freqId, true);
    }

    @Override
    public <K, V, H> boolean deployer$broadcastPackageRequest(StockInventoryType<K, V, H> type, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrder<V> order, @Nullable IdentifiedContainer<H> ignoredHandler, String address) {
        return deployer$broadcastPackageRequest(type, requestType, type.valueHandler().createContained(order.stacks()), ignoredHandler, address);
    }

    @Override
    public <K, V, H> boolean deployer$broadcastPackageRequest(StockInventoryType<K, V, H> type, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrderContained<V> order, @Nullable IdentifiedContainer<H> ignoredHandler, String address) {
        return LogisticsGenericManager.broadcastPackageRequest(type, behaviour.freqId, requestType, order, ignoredHandler, address, 0, true); //TODO: Check
    }

    @Override
    public boolean deployer$broadcastAllPackageRequest(PackageOrderWithCrafts defaultOrder, LogisticallyLinkedBehaviour.RequestType requestType, Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> requests, String address) {
        return LogisticsGenericManager.broadcastAllPackageRequest(defaultOrder, behaviour.freqId, requestType, requests, address);
    }
}
