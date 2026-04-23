package net.liukrast.deployer.lib.logistics.board;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.codecs.CatnipCodecs;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.DeployerConfig;
import net.liukrast.deployer.lib.logistics.board.connection.*;
import net.liukrast.deployer.lib.logistics.board.screen.BasicPanelScreen;
import net.liukrast.deployer.lib.mixin.accessors.FilteringBehaviourAccessor;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.TriPredicate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * <h1>AbstractPanelBehaviour</h1>
 * <p>
 * While in {@code Create} "factory panels" are specific stocking-related blocks,
 * this API generalizes the concept.
 * </p>
 * <p>
 * A panel is defined as a {@code Node} capable of establishing connections with other panels.
 * A panel can act as a source (pointing to another), a target (being pointed to), or both.
 * Depending on their implementation, panels can provide data (outputs), consume data (inputs),
 * or handle bidirectional data flow.
 * </p>
 * <p>
 * And what about create's factory gauges, are they still part of this API?
 * Yes. Factory gauges are no exception. They take item as input from other gauges, put them in a crafting grid, and order them. That's it. They can output several values, including redstone, numbers, and more
 * </p>
 */
public abstract class AbstractPanelBehaviour extends FactoryPanelBehaviour implements ProvidesConnection {
    private int cycled = 0;
    // region Private attributes & static values
    private final PanelType<?> type;
    private final HashMap<String, InteractionData> interactions = new HashMap<>();

