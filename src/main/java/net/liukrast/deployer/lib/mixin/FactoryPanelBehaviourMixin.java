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
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.liukrast.deployer.lib.blockEntity.behaviour.advancedValueSettings.AdvancedValueSettingsBehaviour;
import net.liukrast.deployer.lib.blockEntity.behaviour.advancedValueSettings.RowData;
import net.liukrast.deployer.lib.logistics.IPromiseVisuals;
import net.liukrast.deployer.lib.logistics.LogisticallyLinked;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.PanelRequestHelper;
import net.liukrast.deployer.lib.logistics.board.connection.*;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.mixinExtensions.FPCExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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

@Mixin(FactoryPanelBehaviour.class)
public abstract class FactoryPanelBehaviourMixin extends FilteringBehaviour implements FPBExtension, ProvidesConnection, IPromiseVisuals, AdvancedValueSettingsBehaviour {
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
        builder.registerBoth(DeployerPanelConnections.STOCK_CONNECTION, () -> null);
        builder.registerBoth(DeployerPanelConnections.REDSTONE, () -> this.satisfied && count != 0);
        builder.registerBoth(DeployerPanelConnections.NUMBERS, () -> (float)getLevelInStorage());
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

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public boolean valueEditBoxEnabled() {
        //noinspection ConstantValue
        return !(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour);
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<RowData> getRowData() {
        return List.of(
                new RowData(CreateLang.translate("schedule.condition.threshold.items").component(), 0, 1_000_000),
                new RowData(CreateLang.translate("schedule.condition.threshold.stacks").component(), 0, 1_000_000)
        );
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public void setValueSettings(Player player, int row, double value, boolean ctrlDown) {
        setValueSettings(player, new ValueSettings(row, (int)value), ctrlDown);
    }

    //endregion

    //region Injections
    // Through this area you will encounter a lot of descriptions. I really don't have the memory to remember everything I wrote, so I like to leave a "here's why there's an injection here" comment above every injection

    // Adds default connections to the factory gauge and child classes
    @Inject(method = "<init>", at = @At("RETURN"))
    private void deployer$registerDefaultConnections(FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot, CallbackInfo ci) {
        addConnections(new PanelConnectionBuilder(deployer$connectionsOut, deployer$connectionsIn));
    }

    // Skips the default tick functions from the factory gauge
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z"), cancellable = true)
    private void deployer$skipVanillaTickForCustomPanels(CallbackInfo ci) {
        if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour) ci.cancel();
    }

    @Inject(method = "lazyTick", at = @At("RETURN"))
    private void deployer$pruneExtraTargets(CallbackInfo ci) {
        var level = getWorld();
        deployer$targetedByExtra.keySet().removeIf(pos -> level.isLoaded(pos) && !PanelConnection.hasAnyListener(level.getBlockState(pos).getBlock()));
    }

    // Prevents null values from making the game crash
    @Definition(id = "behaviour", local = @Local(type = FactoryPanelBehaviour.class))
    @Definition(id = "active", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;active:Z") @Expression("behaviour.active")
    @WrapOperation(method = "at(Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;)Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean deployer$allowMissingBehaviour(FactoryPanelBehaviour instance, Operation<Boolean> original) {
        if(instance == null) return true;
        return original.call(instance);
    }

    // Instantiates the custom panel instead of the normal one
    @ModifyVariable(method = "moveTo", at = @At(value = "STORE", ordinal = 0))
    private FactoryPanelBehaviour deployer$recreateCustomPanel(FactoryPanelBehaviour at) {
        var be = ((FactoryPanelBlockEntity) at.blockEntity);
        var slot = at.slot;
        if(be.panels.get(slot) instanceof AbstractPanelBehaviour superOriginal)
            return superOriginal.getPanelType().create(be, slot);
        return at;
    }

    // Adds extra targets to the max-range-24 limit
    @Inject(method = "moveTo", at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;", ordinal = 0), cancellable = true)
    private void deployer$keepExtraTargetsInRange(FactoryPanelPosition newPos, ServerPlayer player, CallbackInfo ci) {
        for(BlockPos pos : deployer$targetedByExtra.keySet()) {
            if(!pos.closerThan(newPos.pos(), 24)) {
                ci.cancel();
                return;
            }
        }
    }

    @Definition(id = "heldItem", local = @Local(type = ItemStack.class, name = "heldItem"))
    @Definition(id = "getItem", method = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;")
    @Definition(id = "LogisticallyLinkedBlockItem", type = LogisticallyLinkedBlockItem.class)
    @Expression("heldItem.getItem() instanceof LogisticallyLinkedBlockItem")
    @ModifyExpressionValue(method = "onShortInteract", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private boolean deployer$acceptLinkedItems(boolean original, @Local(name = "heldItem") ItemStack heldItem) {
        return (heldItem.getItem() instanceof LogisticallyLinked) || original;
    }

    @Inject(method = "tickRequests", at = @At(value = "NEW", target = "java/util/HashMap", ordinal = 0), cancellable = true)
    private void deployer$collectStockTargets(CallbackInfo ci, @Share("targets") LocalRef<PanelRequestHelper.StockTargets> targets) {
        var collected = PanelRequestHelper.collectStockTargets(FactoryPanelBehaviour.class.cast(this));
        if (collected == null || collected.isEmpty()) {
            ci.cancel();
            return;
        }
        targets.set(collected);
    }

    @ModifyExpressionValue(
            method = "tickRequests",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;targetedBy:Ljava/util/Map;",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 1
            )
    )
    private Map<FactoryPanelPosition, FactoryPanelConnection> deployer$onlyVanillaTargets(
            Map<FactoryPanelPosition, FactoryPanelConnection> original,
            @Share("targets") LocalRef<PanelRequestHelper.StockTargets> targets
    ) {
        return targets.get().vanillaTargets();
    }

    @ModifyExpressionValue(method = "tickRequests", at = @At(value = "NEW", target = "()Ljava/util/HashMap;"))
    private HashMap<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> deployer$addCustomItemTargets(
            HashMap<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> original,
            @Share("targets") LocalRef<PanelRequestHelper.StockTargets> targets
    ) {
        original.putAll(targets.get().items());
        return original;
    }

    @Inject(method = "tickRequests", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/HashMultimap;create()Lcom/google/common/collect/HashMultimap;"))
    private void deployer$collectGenericStock(
            CallbackInfo ci,
            @Local(name = "failed") LocalBooleanRef failed,
            @Share("targets") LocalRef<PanelRequestHelper.StockTargets> targets,
            @Share("genericToRequest") LocalRef<Map<StockInventoryType<?, ?, ?>, Multimap<UUID, ?>>> genericToRequest,
            @Share("orderIds") LocalRef<Map<UUID, Integer>> orderIds
    ) {
        genericToRequest.set(PanelRequestHelper.collectAvailableGenericStock(
                targets.get().generic(),
                (from, success) -> sendEffect(from, success),
                () -> failed.set(true)
        ));
        orderIds.set(new HashMap<>());
    }

    @Inject(method = "tickRequests", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void deployer$captureOrderId(
            CallbackInfo ci,
            @Local(name = "entry") Map.Entry<UUID, Collection<BigItemStack>> entry,
            @Local(name = "request") Multimap<PackagerBlockEntity, PackagingRequest> request,
            @Share("orderIds") LocalRef<Map<UUID, Integer>> orderIds,
            @Share("lastRequestEmpty") LocalBooleanRef lastRequestEmpty
    ) {
        orderIds.get().put(entry.getKey(), PanelRequestHelper.orderIdOf(request));
        lastRequestEmpty.set(request.isEmpty());
    }

    @Definition(id = "requests", local = @Local(type = List.class, name = "requests"))
    @Definition(id = "iterator", method = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    @Expression("requests.iterator()")
    @Inject(method = "tickRequests", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0), cancellable = true)
    private void deployer$sendGenericRequests(
            CallbackInfo ci,
            @Local(name = "requests") List<Multimap<PackagerBlockEntity, PackagingRequest>> requests,
            @Share("genericToRequest") LocalRef<Map<StockInventoryType<?, ?, ?>, Multimap<UUID, ?>>> genericToRequest,
            @Share("orderIds") LocalRef<Map<UUID, Integer>> orderIds,
            @Share("lastRequestEmpty") LocalBooleanRef lastRequestEmpty
    ) {
        var genericRequests = PanelRequestHelper.findGenericPackagers(genericToRequest.get(), recipeAddress, orderIds.get());
        if (genericRequests.anyPackagerBusy()) {
            ci.cancel();
            return;
        }
        if (genericRequests.send(lastRequestEmpty.get() ? 0 : 1))
            PanelRequestHelper.flagVanillaRequests(requests);
    }

    /* DATA */
    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putUUID(Ljava/lang/String;Ljava/util/UUID;)V"))
    private void deployer$writeExtraTargets(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci, @Local(name = "panelTag") CompoundTag panelTag) {
        panelTag.put("TargetedByExtra", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(deployer$targetedByExtra.values())).orElseThrow());
    }

    @Inject(method = "read", at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"))
    private void deployer$readExtraTargets(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci, @Local(name = "panelTag") CompoundTag panelTag) {
        deployer$targetedByExtra.clear();
        CatnipCodecUtils.decode(Codec.list(FactoryPanelConnection.CODEC), panelTag.get("TargetedByExtra")).orElse(List.of())
                .forEach(c -> deployer$targetedByExtra.put(c.from.pos(), c));
    }

    /* ADDING/REMOVING CONNECTIONS */

    // Adds extra connections
    @Inject(method = "addConnection", at = @At("HEAD"), cancellable = true)
    private void deployer$connectExtraSource(FactoryPanelPosition fromPos, CallbackInfo ci) {
        var i = FactoryPanelBehaviour.class.cast(this);
        var fromState = i.getWorld().getBlockState(fromPos.pos());
        if(PanelConnection.makeContext(i.getWorld().getBlockState(i.getPos())) == PanelConnection.makeContext(fromState) && PanelConnection.hasAnyListener(fromState.getBlock())) {
            var conn = new FactoryPanelConnection(fromPos, 1);
            deployer$targetedByExtra.put(fromPos.pos(), conn);
            var pc = ProvidesConnection.getCurrentConnection(conn, () -> getInputConnections()
                    .stream()
                    .filter(c -> c.getListener(fromState.getBlock()) != null)
                    .findFirst().orElse(null));
            ((FPCExtension)conn).deployer$setLinkMode(pc);
            if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour apb)
                apb.onConnectionAdded(conn);
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
    private boolean deployer$ignoreRestockerForCustomPanels(boolean original) {
        if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour) return false;
        return original;
    }

    @Definition(id = "source", local = @Local(type = FactoryPanelBehaviour.class, name = "source"))
    @Expression("source == null")
    @ModifyExpressionValue(
            method = "addConnection",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean deployer$rejectInvalidCustomConnection(boolean original, @Local(name = "source") FactoryPanelBehaviour source) {
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
    private <K> K deployer$assignLinkMode(K original, @Local(name = "link") FactoryPanelSupportBehaviour link) {
        FactoryPanelConnection conn = ((FactoryPanelConnection)original);
        if(link instanceof AbstractPanelSupportBehaviour apsb) {
            Supplier<PanelConnection<?>> def = apsb.isOutput() ?
                    () -> ProvidesConnection.getPossibleConnections(apsb, this).stream().findFirst().orElse(null) :
                    () -> ProvidesConnection.getPossibleConnections(this, apsb).stream().findFirst().orElse(null);
            var pc = ProvidesConnection.getCurrentConnection(conn, def);
            ((FPCExtension)conn).deployer$setLinkMode(pc);
        } else if(link.isOutput() && getInputConnections().contains(DeployerPanelConnections.REDSTONE.get()) || (!link.isOutput() && getOutputConnections().contains(DeployerPanelConnections.REDSTONE.get())))
            ((FPCExtension)conn).deployer$setLinkMode(DeployerPanelConnections.REDSTONE.get());
        if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour apb)
            apb.onConnectionAdded(conn);
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
    private <K> K deployer$assignPanelLinkMode(K original, @Local(name = "source") FactoryPanelBehaviour source) {
        FactoryPanelConnection conn = ((FactoryPanelConnection)original);
        var pc = ProvidesConnection.getCurrentConnection(conn, () -> ProvidesConnection.getPossibleConnections(source, this).stream().findFirst().orElse(null));
        ((FPCExtension)conn).deployer$setLinkMode(pc);
        if(FactoryPanelBehaviour.class.cast(this) instanceof AbstractPanelBehaviour apb)
            apb.onConnectionAdded(conn);
        return original;
    }

    @ModifyArg(method = "addConnection", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelConnection;<init>(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;I)V", ordinal = 1), index = 1)
    private int deployer$defaultConnectionAmount(int amount, @Local(name = "source") FactoryPanelBehaviour source) {
        if(source instanceof AbstractPanelBehaviour apb) {
            return apb.getDefaultConnectionAmount();
        }
        return amount;
    }

    @Inject(method = "disconnectAllLinks", at = @At("TAIL"))
    private void deployer$clearExtraTargets(CallbackInfo ci) {
        deployer$targetedByExtra.clear();
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public @Nullable <T> List<AbstractPanelBehaviour.ConnectionValue<T>> getAllValuesWithSource(PanelConnection<T> connection) {
        return AbstractPanelBehaviour.collectValues(FactoryPanelBehaviour.class.cast(this), connection);
    }

    @ModifyVariable(method = "checkForRedstoneInput", at = @At(value = "STORE", ordinal = 0), name = "shouldPower")
    private boolean deployer$readRedstoneInputs(boolean shouldPower) {
        var inputs = getAllValues(DeployerPanelConnections.REDSTONE.get());
        return inputs != null && inputs.contains(true);
    }

    @Definition(id = "shouldPower", local = @Local(type = boolean.class))
    @Definition(id = "redstonePowered", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;redstonePowered:Z")
    @Expression("shouldPower == this.redstonePowered")
    @ModifyExpressionValue(method = "checkForRedstoneInput", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean deployer$applyNumberAndAddressInputs(boolean redstoneUnchanged) {
        boolean valuesChanged = deployer$applyCountInput() | deployer$applyAddressInput();
        if (valuesChanged && redstoneUnchanged)
            blockEntity.notifyUpdate();
        return redstoneUnchanged;
    }

    @Unique
    private boolean deployer$applyCountInput() {
        var numbers = getAllValues(DeployerPanelConnections.NUMBERS.get());
        if (numbers == null || numbers.isEmpty()) return false;
        float sum = 0;
        for (float number : numbers) sum += number;
        int total = (int) sum;
        if (total == count) return false;
        count = total;
        return true;
    }

    @Unique
    private boolean deployer$applyAddressInput() {
        var strings = getAllValues(DeployerPanelConnections.STRING.get());
        if (strings == null || strings.isEmpty()) return false;
        String address = String.join("", strings);
        if (address.equals(recipeAddress)) return false;
        recipeAddress = address;
        return true;
    }
}
