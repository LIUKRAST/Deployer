package net.liukrast.deployer.lib.logistics.board;

import com.mojang.serialization.Codec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.codecs.CatnipCodecs;
import net.createmod.catnip.gui.ScreenOpener;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.mixin.accessors.FactoryPanelBehaviourAccessor;
import net.liukrast.deployer.lib.mixin.accessors.FilteringBehaviourAccessor;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a custom factory panel behavior.
 * This class allows the creation of custom panels that extend the standard
 * FactoryPanelBehaviour system from the Create mod.
 * Panels can define custom connections, rendering, item association, and value propagation.
 * Use {@link PanelType} to register your custom panels.
 */
public abstract class AbstractPanelBehaviour extends FactoryPanelBehaviour {
    private final PanelType<?> type;
    private final Reference2ObjectArrayMap<PanelConnection<?>, Supplier<?>> connections = new Reference2ObjectArrayMap<>();

    /**
     * Common color used to warn the user that the connection is currently waiting for the next tick to update
     * */
    @SuppressWarnings("unused")
    protected static final int WAITING = 0xffd541;
    /**
     * Common color used to warn the user that the connection doesn't do anything
     * */
    protected static final int DISABLED = 0x888898;

    /**
     * Constructs a panel behavior with a custom ValueBoxTransform.
     * This constructor allows customizing the input system.
     *
     * @param valueBoxTransform the transform to use for the panel's ValueBox
     * @param type the type of panel
     * @param be the block entity this panel belongs to
     * @param slot the panel slot in the block
     */
    public AbstractPanelBehaviour(ValueBoxTransform valueBoxTransform, PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        this(type, be, slot);
        ((FilteringBehaviourAccessor)this).setValueBoxTransform(valueBoxTransform);
    }

    /**
     * Constructs a panel behavior with the given type.
     *
     * @param type the type of panel
     * @param be the block entity this panel belongs to
     * @param slot the panel slot in the block
     */
    public AbstractPanelBehaviour(PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(be, slot);
        var builder = new PanelConnectionBuilder();
        addConnections(builder);
        connections.putAll(builder.map);
        this.type = type;
    }

    /**
     * Adds new connections to this panel.
     * The order of connection registration is important for panels that
     * can read multiple connections.
     * Use the builder to register each connection along with its getter.
     *
     * @param builder the connection builder used to register panel connections
     */
    public abstract void addConnections(PanelConnectionBuilder builder);

    /**
     * Returns all registered connections for this panel.
     *
     * @return the set of connections
     */
    public Set<PanelConnection<?>> getConnections() {
        return connections.keySet();
    }

    /**
     * Checks if this panel has a specific connection.
     *
     * @param connection the connection to check
     * @param <T> the type of value the connection provides
     * @return true if this panel has the connection, false otherwise
     */
    public <T> boolean hasConnection(DeferredHolder<PanelConnection<?>, PanelConnection<T>> connection) {
        return hasConnection(connection.value());
    }

    /**
     * Checks if this panel has a specific connection.
     *
     * @param connection the connection to check
     * @return true if this panel has the connection, false otherwise
     */
    public boolean hasConnection(PanelConnection<?> connection) {
        return connections.containsKey(connection);
    }

    /**
     * Returns the display component for a link to another panel.
     *
     * @param shortenNumbers whether to use shortened numeric display
     * @return the component used for display
     */
    @SuppressWarnings("unused")
    public MutableComponent getDisplayLinkComponent(boolean shortenNumbers) {
        return Component.empty();
    }

    /**
     * Whether the panel should skip calling {@link FactoryPanelBehaviour#tick()}
     * */
    public boolean skipOriginalTick() {
        return true;
    }

    /**
     * Determines whether this panel should render its bulb.
     *
     * @param original the original rendering value
     * @return true if the bulb should be rendered, false otherwise
     */
    public boolean shouldRenderBulb(@SuppressWarnings("unused") boolean original) {
        return false;
    }

    /**
     * Since original class extends {@link com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour},
     * return true if you want this gauge to have the render from filtering behavior.
     * */
    public boolean withFilteringBehaviour() {
        return false;
    }

    /**
     * @return The item associated with this behavior. Used for drops and more.
     * */
    public abstract Item getItem();

    /**
     * Returns the model for this custom panel.
     *
     * @param panelState the current panel state
     * @param panelType the type of panel
     * @return the PartialModel used for rendering
     */
    public abstract PartialModel getModel(FactoryPanelBlock.PanelState panelState, FactoryPanelBlock.PanelType panelType);

    /**
     * An easier extension of {@link AbstractPanelBehaviour#write(CompoundTag, HolderLookup.Provider, boolean)}.
     * @param nbt The compound tag of the single gauge slot. Save your data into this
     * */
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {}

