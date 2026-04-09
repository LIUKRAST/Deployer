package net.liukrast.deployer.lib.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.liukrast.deployer.lib.logistics.LogisticallyLinked;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.GenericConnections;
import net.liukrast.deployer.lib.logistics.board.connection.*;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.GenericPackagingRequest;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager;
import net.liukrast.deployer.lib.mixin.accessors.LogisticsManagerAccessor;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.mixinExtensions.FPCExtension;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import org.objectweb.asm.Opcodes;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(FactoryPanelBehaviour.class)
public abstract class FactoryPanelBehaviourMixin extends FilteringBehaviour implements FPBExtension, ProvidesConnection {
    //region Unique values
    @Unique private final Map<BlockPos, FactoryPanelConnection> deployer$targetedByExtra = new HashMap<>();
    @Unique private final Set<PanelConnection<?>> deployer$connectionsIn = new LinkedHashSet<>();
    @Unique private final Map<PanelConnection<?>, Supplier<?>> deployer$connectionsOut = new LinkedHashMap<>();
    //endregion
    //region Shadow variables
    @Shadow public Map<FactoryPanelPosition, FactoryPanelConnection> targetedBy;
    @Shadow public UUID network;
    @Shadow public String recipeAddress;
    @Shadow public boolean satisfied;
    @Shadow @Nullable public static FactoryPanelBehaviour at(BlockAndTintGetter world, FactoryPanelConnection connection) {throw new AssertionError("Mixin injection failed");}
    @Shadow protected abstract void sendEffect(FactoryPanelPosition fromPos, boolean success);
    @Shadow public abstract int getLevelInStorage();
    @Shadow public abstract FactoryPanelPosition getPanelPosition();
    @Shadow public Map<BlockPos, FactoryPanelConnection> targetedByLinks;
    //endregion

    public FactoryPanelBehaviourMixin(SmartBlockEntity be, ValueBoxTransform slot) {
        super(be, slot);
    }

    //region Implementing methods from interface
    @Override public Map<BlockPos, FactoryPanelConnection> deployer$getExtra() {
        return deployer$targetedByExtra;
    }

    @Override
    public Set<PanelConnection<?>> deployer$getInputConnections() {
        return deployer$connectionsIn;
    }

    @Override
    public Set<PanelConnection<?>> deployer$getOutputConnections() {
        return deployer$connectionsOut.keySet();
    }