    protected static final int WAITING = ConnectionLine.pack(0xffd541, false, true);
    protected static final int DISABLED = ConnectionLine.pack(0x888898, false, false);
    //endregion
    //region Constructors
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
        // connections are built in superclass
        Map<String, TriPredicate<Level, BlockPos, BlockState>> map = new HashMap<>();
        addInteractions(new PanelInteractionBuilder(map));
        for(var e : map.entrySet()) interactions.put(e.getKey(), new InteractionData(e.getValue(), this));
        this.type = type;
    }
    //endregion
    //region Connections
    /**
     * Connections are the core process of a panel. This method is called when your panel is created and will want you to decide what the panel needs to handle.
     * When you specify an output, you will be asked to provide a {@link Supplier}. That will tell other gauges trying to read this what value this gauge is holding, in real time.
     * On the other side, specifying inputs is only used as "declaration", you declare that this gauge will eventually read that type of value, so that we know what connections you can pick in the connecting cable.
     * Order matters! In fact, when you first connect two panels, the system will automatically
     * establish a link based on the first compatible data type found in the registration order
     * of the input panel.
     * * <p>
     * If there are no shared data types between the source's outputs and the target's inputs,
     * the connection will remain inactive (no data will flow). However, if multiple data types
     * are compatible, the first one registered in the input panel's builder takes priority
     * as the default.
     * </p>
     * * <p>
     * Users can manually override this selection using a Wrench on the connection.
     * The manual selection is restricted to the intersection of available types:
     * a connection can only be established for a type that is both provided by the output
     * panel and accepted by the input panel.
     * </p>
     */
    public abstract void addConnections(PanelConnectionBuilder builder);

    /**
     * @return The set (ordered) containing all connections this panel outputs.
     * */
    public Set<PanelConnection<?>> getInputConnections() {
        //We delegate to a mixin the super call. Feel free to handle this just like a normal method
        return ((FPBExtension)this).deployer$getInputConnections();
    }

    /**
     * @return The set (ordered) containing all connections this panel reads.
     * */
    public Set<PanelConnection<?>> getOutputConnections() {
        //We delegate to a mixin the super call. Feel free to handle this just like a normal method
        return ((FPBExtension)this).deployer$getOutputConnections();
    }

    /**
     * Returns the value provided by another panel connection.
     *
     * @param connection the panel connection
     * @param <T> the value type
     * @return the optional value
     */
    public <T> Optional<T> getConnectionValue(PanelConnection<T> connection) {
        //We delegate to a mixin the super call. Feel free to handle this just like a normal method
        return ((FPBExtension)this).deployer$getConnectionValue(connection);
    }

    /**
     * Fetches a specific value from a connected source (Panel, Support, or Block).
     * <p>
     * This method handles the logic for different connection types and ensures
     * that the requested {@link PanelConnection} matches the link's active mode.
     * </p>
     *
     * @param position   the connection object representing the link
     * @param connection the type of data connection requested
     * @param source     the panel acting as the input/consumer
     * @param <T>        the type of the value
     * @return a {@link PanelValue} representing the result:
     * <ul>
     * <li>{@link PanelValue.Abort}: if the chunk is unloaded (stop all updates)</li>
     * <li>{@link PanelValue.Empty}: if no value is found or connection types mismatch</li>
     * <li>{@link PanelValue.Present}: if the value was successfully retrieved</li>
     * </ul>
     * @throws IllegalStateException if the connection is not registered in any of the source's maps
     */
    @NotNull
    public static <T> PanelValue<T> getValue(FactoryPanelConnection position, PanelConnection<T> connection, FactoryPanelBehaviour source) {
        if(!source.getWorld().isLoaded(position.from.pos())) return PanelValue.abort();
        if(source.targetedBy.containsValue(position)) {
            FactoryPanelBehaviour at = at(source.getWorld(), position);
            if(at == null) return PanelValue.empty();
            var pc = ProvidesConnection.getCurrentConnection(position, () -> ProvidesConnection.getPossibleConnections(at, source).stream().findFirst().orElse(null));
            if(pc == null || pc != connection) return PanelValue.empty();
            return PanelValue.of(((ProvidesConnection)at).getConnectionValue(connection));
        }
        if(source.targetedByLinks.containsValue(position)) {
            FactoryPanelSupportBehaviour linkAt = linkAt(source.getWorld(), position);
            if(linkAt == null) return PanelValue.empty();
            if(!linkAt.isOutput()) return PanelValue.empty();
            if(linkAt instanceof AbstractPanelSupportBehaviour apsb) {
                var pc = ProvidesConnection.getCurrentConnection(position, () -> ProvidesConnection.getPossibleConnections(apsb, source).stream().findFirst().orElse(null));
                if(pc == null || pc != connection) return PanelValue.empty();
                return PanelValue.of(apsb.getConnectionValue(connection));
            } else if(connection == DeployerPanelConnections.REDSTONE.get()) //noinspection unchecked
                return PanelValue.of((T)(Boolean)(linkAt.shouldPanelBePowered()));
            else return PanelValue.empty();
        }
        if(((FPBExtension)source).deployer$getExtra().containsValue(position)) {
            var pos = position.from.pos();
            var level = source.getWorld();
            var state = level.getBlockState(pos);

            var pc = ProvidesConnection.getCurrentConnection(position, () -> DeployerRegistries.PANEL_CONNECTION.stream()
                    .filter(c -> ((ProvidesConnection)source).getInputConnections().contains(c))
                    .filter(c -> c.getListener(state.getBlock()) != null)
                    .findFirst().orElse(null)
            );
            if(pc == null || pc != connection) return PanelValue.empty();
            var listener = connection.getListener(state.getBlock());
            if(listener == null) return PanelValue.empty();
            return PanelValue.of(listener.invalidate(level, state, pos, level.getBlockEntity(pos)));
        }
        throw new IllegalStateException("Cannot get value on a panel not connected to the input one (Position: " + position.from.pos() + ")");
    }

    public record ConnectionValue<T>(FactoryPanelConnection connection, T value) {}

    /**
     * Aggregates all values from all connected sources for a specific connection type.
     * <p>
     * This version uses a unified stream to process standard panels, links, and extra
     * block connections, maintaining the "Abort" logic for unloaded chunks.
     * </p>
     *
     * @param connection the type of data connection to poll
     * @param <T>        the type of the values
     * @return a {@link List} of all values and their {@link FactoryPanelConnection}, or {@code null} if any source
     * returned an "Abort" state.
     */
    @Override
    public @Nullable <T> List<ConnectionValue<T>> getAllValuesWithSource(PanelConnection<T> connection) {
        List<AbstractPanelBehaviour.ConnectionValue<T>> out = new ArrayList<>();
        boolean shouldAbort = Stream.of(targetedBy.values(), targetedByLinks.values(), getTargetedByExtra().values())
                .flatMap(Collection::stream)
                .anyMatch(gauge -> {
                    PanelValue<T> result = getValue(gauge, connection, this);
                    if (result instanceof PanelValue.Abort) return true;
                    if (result instanceof PanelValue.Present<T>(T value))
                        out.add(new AbstractPanelBehaviour.ConnectionValue<>(gauge, value));
                    return false;
                });

        return shouldAbort ? null : out;
    }


    //endregion
    //region Util functions
    /**
     * Returns the physical {@link Item} associated with this behavior.
     * This is primarily used for block drops and inventory interactions.
     *
     * @return the item representative of this panel behavior
     */
    public abstract Item getItem();

    public List<ItemStack> getItemDrops() {
        return List.of(getItem().getDefaultInstance());
    }

    /**
     * Determines the visual model for this panel during rendering.
     *
     * @param panelState the current state of the panel (e.g., orientation, power)
     * @param panelType  the specific sub-type of the panel
     * @return the {@link PartialModel} to be rendered by the {@code FactoryPanelRenderer}
     */
    public abstract PartialModel getModel(FactoryPanelBlock.PanelState panelState, FactoryPanelBlock.PanelType panelType);

    /**
     * Simplified NBT writing operation. Use this to save custom data into the
     * specific NBT tag reserved for this behavior.
     *
     * @param nbt          the behavior's dedicated {@link CompoundTag}
     * @param registries   the provider for registry-based data (e.g., block entities)
     * @param clientPacket true if this data is being sent to the client (S2C)
     */
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {}

    /**
     * Simplified NBT reading operation. Use this to restore custom data from the
     * specific NBT tag reserved for this behavior.
     *
     * @param nbt          the behavior's dedicated {@link CompoundTag}
     * @param registries   the provider for registry-based data
     * @param clientPacket true if this data is being received on the client (S2C)
     */
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {}

    /**
     * Checks if a general connection can be established between this panel and another.
     * <p>
     * If the connection is blocked, returning a translation key will show a
     * warning message in the player's hotbar (prefixed with {@code create.}).
     * </p>
     *
     * @param from the panel attempting to connect
     * @return a translation key (e.g., "message.invalid_connection") if failed,
     * or {@code null} if the connection is allowed
     * @see #canBePointed(FactoryPanelBehaviour)
     * @see #canPoint(FactoryPanelBehaviour)
     */
    public String canConnect(FactoryPanelBehaviour from) {
        return null;
    }

    /**
     * Checks specifically if this panel can be the **target** of an incoming connection.
     *
     * @param from the panel attempting to point to this instance
     * @return a translation key if blocked, or {@code null} if allowed
     */
    @Nullable
    public String canBePointed(FactoryPanelBehaviour from) {
        return canConnect(from);
    }

    /**
     * Checks specifically if this panel can be the **source** pointing to another panel.
     *
     * @param to the panel this instance is attempting to connect to
     * @return a translation key if blocked, or {@code null} if allowed
     */
    @Nullable
    public String canPoint(FactoryPanelBehaviour to) {
        return canConnect(to);
    }

    /**
     * Forces an update notification to all panels being pointed to by this instance.
     * Use this whenever your output value changes to trigger logic updates or
     * redstone re-evaluations in connected panels.
     */
    @SuppressWarnings("unused")
    public void notifyOutputs() {
        for(FactoryPanelPosition panelPos : targeting) {
            if(!getWorld().isLoaded(panelPos.pos()))
                return;
            FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
            if(behaviour == null) continue;
            behaviour.checkForRedstoneInput();
        }
        for (FactoryPanelConnection connection : targetedByLinks.values()) {
            if (!getWorld().isLoaded(connection.from.pos()))
                return;
            FactoryPanelSupportBehaviour linkAt = linkAt(getWorld(), connection);
            if (linkAt == null)
                return;
            if(linkAt.isOutput()) continue;
            linkAt.notifyLink();
        }
    }

    @Override
    public final void checkForRedstoneInput() {
        if(cycled >= DeployerConfig.Server.FACTORY_PANEL_MAX_CYCLES_PER_TICK.getAsInt()) return;
        cycled++;
        notifiedFromInput();
    }

    @Override
    public void tick() {
        cycled = 0;
        super.tick();
    }

    public void notifiedFromInput() {

    }

    /**
     * Resets the behavior's internal state.
     * Override this to clear custom maps, buffers, or cached values when the gauge is removed or reset.
     */
    public void reset() {}

    /**
     * Decides how the light bulb should be rendered.
     * */
    public BulbState getBulbState() {
        return BulbState.DISABLED;
    }

    /**
     * @return the registered {@link PanelType} for this behavior
     */
    public PanelType<?> getPanelType() {
        return type;
    }

    /**
     * Returns the name used for display in the UI and when hovering over the panel item.
     *
     * @return a {@link Component} representing the panel's name
     */
    @Override
    public @NotNull Component getDisplayName() {
        return getItem().getDefaultInstance().getHoverName();
    }

    /**
     * Generates a text component for display links (e.g., when viewed through a
     * Display Link from Create).
     *
     * @param shortVersion true if the display should use a condensed text format
     * @return a {@link MutableComponent} containing the formatted data for the link
     */
    public MutableComponent getDisplayLinkComponent(boolean shortVersion) {
        return Component.empty();
    }

    /**
     * Defines the items required to build or maintain this panel behavior.
     *
     * @return an {@link ItemRequirement} containing the necessary items
     */
    @Override
    public ItemRequirement getRequiredItems() {
        return isActive() ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, getItem())
                : ItemRequirement.NONE;
    }
    //endregion
    //region Interaction
    public void addInteractions(PanelInteractionBuilder builder) {

    }

    public boolean isInInteraction() {
        return interactions.values().stream().anyMatch(InteractionData::getValue);
    }

    public boolean hasInteraction(String key) {
        if(!interactions.containsKey(key)) return false;
        return interactions.get(key).getValue();
    }

    public boolean hasInteractionUncached(String key, Level level, BlockPos pos, BlockState state) {
        if(!interactions.containsKey(key)) return false;
        return interactions.get(key).predicate.test(level, pos, state);
    }

    public AttachedBlock getInteraction() {
        FactoryPanelBlockEntity panelBE = panelBE();
        BlockState state = panelBE.getBlockState();
        BlockPos targetPos = panelBE.getBlockPos().relative(FactoryPanelBlock.connectedDirection(state)
                .getOpposite());
        assert panelBE.getLevel() != null;
        var level = panelBE.getLevel();
        var state1 = panelBE.getLevel().getBlockState(targetPos);
        return new AttachedBlock(level, targetPos, state1);
    }

    @Nullable
    public AttachedBlock getInteraction(String key) {
        var attached = getInteraction();
        if(!hasInteractionUncached(key, attached.level, attached.pos, attached.state)) return null;
        return attached;
    }

    @Nullable
    public BlockEntity getInteractionBlockEntity(String key) {
        var data = getInteraction(key);
        if(data == null) return null;
        return data.level.getBlockEntity(data.pos);
    }
    //endregion
    //region Other
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

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return null;
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


    @Override
    public void destroy() {
        super.destroy();
        if(blockEntity instanceof FactoryPanelBlockEntity be) {
            var newBehaviour = new FactoryPanelBehaviour(be, this.slot);
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
        if (!active)
            return;

        CompoundTag panelTag = new CompoundTag();
        panelTag.putBoolean("Satisfied", satisfied);
        panelTag.putBoolean("PromisedSatisfied", promisedSatisfied);
        panelTag.putBoolean("RedstonePowered", redstonePowered);
        panelTag.put("Targeting", CatnipCodecUtils.encode(CatnipCodecs.set(FactoryPanelPosition.CODEC), targeting).orElseThrow());
        panelTag.put("TargetedBy", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(targetedBy.values())).orElseThrow());
        panelTag.put("TargetedByLinks", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(targetedByLinks.values())).orElseThrow());
        panelTag.put("TargetedByExtra", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(getTargetedByExtra().values())).orElseThrow());
        easyWrite(panelTag, registries, clientPacket);
        nbt.put(CreateLang.asId(slot.name()), panelTag);
    }

    public final Map<BlockPos, FactoryPanelConnection> getTargetedByExtra() {
        return ((FPBExtension)this).deployer$getExtra();
    }

    @Override
    public boolean canShortInteract(ItemStack toApply) {
        return false;
    }

    @Override
    public ItemStack getFilter() {
        return ItemStack.EMPTY;
    }

    public int getDefaultConnectionAmount() {
        return 1;
    }

    @Override
    public void setNetwork(UUID network) {
    }

    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult, boolean client) {
        if (client)
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> displayScreen(player));
    }

    @Override
    public boolean isCountVisible() {
        return true;
    }

    @Override
    public final void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        boolean isClientSide = player.level().isClientSide;
        // Wrench cycles through arrow bending
        if (targeting.size() + targetedByLinks.size() > 0 && player.getItemInHand(hand).is(Tags.Items.TOOLS_WRENCH)) {
            int sharedMode = -1;
            boolean notifySelf = false;

            for (FactoryPanelPosition target : targeting) {
                FactoryPanelBehaviour at = at(getWorld(), target);
                if (at == null)
                    continue;
                FactoryPanelConnection connection = at.targetedBy.get(getPanelPosition());
                if (connection == null)
                    continue;
                if (sharedMode == -1)
                    sharedMode = (connection.arrowBendMode + 1) % 4;
                connection.arrowBendMode = sharedMode;
                if (!isClientSide)
                    at.blockEntity.notifyUpdate();
            }

            for (FactoryPanelConnection connection : targetedByLinks.values()) {
                if (sharedMode == -1)
                    sharedMode = (connection.arrowBendMode + 1) % 4;
                connection.arrowBendMode = sharedMode;
                if (!isClientSide)
                    notifySelf = true;
            }

            if (sharedMode == -1)
                return;

            char[] boxes = "□□□□".toCharArray();
            boxes[sharedMode] = '■';
            player.displayClientMessage(CreateLang.translate("factory_panel.cycled_arrow_path", new String(boxes))
                    .component(), true);
            if (notifySelf)
                blockEntity.notifyUpdate();

            return;
        }

        // Client might be in the process of connecting a panel
        if (isClientSide)
            if (FactoryPanelConnectionHandler.panelClicked(getWorld(), player, this))
                return;
        onShortInteract(player, hand, side, hitResult, isClientSide);
    }

    public void onConnectionAdded(FactoryPanelConnection connection) {

    }

    //endregion

    public record AttachedBlock(Level level, BlockPos pos, BlockState state) {}

    private static class InteractionData {
        private Boolean cached;
        private final TriPredicate<Level, BlockPos, BlockState> predicate;
        private final AbstractPanelBehaviour parent;

        private InteractionData(TriPredicate<Level, BlockPos, BlockState> predicate, AbstractPanelBehaviour parent) {
            this.predicate = predicate;
            this.parent = parent;
        }
        private boolean getValue() {
            if(cached == null) {
                cached = getUncached();
            }
            return cached;
        }

        private boolean getUncached() {
            AttachedBlock attached = parent.getInteraction();
            return predicate.test(attached.level, attached.pos, attached.state);
        }
    }

    public enum BulbState {
        DISABLED, RED, GREEN
    }
}
