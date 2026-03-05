package net.liukrast.deployer.lib.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.serialization.Codec;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.liukrast.deployer.lib.logistics.LogisticallyLinked;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.GenericConnections;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.logistics.board.connection.StockConnection;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.GenericPackagingRequest;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(FactoryPanelBehaviour.class)
public abstract class FactoryPanelBehaviourMixin extends FilteringBehaviour implements FPBExtension {
    /* UNIQUE VARIABLES */
    @Unique private final Map<BlockPos, FactoryPanelConnection> deployer$targetedByExtra = new HashMap<>();
    /* SHADOWS */
    @Shadow public Map<FactoryPanelPosition, FactoryPanelConnection> targetedBy;

    public FactoryPanelBehaviourMixin(SmartBlockEntity be, ValueBoxTransform slot) {
        super(be, slot);
    }

    @Shadow @Nullable public static FactoryPanelBehaviour at(BlockAndTintGetter world, FactoryPanelConnection connection) {throw new AssertionError("Mixin injection failed");}

    @Shadow
    public UUID network;

    @Shadow
    protected abstract void sendEffect(FactoryPanelPosition fromPos, boolean success);

    @Shadow
    public String recipeAddress;

    /* IMPL METHODS */
    @Override public Map<BlockPos, FactoryPanelConnection> deployer$getExtra() {return deployer$targetedByExtra;}

    /* VERY IMPORTANT */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;tickStorageMonitor()V"))
    private void tick(FactoryPanelBehaviour instance, Operation<Void> original) {
        if(instance instanceof AbstractPanelBehaviour apb) apb.tickStorageMonitor();
        else original.call(instance);
    }