    @Override
    public <T> Optional<T> deployer$getConnectionValue(PanelConnection<T> connection) {
        if(!deployer$connectionsOut.containsKey(connection)) return Optional.empty();
        // We can safely cast here.
        //noinspection unchecked
        return Optional.ofNullable((T) deployer$connectionsOut.get(connection).get());
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public void addConnections(PanelConnectionBuilder builder) {
        builder.registerBoth(DeployerPanelConnections.STOCK_CONNECTION, () -> p -> {});
        builder.registerBoth(DeployerPanelConnections.REDSTONE, () -> this.satisfied && count != 0 ? 15 : 0);
        builder.registerBoth(DeployerPanelConnections.INTEGER, this::getLevelInStorage);
        builder.registerBoth(DeployerPanelConnections.STRING, () -> {
            var source = AllDisplaySources.GAUGE_STATUS.get().createEntry(getWorld(), getPanelPosition());
            return source == null ? null : source.getFirst() + source.getValue().getString();
        });
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public Set<PanelConnection<?>> getInputConnections() {
        return deployer$getInputConnections();
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public Set<PanelConnection<?>> getOutputConnections() {
        return deployer$getOutputConnections();
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public <T> Optional<T> getConnectionValue(PanelConnection<T> connection) {
        return deployer$getConnectionValue(connection);
    }
    //endregion

    //region Injections
    // Through this area you will encounter a lot of descriptions. I really don't have the memory to remember everything I wrote, so I like to leave a "here's why there's an injection here" comment above every injection

    // Adds default connections to the factory gauge and child classes
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot, CallbackInfo ci) {
        addConnections(new PanelConnectionBuilder(deployer$connectionsOut, deployer$connectionsIn));
    }

    // Skips the default tick functions from the factory gauge
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/filtering/FilteringBehaviour;tick()V", shift = At.Shift.AFTER), cancellable = true)
    private void tick(CallbackInfo ci) {
        if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour) ci.cancel();
    }

    // Prevents null values from making the game crash
    @Definition(id = "behaviour", local = @Local(type = FactoryPanelBehaviour.class))
    @Definition(id = "active", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;active:Z") @Expression("behaviour.active")
    @WrapOperation(method = "at(Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;)Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean at(FactoryPanelBehaviour instance, Operation<Boolean> original) {
        if(instance == null) return true;
        return original.call(instance);
    }

    // Instantiates the custom panel instead of the normal one
    @ModifyVariable(method = "moveTo", at = @At(value = "STORE", ordinal = 0))
    private FactoryPanelBehaviour moveTo(FactoryPanelBehaviour at) {
        var be = ((FactoryPanelBlockEntity) at.blockEntity);
        var slot = at.slot;
        if(be.panels.get(slot) instanceof AbstractPanelBehaviour superOriginal)
            return superOriginal.getPanelType().create(be, slot);
        return at;
    }

    // Adds extra targets to the max-range-24 limit
    @Inject(method = "moveTo", at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;", ordinal = 0), cancellable = true)
    private void moveTo(FactoryPanelPosition newPos, ServerPlayer player, CallbackInfo ci) {
        for(BlockPos pos : deployer$targetedByExtra.keySet()) {
            if(!pos.closerThan(newPos.pos(), 24)) {
                ci.cancel();
                return;
            }
        }
    }

    // Initializes the consolidated share
    // Consolidated is a map of all orders to make
    @Inject(method = "tickRequests", at = @At("HEAD"))
    private void tickRequests(
            CallbackInfo ci,
            @Share("real_targeted_by") LocalRef<Map<FactoryPanelPosition, FactoryPanelConnection>> realTargets,
            @Share("consolidated") LocalRef<Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>>> consolidated
    ) {
        consolidated.set(new HashMap<>());
        realTargets.set(targetedBy
                .entrySet()
                .stream()
                .filter(e -> {
                    var connection = e.getValue();
                    FactoryPanelBehaviour source = at(getWorld(), connection);
                    var pc = ProvidesConnection.getCurrentConnection(connection, () -> ProvidesConnection.getPossibleConnections(source, this).stream().findFirst().orElse(null));
                    if(pc == null || pc != DeployerPanelConnections.STOCK_CONNECTION.get()) return false;
                    PanelValue<StockConnection> result = AbstractPanelBehaviour.getValue(connection, DeployerPanelConnections.STOCK_CONNECTION.get(), FactoryPanelBehaviour.class.cast(this));
                    if(result instanceof PanelValue.Present<StockConnection>(StockConnection value)) value.registerStockOrders(new StockConnection.ItemsToOrderProvider(consolidated.get(), source, connection));
                    return true;
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ))
        );
    }

    @ModifyExpressionValue(
            method = "tickRequests",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;targetedBy:Ljava/util/Map;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private Map<FactoryPanelPosition, FactoryPanelConnection> tickRequests(
            Map<FactoryPanelPosition, FactoryPanelConnection> original,
            @Share("real_targeted_by") LocalRef<Map<FactoryPanelPosition, FactoryPanelConnection>> realTargets
    ) {
        return realTargets.get();
    }

    @Inject(method = "tickRequests", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/HashMultimap;create()Lcom/google/common/collect/HashMultimap;"))
    private void tickRequests(
            CallbackInfo ci,
            @Local(name = "failed") LocalBooleanRef failed,
            @Share("consolidated") LocalRef<Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>>> consolidated,
            @Share("toRequest") LocalRef<Map<StockInventoryType<?,?,?>, Multimap<UUID, ?>>> toRequest,
            @Share("saved_uuids") LocalRef<Map<UUID, Integer>> savedUUIDs
    ) {
        toRequest.set(new HashMap<>());
        savedUUIDs.set(new HashMap<>());
        for (var e : consolidated.get().entrySet()) deployer$tickRequests(e.getKey(), e.getValue(), failed, toRequest.get().computeIfAbsent(e.getKey(), k -> HashMultimap.create()));

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
            @Share("saved_uuids") LocalRef<Map<UUID, Integer>> savedUUIDs,
            @Share("isValsEmpty") LocalBooleanRef isValsEmpty
    ) {
        var vals = request.values();
        int id = vals.isEmpty() ? LogisticsManagerAccessor.getR().nextInt() :
                Optional.ofNullable(vals.iterator().next()).map(PackagingRequest::orderId)
                                .orElseGet(() -> LogisticsManagerAccessor.getR().nextInt());
        savedUUIDs.get().put(entry.getKey(), id);
        isValsEmpty.set(vals.isEmpty());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Definition(id = "requests", local = @Local(type = List.class, name = "requests"))
    @Definition(id = "iterator", method = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    @Expression("requests.iterator()")
    @Inject(method = "tickRequests", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0), cancellable = true)
    private void tickRequests(
            CallbackInfo ci,
            @Local(name = "requests") List<Multimap<PackagerBlockEntity, PackagingRequest>> requests,
            @Share("toRequest") LocalRef<Map<StockInventoryType<?,?,?>, Multimap<UUID, ?>>> toRequest,
            @Share("saved_uuids") LocalRef<Map<UUID, Integer>> savedUUIDs,
            @Share("isValsEmpty") LocalBooleanRef isValsEmpty
    ) {
        Map<StockInventoryType<?,?,?>, Map<UUID, Collection<?>>> asMap = new HashMap<>();
        Map<StockInventoryType<?,?,?>, List<Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>>>> d$requests = new HashMap<>();

        // Map init
        for(var e : toRequest.get().entrySet()) {
            asMap.put(e.getKey(), (Map)e.getValue().asMap());
            d$requests.put(e.getKey(), new ArrayList<>());
        }

        // Collect request distributions
        for(var e : asMap.entrySet()) {
            deployer$tickRequests(e.getKey(), e.getValue(), d$requests.get(e.getKey()), savedUUIDs.get());
        }

        // Check if any packager is busy, cancel all
        for(var e : d$requests.values()) {
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
        for(var e : d$requests.entrySet()) {
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

        if(oneFound)
            for(var e : requests) {
                e.values().forEach(pr -> ((PRExtension)(Object)pr).deployer$flag());
            }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Unique
    private <K,V,H> void deployer$tickRequests(StockInventoryType<K,V,H> type, Map<UUID, Collection<?>> asMap$raw, List<Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>>> requests$raw, Map<UUID, Integer> savedUUIDs) {
        Map<UUID, Collection<V>> asMap = (Map) asMap$raw;
        List<Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>>> requests = (List)requests$raw;
        for(var entry : asMap.entrySet()) {
            var order = type.valueHandler().createContained(new ArrayList<>(entry.getValue()));
            Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> request =
                    LogisticsGenericManager.findPackagersForRequest(type, entry.getKey(), order, null, recipeAddress, () -> savedUUIDs.computeIfAbsent(entry.getKey(), key -> LogisticsManagerAccessor.getR().nextInt()));
            requests.add(request);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Unique
    private <K,V,H> void deployer$tickRequests(Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>> entry$raw, int index, boolean isLast) {
        Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> entry = (Multimap)entry$raw;
        LogisticsGenericManager.performPackageRequests(entry, index, isLast);
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

    @ModifyExpressionValue(
            method = "addConnection",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;restocker:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean addConnection(boolean original) {
        if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour) return false;
        return original;
    }

    @Definition(id = "source", local = @Local(type = FactoryPanelBehaviour.class, name = "source"))
    @Expression("source == null")
    @ModifyExpressionValue(
            method = "addConnection",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean addConnection(boolean original, @Local(name = "source") FactoryPanelBehaviour source) {
        var inst = FactoryPanelBehaviour.class.cast(this);
        // if true cancel, false is "connect"
        // If a panel is null cancel
        if(original) return true;

        if(!(inst instanceof AbstractPanelBehaviour apb)) {
            if(!(source instanceof AbstractPanelBehaviour apb1)) return false; // Continue
            return apb1.canPoint(inst) != null; // If an error is present -> return true -> cancel
        } else {
            if(!(source instanceof AbstractPanelBehaviour apb1)) return apb.canBePointed(source) != null; // Continue
            return apb1.canPoint(inst) != null || apb.canBePointed(apb1) != null;
        }
    }

    @ModifyArg(
            method = "addConnection",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 0
            ),
            index = 1
    )
    private <K> K addConnection(K original, @Local(name = "link") FactoryPanelSupportBehaviour link) {
        FactoryPanelConnection conn = ((FactoryPanelConnection)original);
        if(link instanceof AbstractPanelSupportBehaviour apsb) {
            Supplier<PanelConnection<?>> def = apsb.isOutput() ?
                    () -> ProvidesConnection.getPossibleConnections(this, apsb).stream().findFirst().orElse(null) :
                    () -> ProvidesConnection.getPossibleConnections(apsb, this).stream().findFirst().orElse(null);
            var pc = ProvidesConnection.getCurrentConnection(conn, def);
            ((FPCExtension)conn).deployer$setLinkMode(pc);
        } else if(link.isOutput() && getInputConnections().contains(DeployerPanelConnections.REDSTONE.get()) || (!link.isOutput() && getOutputConnections().contains(DeployerPanelConnections.REDSTONE.get())))
            ((FPCExtension)conn).deployer$setLinkMode(DeployerPanelConnections.REDSTONE.get());
        return original;
    }

    @ModifyArg(
            method = "addConnection",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 1
            ),
            index = 1
    )
    private <K> K addConnection$1(K original, @Local(name = "source") FactoryPanelBehaviour source) {
        FactoryPanelConnection conn = ((FactoryPanelConnection)original);
        var pc = ProvidesConnection.getCurrentConnection(conn, () -> ProvidesConnection.getPossibleConnections(source, this).stream().findFirst().orElse(null));
        ((FPCExtension)conn).deployer$setLinkMode(pc);
        return original;
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

    @Unique
    @Nullable
    private <T> List<T> deployer$getAllValues(PanelConnection<T> connection) {
        List<T> out = new ArrayList<>();
        boolean shouldAbort = Stream.of(targetedBy.values(), targetedByLinks.values(), this.deployer$getExtra().values())
                .flatMap(Collection::stream)
                .anyMatch(gauge -> {
                    PanelValue<T> result = AbstractPanelBehaviour.getValue(gauge, connection, FactoryPanelBehaviour.class.cast(this));
                    if (result instanceof PanelValue.Abort) return true;
                    if (result instanceof PanelValue.Present<T>(T value)) out.add(value);
                    return false;
                });

        return shouldAbort ? null : out;
    }

    /* OTHER PANELS UPDATE */
    @ModifyVariable(method = "checkForRedstoneInput", at = @At(value = "STORE", ordinal = 0), name = "shouldPower")
    private boolean checkForRedstoneInput(boolean shouldPower) {
        var li = deployer$getAllValues(DeployerPanelConnections.REDSTONE.get());
        if(li == null) return false;
        return li.stream().anyMatch(k -> k > 0);
    }

    @Definition(id = "shouldPower", local = @Local(type = boolean.class))
    @Definition(id = "redstonePowered", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;redstonePowered:Z")
    @Expression("shouldPower == this.redstonePowered")
    @ModifyExpressionValue(method = "checkForRedstoneInput", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean checkForRedstoneInput$1(boolean original) {
        var nums = deployer$getAllValues(DeployerPanelConnections.INTEGER.get());
        var strs = deployer$getAllValues(DeployerPanelConnections.STRING.get());
        if (nums == null || strs == null) return false;

        Integer total = nums.isEmpty() ? null : nums.stream().reduce(0, Integer::sum);
        String fAddress = strs.isEmpty() ? null : String.join("", strs);

        boolean changed = original
                || (total != null && !total.equals(count))
                || (fAddress != null && !fAddress.equals(recipeAddress));

        if (changed) {
            if (total != null) count = total;
            if (fAddress != null) recipeAddress = fAddress;
            return true;
        }

        return false;
    }

    /* INTERACTION */
    @ModifyExpressionValue(method = "onShortInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private boolean onShortInteract(boolean original) {
        var instance = FactoryPanelBehaviour.class.cast(this);
        return !(instance instanceof AbstractPanelBehaviour) && original;
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
