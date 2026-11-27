package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.IdentifiedContainer;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.Deployer$MappedInfo;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.logistics.stockTicker.LogisticalStockGenericRequestPacket;
import net.liukrast.deployer.lib.mixinExtensions.STBEExtension;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin(StockTickerBlockEntity.class)
public abstract class StockTickerBlockEntityMixin extends StockCheckingBlockEntityMixin implements STBEExtension {

    @Shadow protected String previouslyUsedAddress;
    @Shadow protected List<ItemStack> categories;
    @Unique private final Deployer$MappedInfo deployer$mappedInfo = new Deployer$MappedInfo();

    public StockTickerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "refreshClientStockSnapshot", at = @At("TAIL"))
    private void refreshClientStockSnapshot(CallbackInfo ci) {
        CatnipServices.NETWORK.sendToServer(new LogisticalStockGenericRequestPacket(worldPosition));
    }

    @Override
    public <K,V,H> List<List<V>> deployer$getClientStockSnapshot(StockInventoryType<K,V, H> type) {
        return deployer$mappedInfo.getLastClientsideStockSnapshot(type);
    }

    @Override
    public <K, V, H> AbstractInventorySummary<K, V> deployer$getLastClientsideStockSnapshotAsSummary(StockInventoryType<K, V, H> type) {
        return deployer$mappedInfo.getLastClientsideStockSnapshotAsSummary(type);
    }

    @Override
    public boolean deployer$broadcastAllPackageRequest(PackageOrderWithCrafts defaultOrder, LogisticallyLinkedBehaviour.RequestType requestType, Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> requests, String address) {
        boolean result = super.deployer$broadcastAllPackageRequest(defaultOrder, requestType, requests, address);
        previouslyUsedAddress = address;
        notifyUpdate();
        return result;
    }

    @Override
    public <K, V, H> boolean deployer$broadcastPackageRequest(StockInventoryType<K, V, H> type, LogisticallyLinkedBehaviour.RequestType requestType, GenericOrderContained<V> order, IdentifiedContainer<H> ignoredHandler, String address) {
        boolean result = super.deployer$broadcastPackageRequest(type, requestType, order, ignoredHandler, address);
        previouslyUsedAddress = address;
        notifyUpdate();
        return result;
    }

    @Override
    public <K, V, H> AbstractInventorySummary<K, V> deployer$getRecentSummary(StockInventoryType<K, V, H> type) {
        AbstractInventorySummary<K,V> recentSummary = super.deployer$getRecentSummary(type);
        int contributingLinks = recentSummary.contributingLinks;
        if (deployer$mappedInfo.getActiveLinks(type) != contributingLinks && !isRemoved()) {
            deployer$mappedInfo.setActiveLinks(type, contributingLinks);
            sendData();
        }
        return recentSummary;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if(!clientPacket) return;
        CompoundTag activeLinks = new CompoundTag();
        for(StockInventoryType<?,?,?> type : DeployerRegistries.STOCK_INVENTORY) {
            @SuppressWarnings("DataFlowIssue")
            var id = DeployerRegistries.STOCK_INVENTORY.getKey(type).toString();
            activeLinks.putInt(id, deployer$mappedInfo.getActiveLinks(type));
        }
        tag.put("deployer$ActiveLinks", activeLinks);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if(!clientPacket) return;
        var activeLinks = tag.getCompound("deployer$ActiveLinks");
        for(var type : DeployerRegistries.STOCK_INVENTORY) {
            @SuppressWarnings("DataFlowIssue")
            var id = DeployerRegistries.STOCK_INVENTORY.getKey(type).toString();
            deployer$mappedInfo.setActiveLinks(type, activeLinks.getInt(id));
        }
    }

    @Override
    public <K,V,H> void deployer$receiveStockPacket(StockInventoryType<K,V,H> type, List<V> stacks, boolean endOfTransmission) {
        if(deployer$mappedInfo.getNewlyReceivedStockSnapshot(type) == null)
            deployer$mappedInfo.setNewlyReceivedStockSnapshot(type, new ArrayList<>());
        deployer$mappedInfo.getNewlyReceivedStockSnapshot(type).addAll(stacks);

        if(!endOfTransmission) return;
        deployer$mappedInfo.setLastClientsideStockSnapshotAsSummary(type, type.networkHandler().createSummary());
        deployer$mappedInfo.setLastClientsideStockSnapshot(type, new ArrayList<>());

        for(V stack : deployer$mappedInfo.getNewlyReceivedStockSnapshot(type))
            deployer$mappedInfo.getLastClientsideStockSnapshotAsSummary(type).add(stack);

        for(ItemStack filter : categories) {
            List<V> inCategory = new ArrayList<>();
            if(!filter.isEmpty()) {
                FilterItemStack filterItemStack = FilterItemStack.of(filter);
                for(Iterator<V> iterator = deployer$mappedInfo.getNewlyReceivedStockSnapshot(type).iterator(); iterator.hasNext();) {
                    V stack = iterator.next();
                    if(!type.valueHandler().test(filterItemStack, level, stack)) continue;
                    inCategory.add(stack);
                    iterator.remove();
                }
            }
            deployer$mappedInfo.getLastClientsideStockSnapshot(type).add(inCategory);
        }

        List<V> unsorted = new ArrayList<>(deployer$mappedInfo.getNewlyReceivedStockSnapshot(type));
        deployer$mappedInfo.getLastClientsideStockSnapshot(type).add(unsorted);
        deployer$mappedInfo.setNewlyReceivedStockSnapshot(type, null);
    }

    @Override
    public Deployer$MappedInfo deployer$getMappedInfo() {
        return deployer$mappedInfo;
    }
}