    /**
     * An easier extension of {@link AbstractPanelBehaviour#read(CompoundTag, HolderLookup.Provider, boolean)}.
     * @param nbt The compound tag of the single gauge slot. Read your data from this slot
     * */
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {}

    /**
     * Opens the editor screen for this panel on the client.
     *
     * @param player the player interacting with the panel
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void displayScreen(Player player) {
        if (player instanceof LocalPlayer)
            ScreenOpener.open(new BasicPanelScreen<>(this));
    }

    /**
     * Determines whether this panel should ignore specific connection issues.
     *
     * @param issue the issue string
     * @return true to ignore, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean ignoreIssue(@Nullable String issue) {
        return "factory_panel.no_item".equals(issue);
    }

    /**
     * Calculates the connection path color when connecting to another panel.
     *
     * @param other the other panel behavior
     * @param original the original color
     * @return the calculated color
     */
    @SuppressWarnings("unused")
    public int calculatePath(FactoryPanelBehaviour other, int original) {
        return DISABLED;
    }

    /**
     * Calculates the connection path color for {@link net.liukrast.deployer.lib.logistics.board.connection.ConnectionExtra} panel elements.
     *
     * @param pos the position of the extra element
     * @return the calculated color
     */
    @SuppressWarnings("unused")
    public int calculateExtraPath(BlockPos pos) {
        return DISABLED;
    }

    /**
     * Returns the value provided by another panel connection.
     *
     * @param connection the panel connection
     * @param <T> the value type
     * @return the optional value
     */
    public <T> Optional<T> getConnectionValue(DeferredHolder<PanelConnection<?>, PanelConnection<T>> connection) {
        return getConnectionValue(connection.get());
    }

    /**
     * Returns the value provided by another panel connection.
     *
     * @param connection the panel connection
     * @param <T> the value type
     * @return the optional value
     */
    public <T> Optional<T> getConnectionValue(PanelConnection<T> connection) {
        if(!connections.containsKey(connection)) return Optional.empty();
        // We can safely cast here.
        //noinspection unchecked
        return Optional.ofNullable((T) connections.get(connection).get());
    }

    /**
     * Executes a consumer for each link connected to this panel.
     *
     * @param consumer the consumer to apply
     */
    @SuppressWarnings("unused")
    public void consumeForLinks(Consumer<FactoryPanelSupportBehaviour> consumer) {
        for(FactoryPanelConnection connection : targetedByLinks.values()) {
            if(!getWorld().isLoaded(connection.from.pos())) return;
            FactoryPanelSupportBehaviour linkAt = linkAt(getWorld(), connection);
            if(linkAt == null) return;
            if(!linkAt.isOutput()) continue;
            consumer.accept(linkAt);
        }
    }

    /**
     * Executes a consumer for each connected panel.
     *
     * @param panelConnection the connection value to retrieve
     * @param consumer the consumer to apply
     * @param toConsider other connections to ignore for priority
     * @param <T> the value type
     */
    @SuppressWarnings("unused")
    public <T> void consumeForPanels(PanelConnection<T> panelConnection, Consumer<T> consumer, PanelConnection<?>... toConsider) {
        block: for(FactoryPanelConnection connection : targetedBy.values()) {
            if(!getWorld().isLoaded(connection.from.pos())) return;
            FactoryPanelBehaviour at = at(getWorld(), connection);
            if(at == null) return;
            for (var c : DeployerPanelConnections.getConnections(at)) {
                for (var consider : toConsider) if (c == consider) continue block;
                if (c == panelConnection) break;
            }
            var opt = DeployerPanelConnections.getConnectionValue(at, panelConnection);
            if(opt.isEmpty()) continue;
            consumer.accept(opt.get());
        }
    }

    /**
     * Executes a consumer for each extra panel element connected.
     *
     * @param panelConnection the connection value to retrieve
     * @param consumer the bi-consumer to apply with block position and value
     * @param <T> the value type
     */
    @SuppressWarnings("unused")
    public <T> void consumeForExtra(PanelConnection<T> panelConnection, BiConsumer<BlockPos, T> consumer) {
        Set<BlockPos> toRemove = new HashSet<>();
        for(var connection : ((FPBExtension)this).deployer$getExtra().values()) {
            var pos = connection.from.pos();
            if(!getWorld().isLoaded(pos)) return;
            var level = getWorld();
            var state = level.getBlockState(pos);
            var be = level.getBlockEntity(pos);
            var listener = panelConnection.getListener(state.getBlock());
            if(listener == null) {
                toRemove.add(connection.from.pos());
                continue;
            }
            var opt = listener.invalidate(level, state, pos, be);
            opt.ifPresent(t -> consumer.accept(pos, t));

        }
        toRemove.forEach(pos -> ((FPBExtension)this).deployer$getExtra().remove(pos));
        if(!toRemove.isEmpty()) blockEntity.notifyUpdate();
    }