    /* Allows abstract panels to decide whether they want to use or original tick function */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/filtering/FilteringBehaviour;tick()V", shift = At.Shift.AFTER), cancellable = true)
    private void tick(CallbackInfo ci) {if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour ab && ab.skipOriginalTick()) ci.cancel();}

    @Definition(id = "behaviour", local = @Local(type = FactoryPanelBehaviour.class))
    @Definition(id = "active", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;active:Z") @Expression("behaviour.active")
    @WrapOperation(method = "at(Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;)Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean at(FactoryPanelBehaviour instance, Operation<Boolean> original) {
        if(instance == null) return true;
        return original.call(instance);
    }

    /* MOVE TO */
    @ModifyVariable(method = "moveTo", at = @At(value = "STORE", ordinal = 0))
    private FactoryPanelBehaviour moveTo(FactoryPanelBehaviour original) {
        var be = ((FactoryPanelBlockEntity)original.blockEntity);
        var slot = original.slot;
        if(be.panels.get(slot) instanceof AbstractPanelBehaviour superOriginal)
            return superOriginal.getPanelType().create(be, slot);
        return original;
    }

    @Inject(method = "moveTo", at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;", ordinal = 0), cancellable = true)
    private void moveTo(FactoryPanelPosition newPos, ServerPlayer player, CallbackInfo ci) {
        for(BlockPos pos : deployer$targetedByExtra.keySet()) {
            if(!pos.closerThan(newPos.pos(), 24)) {
                ci.cancel();
                return;
            }
        }
    }

    /* TICK REQUESTS */

    @Inject(method = "tickRequests", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;<init>()V"))
    private void tickRequests(
            CallbackInfo ci,
            @Share("consolidated") LocalRef<Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>>> map
    ) {
        map.set(new HashMap<>());
    }

    /* In the tick requests we filter all the gauges that do not contain a filter connection to avoid ticking non-item-related gauges */
    @ModifyExpressionValue(method = "tickRequests", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;", ordinal = 0))
    private Collection<FactoryPanelConnection> tickRequests(
            Collection<FactoryPanelConnection> original,
            @Share("consolidated") LocalRef<Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>>> consolidated
    ) {
        return original.stream()
                .filter(connection -> {
                    FactoryPanelBehaviour source = at(getWorld(), connection);
                    if(source instanceof AbstractPanelBehaviour apb) {
                        for(var c : apb.getConnections()) {
                            if(c == DeployerPanelConnections.STOCK_CONNECTION.get()) {
                                apb.getConnectionValue(DeployerPanelConnections.STOCK_CONNECTION.get())
                                        .ifPresent(sc -> sc.registerStockOrders(new StockConnection.ItemsToOrderProvider(consolidated.get(), source, connection)));
                                return false;
                            }
                            if(c == DeployerPanelConnections.ITEM_STACK.get()) return true;
                            if(c == DeployerPanelConnections.REDSTONE.get()) return false;
                            if(c == DeployerPanelConnections.INTEGER.get()) return false;
                            if(c == DeployerPanelConnections.STRING.get()) return false;
                        }
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    @Inject(method = "tickRequests", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/HashMultimap;create()Lcom/google/common/collect/HashMultimap;"))
    private void tickRequests(
            CallbackInfo ci,
            @Local(name = "failed") LocalBooleanRef failed,
            @Share("consolidated") LocalRef<Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>>> consolidated,
            @Share("toRequest") LocalRef<Map<StockInventoryType<?,?,?>, Multimap<UUID, ?>>> toRequest,
            @Share("savedUUIDs") LocalRef<Map<UUID, Integer>> savedUUIDs
    ) {
        toRequest.set(new HashMap<>());
        savedUUIDs.set(new HashMap<>());

        for (var e : consolidated.get().entrySet()) {
            deployer$tickRequests(e.getKey(), e.getValue(), failed, toRequest.get().computeIfAbsent(e.getKey(), k -> HashMultimap.create()));
        }
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private <K, V, H> void deployer$tickRequests(
            StockInventoryType<K, V, H> type,
            Map<UUID, Map<?, GenericConnections<?>>> map,
            @Local LocalBooleanRef failed,
            Multimap<UUID, ?> toRequest
            ) {
        for (var entry : map.entrySet()) {
            UUID network = entry.getKey();
            Map<V, GenericConnections<V>> typed =
                    (Map) entry.getValue();

            AbstractInventorySummary<K, V> summary = LogisticsGenericManager.getSummaryOfNetwork(type, network, true);

            for(GenericConnections<V> connections : typed.values()) {
                if (connections.totalAmount == 0 || type.valueHandler().isEmpty(connections.item) || summary.getCountOf(connections.item) < connections.totalAmount) {
                    for (FactoryPanelConnection connection : connections)
                        sendEffect(connection.from, false);
                    failed.set(true);
                    continue;
                }

                V stack = type.valueHandler().copyWithCount(connections.item, connections.totalAmount);
                ((Multimap<UUID, V>) toRequest).put(network, stack);
                for (FactoryPanelConnection connection : connections)
                    sendEffect(connection.from, true);
            }
        }
    }

    @Inject(method = "tickRequests", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void tickRequests(
            CallbackInfo ci,
            @Local(name = "entry") Map.Entry<UUID, Collection<BigItemStack>> entry,
            @Local(name = "request") Multimap<PackagerBlockEntity, PackagingRequest> request,
            @Share("savedUUIDs") LocalRef<Map<UUID, Integer>> savedUUIDs,
            @Share("isValsEmpty") LocalBooleanRef isValsEmpty
    ) {
        var vals = request.values();
        int id = vals.isEmpty() ? LogisticsManagerAccessor.getR().nextInt() :
                Optional.ofNullable(vals.iterator().next()).map(PackagingRequest::orderId)
                                .orElseGet(() -> LogisticsManagerAccessor.getR().nextInt());
        savedUUIDs.get().put(entry.getKey(), id);
        isValsEmpty.set(vals.isEmpty());
    }

    @Definition(id = "requests", local = @Local(type = List.class, name = "requests"))
    @Definition(id = "iterator", method = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    @Expression("requests.iterator()")
    @Inject(method = "tickRequests", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0), cancellable = true)
    private void tickRequests(
            CallbackInfo ci,
            @Local(name = "requests") List<Multimap<PackagerBlockEntity, PackagingRequest>> localRequests,
            @Share("toRequest") LocalRef<Map<StockInventoryType<?,?,?>, Multimap<UUID, ?>>> toRequest,
            @Share("savedUUIDs") LocalRef<Map<UUID, Integer>> savedUUIDs,
            @Share("isValsEmpty") LocalBooleanRef isValsEmpty
    ) {
        Map<StockInventoryType<?,?,?>, Map<UUID, Collection<?>>> asMap = new HashMap<>();
        Map<StockInventoryType<?,?,?>, List<Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>>>> requests = new HashMap<>();

        // Map init
        for(var e : toRequest.get().entrySet()) {
            asMap.put(e.getKey(), (Map)e.getValue().asMap());
            requests.put(e.getKey(), new ArrayList<>());
        }

        // Collect request distributions
        for(var e : asMap.entrySet()) {
            deployer$tickRequests(e.getKey(), e.getValue(), requests.get(e.getKey()), savedUUIDs.get());
        }

        // Check if any packager is busy, cancel all
        for(var e : requests.values()) {
            for(var entry : e) {
                for(AbstractPackagerBlockEntity<?,?,?> packager : entry.keySet())
                    if(packager.isTooBusyFor(LogisticallyLinkedBehaviour.RequestType.RESTOCK)) {
                        ci.cancel();
                        return;
                    }
            }
        }

        // Send it

        int index = isValsEmpty.get() ? 0 : 1;
        boolean oneFound = false;
        for(var e : requests.entrySet()) {
            if(e.getValue().isEmpty()) continue;
            var it = e.getValue().iterator();
            while(it.hasNext()) {
                var entry = it.next();
                boolean isLast = !it.hasNext();
                deployer$tickRequests(entry, index, isLast);
            }
            oneFound = true;
            index++;
        }

        //TODO: Modify local
        if(oneFound)
            for(var e : localRequests) {
                e.values().forEach(pr -> PRExtension.class.cast(pr).deployer$flag());
            }
    }

    @Unique
    private <K,V,H> void deployer$tickRequests(StockInventoryType<K,V,H> type, Map<UUID, Collection<?>> asMap$raw, List<Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>>> requests$raw, Map<UUID, Integer> savedUUIDs) {
        Map<UUID, Collection<V>> asMap = (Map) asMap$raw;
        List<Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>>> requests = (List)requests$raw;
        for(var entry : asMap.entrySet()) {
            GenericOrderContained<V> order = GenericOrderContained.simple(new ArrayList<>(entry.getValue()));
            Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> request =
                    LogisticsGenericManager.findPackagersForRequest(type, entry.getKey(), order, null, recipeAddress, () -> savedUUIDs.get(entry.getKey()));
            requests.add(request);
        }
    }

    @Unique
    private <K,V,H> void deployer$tickRequests(Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>> entry$raw, int index, boolean isLast) {
        Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> entry = (Multimap)entry$raw;
        LogisticsGenericManager.performPackageRequests(entry, index, isLast);
    }

    @WrapOperation(
            method = "tickRequests",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/packagerLink/RequestPromiseQueue;add(Lcom/simibubi/create/content/logistics/packagerLink/RequestPromise;)V"
            )
    )
    private void tickRequests(RequestPromiseQueue instance, RequestPromise promise, Operation<Void> original) {
        if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour apb) {
            apb.addPromises(instance);
        } else original.call(instance, promise);
    }


    /* DATA */
    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putUUID(Ljava/lang/String;Ljava/util/UUID;)V"))
    private void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci, @Local(name = "panelTag") CompoundTag panelTag) {
        panelTag.put("TargetedByExtra", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(deployer$targetedByExtra.values())).orElseThrow());
    }

    @Inject(method = "read", at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"))
    private void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci, @Local(name = "panelTag") CompoundTag panelTag) {
        deployer$targetedByExtra.clear();
        CatnipCodecUtils.decode(Codec.list(FactoryPanelConnection.CODEC), panelTag.get("TargetedByExtra")).orElse(List.of())
                .forEach(c -> deployer$targetedByExtra.put(c.from.pos(), c));
    }

    /* ADDING/REMOVING CONNECTIONS */
    @Inject(method = "addConnection", at = @At("HEAD"), cancellable = true)
    private void addConnection(FactoryPanelPosition fromPos, CallbackInfo ci) {
        var i = FactoryPanelBehaviour.class.cast(this);
        var fromState = i.getWorld().getBlockState(fromPos.pos());
        if(PanelConnection.makeContext(i.getWorld().getBlockState(i.getPos())) == PanelConnection.makeContext(fromState) && DeployerRegistries.PANEL_CONNECTION
                .stream()
                .map(c -> c.getListener(fromState.getBlock()))
                .anyMatch(Objects::nonNull)
        ) {
            deployer$targetedByExtra.put(fromPos.pos(), new FactoryPanelConnection(fromPos, 1));
            i.blockEntity.notifyUpdate();
            ci.cancel();
        }
    }

    @ModifyArg(method = "addConnection", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelConnection;<init>(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;I)V", ordinal = 1), index = 1)
    private int addConnection(int amount, @Local(name = "source") FactoryPanelBehaviour source) {
        if(source instanceof AbstractPanelBehaviour apb) {
            return apb.getDefaultConnectionAmount();
        }
        return amount;
    }

    @Inject(method = "disconnectAllLinks", at = @At("TAIL"))
    private void disconnectAllLinks(CallbackInfo ci) {
        deployer$targetedByExtra.clear();
    }

    /* OTHER PANELS UPDATE */
    @ModifyVariable(method = "checkForRedstoneInput", at = @At(value = "STORE", ordinal = 0), name = "shouldPower")
    private boolean checkForRedstoneInput(boolean shouldPower, @Cancellable CallbackInfo ci) {
        var i = FactoryPanelBehaviour.class.cast(this);
        block: for(FactoryPanelConnection connection : targetedBy.values()) {
            if(!i.getWorld().isLoaded(connection.from.pos())) {
                ci.cancel();
                return false;
            }
            Level world = i.getWorld();
            FactoryPanelBehaviour behaviour = at(world, connection);
            if(behaviour == null || !behaviour.isActive()) return false;
            if(!(behaviour instanceof AbstractPanelBehaviour panel)) continue;
            for(var c : panel.getConnections()) {
                if(c == DeployerPanelConnections.STOCK_CONNECTION.get()) continue block;
                if(c == DeployerPanelConnections.ITEM_STACK.get()) continue block;
                if(c == DeployerPanelConnections.INTEGER.get()) continue block;
                if(c == DeployerPanelConnections.REDSTONE.get()) {
                    shouldPower |= panel.getConnectionValue(DeployerPanelConnections.REDSTONE).orElse(0) > 0;
                    continue block;
                }
            }
        }
        for(var connection : deployer$targetedByExtra.values()) {
            var pos = connection.from.pos();
            if(!i.getWorld().isLoaded(pos)) {
                ci.cancel();
                return false;
            }
            var state = i.getWorld().getBlockState(pos);
            var be = i.getWorld().getBlockEntity(pos);
            var listener = DeployerPanelConnections.REDSTONE.get().getListener(state.getBlock());
            if(listener == null) continue;
            var opt = listener.invalidate(i.getWorld(), state, pos, be);
            if(opt.isPresent()) shouldPower |= opt.get() > 0;
        }
        return shouldPower;
    }

    @Definition(id = "shouldPower", local = @Local(type = boolean.class))
    @Definition(id = "redstonePowered", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;redstonePowered:Z")
    @Expression("shouldPower == this.redstonePowered")
    @ModifyExpressionValue(method = "checkForRedstoneInput", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean checkForRedstoneInput$1(boolean original) {
        Integer total = null;
        StringBuilder addressChange = null;
        block: for(FactoryPanelConnection connection : targetedBy.values()) {
            if(!getWorld().isLoaded(connection.from.pos())) {
                return false;
            }
            Level world = getWorld();
            FactoryPanelBehaviour behaviour = at(world, connection);
            if(behaviour == null || !behaviour.isActive()) return false;
            if(!(behaviour instanceof AbstractPanelBehaviour panel)) continue;
            Set<PanelConnection<?>> connections = panel.getConnections();
            for(PanelConnection<?> c : connections) {
                if(c == DeployerPanelConnections.STOCK_CONNECTION.get()) continue block;
                if(c == DeployerPanelConnections.ITEM_STACK.get()) continue block;
                if(c == DeployerPanelConnections.INTEGER.get()) {
                    if(total == null) total = 0;
                    total += panel.getConnectionValue(DeployerPanelConnections.INTEGER.get()).orElse(0);
                    continue block;
                }
                if(c == DeployerPanelConnections.REDSTONE.get()) continue block;
                if(c == DeployerPanelConnections.STRING.get()) {
                    if(addressChange == null) addressChange = new StringBuilder(panel.getConnectionValue(DeployerPanelConnections.STRING.get()).orElse(""));
                    else addressChange.append(panel.getConnectionValue(DeployerPanelConnections.STRING.get()).orElse(""));
                    continue block;
                }
            }
        }
        for(var connection : deployer$targetedByExtra.values()) {
            var pos = connection.from.pos();
            if(!getWorld().isLoaded(pos)) {
                return false;
            }
            var state = getWorld().getBlockState(pos);
            var be = getWorld().getBlockEntity(pos);
            var redstoneListener = DeployerPanelConnections.REDSTONE.get().getListener(state.getBlock());
            if(redstoneListener != null && redstoneListener.invalidate(getWorld(), state, pos, be).isPresent()) continue;
            var intListener = DeployerPanelConnections.INTEGER.get().getListener(state.getBlock());
            if(intListener != null) {
                var opt = intListener.invalidate(getWorld(), state, pos, be);
                if (opt.isPresent()) {
                    total += opt.get();
                    continue;
                }
            }
            var listener = DeployerPanelConnections.STRING.get().getListener(state.getBlock());
            if(listener == null) continue;
            var opt = listener.invalidate(getWorld(), state, pos, be);
            if(opt.isPresent()) {
                if(addressChange == null) addressChange = new StringBuilder(opt.get());
                else addressChange.append(opt.get());
            }
        }
        String fAddress = addressChange == null ? null : addressChange.toString();

        if(original || (total != null && total != count) || (fAddress != null && !fAddress.equals(recipeAddress))) {
            if(total != null) count = total;
            if(fAddress != null) recipeAddress = fAddress;
            return true;
        }
        return false;
    }

    @Inject(method = "notifyRedstoneOutputs", at = @At("TAIL"))
    private void notifyRedstoneOutputs(CallbackInfo ci) {
        // Implement for future outputs
    }

    /* INTERACTION */
    @ModifyExpressionValue(method = "onShortInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private boolean onShortInteract(boolean original) {
        var instance = FactoryPanelBehaviour.class.cast(this);
        return instance instanceof AbstractPanelBehaviour panel ? panel.withFilteringBehaviour() && original : original;
    }

    @Definition(id = "heldItem", local = @Local(type = ItemStack.class))
    @Definition(id = "getItem", method = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;")
    @Definition(id = "LogisticallyLinkedBlockItem", type = LogisticallyLinkedBlockItem.class)
    @Expression("heldItem.getItem() instanceof LogisticallyLinkedBlockItem")
    @ModifyExpressionValue(method = "onShortInteract", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean onShortInteract$1(boolean original) {
        var instance = FactoryPanelBehaviour.class.cast(this);
        return original && !(instance instanceof AbstractPanelBehaviour);
    }

    @ModifyExpressionValue(method = "onShortInteract", at = @At(value = "INVOKE", target = "Ljava/util/Map;size()I"))
    private int onShortInteract(int original) {
        return original + deployer$targetedByExtra.size();
    }

    @ModifyExpressionValue(method = "onShortInteract", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private Collection<FactoryPanelConnection> onShortInteract(Collection<FactoryPanelConnection> original) {
        return Stream.concat(original.stream(), deployer$targetedByExtra.values().stream()).collect(Collectors.toSet());
    }

    @Definition(id = "heldItem", local = @Local(type = ItemStack.class, name = "heldItem"))
    @Definition(id = "getItem", method = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;")
    @Definition(id = "LogisticallyLinkedBlockItem", type = LogisticallyLinkedBlockItem.class)
    @Expression("heldItem.getItem() instanceof LogisticallyLinkedBlockItem")
    @ModifyExpressionValue(method = "onShortInteract", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean onShortInteract(boolean original, @Local(name = "heldItem") ItemStack heldItem) {
        return original || heldItem.getItem() instanceof LogisticallyLinked;
    }
}
