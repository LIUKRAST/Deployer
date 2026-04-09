package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockCheckingBlockEntity;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixinExtensions.RRBEExtension;
import net.liukrast.deployer.lib.mixinExtensions.SCBEExtension;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(RedstoneRequesterBlockEntity.class)
public abstract class RedstoneRequesterBlockEntityMixin extends StockCheckingBlockEntity implements RRBEExtension {
    @Shadow public boolean allowPartialRequests;

    @Unique private final Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> deployer$encodedRequests = DeployerRegistries.STOCK_INVENTORY.stream()
            .collect(Collectors.toMap(
                    e -> e,
                    e -> GenericOrderContained.empty()
            ));

    public RedstoneRequesterBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public <K, V, H> void deployer$setEncodedRequest(StockInventoryType<K, V, H> type, GenericOrderContained<V> encodedRequest) {
        deployer$encodedRequests.put(type, encodedRequest);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V, H> GenericOrderContained<V> deployer$getEncodedRequest(StockInventoryType<K, V, H> type) {
        return (GenericOrderContained<V>) deployer$encodedRequests.getOrDefault(type, GenericOrderContained.empty());
    }

    @Inject(method = "triggerRequest", at = @At("HEAD"), cancellable = true)
    private void triggerRequest(CallbackInfo ci, @Share("any_succeeded") LocalBooleanRef anySucceededLoc) {
        boolean anySucceeded = false;
        for(StockInventoryType<?,?,?> type : DeployerRegistries.STOCK_INVENTORY) {
            anySucceeded |= deployer$triggerRequest(type, ci);
        }
        anySucceededLoc.set(anySucceeded);
    }

    @Unique
    private <K,V,H> boolean deployer$triggerRequest(StockInventoryType<K, V, H> type, CallbackInfo ci) {
        GenericOrderContained<V> encodedRequest = deployer$getEncodedRequest(type);

        if(encodedRequest.isEmpty())
            return false;

        boolean anySucceeded = false;
        AbstractInventorySummary<K, V> summaryOfOrder = type.networkHandler().createSummary();
        encodedRequest.stacks().forEach(summaryOfOrder::add);

        AbstractInventorySummary<K, V> summary = ((SCBEExtension)this).deployer$getAccurateSummary(type);

        for(V entry : summaryOfOrder.getStacks()) {
            if(summary.getCountOf(entry) >= type.valueHandler().getCount(entry)) {
                anySucceeded = true;
                continue;
            }
            if(!allowPartialRequests && level instanceof ServerLevel serverLevel) {
                CatnipServices.NETWORK.sendToClientsAround(serverLevel, worldPosition, 32,
                        new RedstoneRequesterEffectPacket(worldPosition, false));
                ci.cancel();
                return false;
            }
        }

        return anySucceeded;
    }

    @ModifyExpressionValue(method = "triggerRequest", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;isEmpty()Z"))
    private boolean triggerRequest$1(boolean original) {
        return original && deployer$encodedRequests.values().stream().allMatch(GenericOrderContained::isEmpty);
    }

    @ModifyArg(method = "triggerRequest", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/redstoneRequester/RedstoneRequesterEffectPacket;<init>(Lnet/minecraft/core/BlockPos;Z)V"), index = 1)
    private boolean triggerRequest(boolean original, @Share("any_succeeded") LocalBooleanRef anySucceededLoc) {
        return original || anySucceededLoc.get();
    }

    @WrapOperation(method = "triggerRequest", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/redstoneRequester/RedstoneRequesterBlockEntity;broadcastPackageRequest(Lcom/simibubi/create/content/logistics/packagerLink/LogisticallyLinkedBehaviour$RequestType;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Lcom/simibubi/create/content/logistics/packager/IdentifiedInventory;Ljava/lang/String;)Z"))
    private boolean triggerRequest(RedstoneRequesterBlockEntity instance, LogisticallyLinkedBehaviour.RequestType requestType, PackageOrderWithCrafts packageOrderWithCrafts, IdentifiedInventory identifiedInventory, String s, Operation<Boolean> original, @Share("any_succeeded") LocalBooleanRef anySucceededLoc) {
        if(!anySucceededLoc.get()) return original.call(instance, requestType, packageOrderWithCrafts, identifiedInventory, s);
        return ((SCBEExtension)this).deployer$broadcastAllPackageRequest(packageOrderWithCrafts, requestType, deployer$encodedRequests, s);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        CompoundTag specialData = tag.getCompound("deployer$encodedRequest");
        for(String key : specialData.getAllKeys()) {
            StockInventoryType<?,?,?> type = DeployerRegistries.STOCK_INVENTORY.get(ResourceLocation.parse(key));
            if(type == null) continue;
            deployer$encodedRequests.put(type, CatnipCodecUtils.decode(type.valueHandler().orderContainedCodec(), registries, specialData.getCompound(key)).orElse(GenericOrderContained.empty()));
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(method = {"writeSafe", "write"}, at = @At("TAIL"))
    private void writeSafe(CallbackInfo ci, @Local(argsOnly = true) CompoundTag tag, @Local(argsOnly = true) HolderLookup.Provider registries) {
        CompoundTag specialData = new CompoundTag();
        for(var entry : deployer$encodedRequests.entrySet()) {
            deployer$encode((StockInventoryType<?, Object, ?>) entry.getKey(), (GenericOrderContained<Object>) entry.getValue(), specialData, registries);
        }
        tag.put("deployer$encodedRequest", specialData);
    }

    @Unique
    private <K,V,H> void deployer$encode(StockInventoryType<K,V,H> type, GenericOrderContained<V> order, CompoundTag tag, HolderLookup.Provider registries) {
        var id = DeployerRegistries.STOCK_INVENTORY.getKey(type);
        if(id == null) return;
        tag.put(id.toString(), CatnipCodecUtils.encode(type.valueHandler().orderContainedCodec(), registries, order).orElseThrow());
    }

    @Override
    public Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> deployer$getAllEncodedRequests() {
        return deployer$encodedRequests;
    }
}