    /**
     * @return Allows to get {@link FactoryPanelBehaviour#timer}
     * */
    @SuppressWarnings({"JavadocReference", "unused"})
    public int getTimer() {
        return ((FactoryPanelBehaviourAccessor)this).deployer$getTimer();
    }

    protected void setTimer(int value) {
        ((FactoryPanelBehaviourAccessor)this).deployer$setTimer(value);
    }

    /**
     * @return Allows to get {@link FactoryPanelBehaviour#lastReportedLevelInStorage}
     * */
    @SuppressWarnings({"JavadocReference", "unused"})
    public int getLastReportedLevelInStorage() {
        return ((FactoryPanelBehaviourAccessor)this).deployer$getLastReportedLevelInStorage();
    }

    /**
     * Allows to set {@link FactoryPanelBehaviour#lastReportedLevelInStorage}
     * */
    @SuppressWarnings({"JavadocReference", "unused"})
    protected void setLastReportedLevelInStorage(int value) {
        ((FactoryPanelBehaviourAccessor)this).deployer$setLastReportedLevelInStorage(value);
    }

    /**
     * @return Allows to get {@link FactoryPanelBehaviour#lastReportedUnloadedLinks}
     * */
    @SuppressWarnings({"JavadocReference", "unused"})
    public int getLastReportedUnloadedLinks() {
        return ((FactoryPanelBehaviourAccessor)this).deployer$getLastReportedUnloadedLinks();
    }

    protected void setLastReportedUnloadedLinks(int value) {
        ((FactoryPanelBehaviourAccessor)this).deployer$setLastReportedUnloadedLinks(value);
    }

    /**
     * @return Allows to get {@link FactoryPanelBehaviour#lastReportedPromises}
     * */
    @SuppressWarnings({"JavadocReference", "unused"})
    public int getLastReportedPromises() {
        return ((FactoryPanelBehaviourAccessor)this).deployer$getLastReportedPromises();
    }

    protected void setLastReportedPromises(int value) {
        ((FactoryPanelBehaviourAccessor)this).deployer$setLastReportedPromises(value);
    }

    protected int getPromiseExpiryTimeInTicks() {
        return ((FactoryPanelBehaviourAccessor)this).deployer$invokeGetPromiseExpiryTimeInTicks();
    }

    /**
     * @return the panel type
     */
    public PanelType<?> getPanelType() {
        return type;
    }

    /**
     * Creates the menu for this gauge. Since this is not necessary for a basic gauge, we return null
     * @param containerId The container ID
     * @param playerInventory The player involved's inventory
     * @param player The player involved
     * @return The container to open
     * */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }

    /**
     * Removes the panel
     * */
    @Override
    public void destroy() {
        super.destroy();
        if(blockEntity instanceof FactoryPanelBlockEntity be) {
            var newBehaviour = new FactoryPanelBehaviour(be, this.slot); //TODO: Might break with other mods
            newBehaviour.active = false;
            blockEntity.attachBehaviourLate(newBehaviour);
            be.panels.put(slot, newBehaviour);
            be.redraw = true;
            be.lastShape = null;
            be.notifyUpdate();
        }
    }

    @ApiStatus.Internal
    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        CompoundTag panelTag = nbt.getCompound(CreateLang.asId(slot.name()));
        if (panelTag.isEmpty()) {
            active = false;
            return;
        } else {
            active = true;
        }
        easyRead(panelTag, registries, clientPacket);
    }

    @ApiStatus.Internal
    @Override
    public void writeSafe(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeSafe(nbt, registries);
        CompoundTag special = nbt.contains("CustomPanels") ? nbt.getCompound("CustomPanels") : new CompoundTag();
        special.putString(CreateLang.asId(slot.name()), Objects.requireNonNull(DeployerRegistries.PANEL.getKey(type)).toString());
        nbt.put("CustomPanels", special);
    }

    @ApiStatus.Internal
    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag special = nbt.contains("CustomPanels") ? nbt.getCompound("CustomPanels") : new CompoundTag();
        special.putString(CreateLang.asId(slot.name()), Objects.requireNonNull(DeployerRegistries.PANEL.getKey(type)).toString());
        nbt.put("CustomPanels", special);
        super.write(nbt, registries, clientPacket);
        //We avoid adding some data that is pointless in a generic gauge.
        // You can re-add it in your custom write method though
        //NOTE: If you feel like some data should not be avoided, please open a GitHub issue to report this.
        // This API is still very WIP and support is very well accepted
        if (!active)
            return;

        CompoundTag panelTag = new CompoundTag();
        panelTag.putBoolean("Satisfied", satisfied);
        panelTag.putBoolean("PromisedSatisfied", promisedSatisfied);
        panelTag.putBoolean("RedstonePowered", redstonePowered);
        panelTag.put("Targeting", CatnipCodecUtils.encode(CatnipCodecs.set(FactoryPanelPosition.CODEC), targeting).orElseThrow());
        panelTag.put("TargetedBy", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(targetedBy.values())).orElseThrow());
        panelTag.put("TargetedByLinks", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(targetedByLinks.values())).orElseThrow());
        FPBExtension extra = ((FPBExtension)this);
        panelTag.put("TargetedByExtra", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(extra.deployer$getExtra().values())).orElseThrow());

        if(withFilteringBehaviour()) {
            panelTag.put("Filter", getFilter().saveOptional(registries));
            panelTag.putInt("FilterAmount", count);
            panelTag.putBoolean("UpTo", upTo);
        }

        easyWrite(panelTag, registries, clientPacket);

        nbt.put(CreateLang.asId(slot.name()), panelTag);
    }

    @ApiStatus.Internal
    @Override
    public boolean canShortInteract(ItemStack toApply) {
        return withFilteringBehaviour() && super.canShortInteract(toApply);
    }

    /**
     * @return The filter item that factory gauges will get
     * */
    @Override
    public ItemStack getFilter() {
        return getConnectionValue(DeployerPanelConnections.ITEM_STACK).orElse(ItemStack.EMPTY);
    }

    /**
     * Notifies connected panels of redstone output changes.
     */
    // We invoke the private function through mixin. Create, why are you making this method private...
    @SuppressWarnings("unused")
    public void notifyRedstoneOutputs() {
        for(FactoryPanelPosition panelPos : targeting) {
            if(!getWorld().isLoaded(panelPos.pos()))
                return;
            FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
            if(behaviour == null) continue;
            behaviour.checkForRedstoneInput();
        }
        ((FactoryPanelBehaviourAccessor)this).deployer$invokeNotifyRedstoneOutputs();
    }

    /**
     * @return whether this panel accepts value settings
     */
    @Override
    public boolean acceptsValueSettings() {
        return true;
    }

    /**
     * @return the display name of this panel
     */
    @Override
    public @NotNull Component getDisplayName() {
        return getItem().getDefaultInstance().getHoverName();
    }

    /**
     * @return the items required by this panel
     */
    @Override
    public ItemRequirement getRequiredItems() {
        return isActive() ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, getItem())
                : ItemRequirement.NONE;
    }

    public void tickStorageMonitor() {
        ((FactoryPanelBehaviourAccessor) this).deployer$invokeTickStorageMonitor();
    }

    /**
     * Builder for registering panel connections.
     */
    public static class PanelConnectionBuilder {
        private final Map<PanelConnection<?>, Supplier<?>> map = new Reference2ObjectArrayMap<>();

        private PanelConnectionBuilder() {}

        /**
         * Registers a connection with its getter function.
         *
         * @param panelConnection the panel connection to register
         * @param getter the supplier that provides the connection value
         * @param <T> the value type
         * @return this builder for chaining
         */
        public <T> PanelConnectionBuilder put(@NotNull DeferredHolder<PanelConnection<?>, PanelConnection<T>> panelConnection, @NotNull Supplier<T> getter) {
            return put(panelConnection.get(), getter);
        }

        /**
         * Registers a connection with its getter function.
         *
         * @param panelConnection the panel connection to register
         * @param getter the supplier that provides the connection value
         * @param <T> the value type
         * @return this builder for chaining
         */
        public <T> PanelConnectionBuilder put(@NotNull PanelConnection<T> panelConnection, @NotNull Supplier<T> getter) {
            map.put(panelConnection, getter);
            return this;
        }
    }

    @Nullable
    public BlockEntity getAttachedBlockEntity() {
        FactoryPanelBlockEntity panelBE = panelBE();
        BlockState state = panelBE.getBlockState();
        if(!panelBE.restocker || !AllBlocks.FACTORY_GAUGE.has(state))
            return null;
        BlockPos packagerPos = panelBE.getBlockPos().relative(FactoryPanelBlock.connectedDirection(state)
                .getOpposite());
        assert panelBE.getLevel() != null;
        if (!panelBE.getLevel().isLoaded(packagerPos))
            return null;
        return panelBE.getLevel().getBlockEntity(packagerPos);
    }

    public int getDefaultConnectionAmount() {
        return 1;
    }

    public void addPromises(RequestPromiseQueue queue) {
        queue.add(new RequestPromise(new BigItemStack(getFilter(), recipeOutput)));
    }

}
