package net.liukrast.deployer.lib.logistics.board;


import java.lang.Boolean;
import java.lang.Integer;
import com.mojang.serialization.Codec;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.codecs.CatnipCodecs;
import net.createmod.catnip.gui.ScreenOpener;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.mixin.FactoryPanelBehaviourAccessor;
import net.liukrast.deployer.lib.mixin.FactoryPanelBehaviourIMixin;
import net.liukrast.deployer.lib.mixin.FilteringBehaviourMixin;
import net.liukrast.deployer.lib.mixinExtension.IFPExtension;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines a custom panel. Register it through {@link PanelType}
 * */
public abstract class AbstractPanelBehaviour extends FactoryPanelBehaviour {
    /* JAVADOC CODE */
    private final PanelType<?> type;
    private final Reference2ObjectArrayMap<PanelConnection<?>, Supplier<?>> connections = new Reference2ObjectArrayMap<>();

    /**
     * Common color used to warn the user that the connection is currently waiting for the next tick to update
     * */
    protected static final Integer WAITING = 0xffd541;
    /**
     * Common color used to warn the user that the connection doesn't do anything
     * */
    protected static final Integer DISABLED = 0x888898;

    /**
     * This constructor allows to modify the valueBoxTransform to make a custom input system
     * */
    public AbstractPanelBehaviour(ValueBoxTransform valueBoxTransform, PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        this(type, be, slot);
        ((FilteringBehaviourMixin)this).setValueBoxTransform(valueBoxTransform);
    }

    public AbstractPanelBehaviour(PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(be, slot);
        var builder = new PanelConnectionBuilder();
        addConnections(builder);
        connections.putAll(builder.map);
        this.type = type;
    }

    /**
     * Adds a new connection provider to this gauge.
     * This means other gauges can read information from this gauge.
     * Also keep in mind that the order these connections is added is important for some panels which read multiple connections.
     * E.g. a panel which accepts both string and redstone, will decide which to read it based on the priority you gave here
     * */
    public abstract void addConnections(PanelConnectionBuilder builder);

    /**
     * @return the connections set, sorted
     * */
    public Set<PanelConnection<?>> getConnections() {
        return connections.keySet();
    }

    /**
     * @return Whether the panel has a precise connection, using forge's deferred holder
     * */
    public <T> Boolean hasConnection(DeferredHolder<PanelConnection<?>, PanelConnection<T>> connection) {
        return hasConnection(connection.get());
    }

    /**
     * @return Whether this behavior has a precise connection
     * */
    public Boolean hasConnection(PanelConnection<?> connection) {
        return connections.containsKey(connection);
    }

    /**
     * @param shortenNumbers whether the display is in mode "shortened" or "full_number"
     * @return The component for display links
     * */
    public MutableComponent getDisplayLinkComponent(Boolean shortenNumbers) {
        return Component.empty();
    }

    /**
     * Whether the panel should skip calling {@link FactoryPanelBehaviour#tick()}
     * */
    public Boolean skipOriginalTick() {
        return true;
    }

    /**
     * @return Whether the panel should render its bulb
     * */
    public Boolean shouldRenderBulb(Boolean original) {
        return false;
    }

    /**
     * Since original class extends {@link com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour},
     * return true if you want this gauge to have the render from filtering behavior.
     * */
    public Boolean withFilteringBehaviour() {
        return false;
    }

    /**
     * @return The item associated with this behavior. Used for drops and more.
     * */
    public abstract Item getItem();

    /**
     * @return the model for your custom gauge. Will automatically be used for rendering.
     * */
    public abstract PartialModel getModel(FactoryPanelBlock.PanelState panelState, FactoryPanelBlock.PanelType panelType);

    /**
     * An easier extension of {@link AbstractPanelBehaviour#write(CompoundTag, HolderLookup.Provider, Boolean)}.
     * @param nbt The compound tag of the single gauge slot. Save your data into this
     * */
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, Boolean clientPacket) {}

    /**
     * An easier extension of {@link AbstractPanelBehaviour#read(CompoundTag, HolderLookup.Provider, Boolean)}.
     * @param nbt The compound tag of the single gauge slot. Read your data from this slot
     * */
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, Boolean clientPacket) {}

    /**
     * Opens the editor screen for this panel
     * */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void displayScreen(Player player) {
        if (player instanceof LocalPlayer)
            ScreenOpener.open(new BasicPanelScreen<>(this));
    }

    /**
     * This function is called when trying to connect two gauges together.
     * The default declaration you see below ignores the {@code no_item} issue,
     * since most of the custom gauges do not actually need a custom item inside to connect.
     * @return whether it should ignore or not the issue inserted.
     * */
    public Boolean ignoreIssue(@Nullable String issue) {
        return "factory_panel.no_item".equals(issue);
    }

    /**
     * Generates color per connections with other gauges.
     * @param original the original color a normal gauge would return (just in case you needed)
     * */
    public Integer calculatePath(FactoryPanelBehaviour other, Integer original) {
        return DISABLED;
    }

    /**
     * Generates color per connections with extra panel elements
     * */
    public Integer calculateExtraPath(BlockPos pos) {
        return DISABLED;
    }

    /**
     * @return Obtains the value another panel is providing for this special panel connection
     * */
    public <T> Optional<T> getConnectionValue(DeferredHolder<PanelConnection<?>, PanelConnection<T>> connection) {
        return getConnectionValue(connection.get());
    }

    /**
     * @return Obtains the value another panel is providing for this special panel connection
     * */
    public <T> Optional<T> getConnectionValue(PanelConnection<T> connection) {
        if(!connections.containsKey(connection)) return Optional.empty();
        // We can safely cast here.
        //noinspection unchecked
        return Optional.ofNullable((T) connections.get(connection).get());
    }

    /**
     * Executes a consumer for each link connected to this panel
     * */
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
     * Executes a consumer for each gauge connected.
     * @param panelConnection defines what connection value we are trying to gather from our panels
     * @param toConsider All the other connections to consider. If your panel extracts another connection value somewhere else, you should put that here. This way, if the panel has a priority on that other connection, it will not be added to the consumer list
     * */
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
     * Executes a consumer for each extra panel element connected
     * */
    public <T> void consumeForExtra(PanelConnection<T> panelConnection, BiConsumer<BlockPos, T> consumer) {
        Set<BlockPos> toRemove = new HashSet<>();
        for(var connection : ((IFPExtension)this).deployer$getExtra().values()) {
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
        toRemove.forEach(pos -> ((IFPExtension)this).deployer$getExtra().remove(pos));
        if(!toRemove.isEmpty()) blockEntity.notifyUpdate();
    }

    /* UTIL METHODS IMPLEMENTATION */
    public Integer getTimer() {
        return ((FactoryPanelBehaviourAccessor)this).timer();
    }

    public Integer getLastReportedLevelInStorage() {
        return ((FactoryPanelBehaviourAccessor)this).lastReportedLevelInStorage();
    }

    public Integer getLastReportedUnloadedLinks() {
        return ((FactoryPanelBehaviourAccessor)this).lastReportedUnloadedLinks();
    }

    public Integer getLastReportedPromises() {
        return ((FactoryPanelBehaviourAccessor)this).lastReportedPromises();
    }

    /* API IMPLEMENTATION */
    public PanelType<?> getPanelType() {
        return type;
    }

    @Override
    public AbstractContainerMenu createMenu(Integer containerId, Inventory playerInventory, Player player) {
        return null;
    }

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

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, Boolean clientPacket) {
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

    @Override
    public void writeSafe(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeSafe(nbt, registries);
        CompoundTag special = nbt.contains("CustomPanels") ? nbt.getCompound("CustomPanels") : new CompoundTag();
        special.putString(CreateLang.asId(slot.name()), Objects.requireNonNull(DeployerRegistries.PANEL.getKey(type)).toString());
        nbt.put("CustomPanels", special);
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, Boolean clientPacket) {
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
        IFPExtension extra = ((IFPExtension)this);
        panelTag.put("TargetedByExtra", CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), new ArrayList<>(extra.deployer$getExtra().values())).orElseThrow());

        if(withFilteringBehaviour()) {
            panelTag.put("Filter", getFilter().saveOptional(registries));
            panelTag.putInt("FilterAmount", count);
            panelTag.putBoolean("UpTo", upTo);
        }

        easyWrite(panelTag, registries, clientPacket);

        nbt.put(CreateLang.asId(slot.name()), panelTag);
    }

    @Override
    public Boolean canShortInteract(ItemStack toApply) {
        return withFilteringBehaviour() && super.canShortInteract(toApply);
    }

    @Override
    public ItemStack getFilter() {
        return getConnectionValue(DeployerPanelConnections.ITEMSTACK).orElse(ItemStack.EMPTY);
    }

    // We invoke the private function through mixin. Create, why are you making this method private...
    public void notifyRedstoneOutputs() {
        for(FactoryPanelPosition panelPos : targeting) {
            if(!getWorld().isLoaded(panelPos.pos()))
                return;
            FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
            if(behaviour == null) continue;
            behaviour.checkForRedstoneInput();
        }
        ((FactoryPanelBehaviourIMixin)this).invokeNotifyRedstoneOutputs();
    }

    @Override
    public Boolean acceptsValueSettings() {
        return true;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getItem().getDefaultInstance().getHoverName();
    }

    @Override
    public ItemRequirement getRequiredItems() {
        return isActive() ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, getItem())
                : ItemRequirement.NONE;
    }

    public static class PanelConnectionBuilder {
        private final Map<PanelConnection<?>, Supplier<?>> map = new Reference2ObjectArrayMap<>();

        private PanelConnectionBuilder() {}

        public <T> PanelConnectionBuilder put(@NotNull DeferredHolder<PanelConnection<?>, PanelConnection<T>> panelConnection, @NotNull Supplier<T> getter) {
            return put(panelConnection.get(), getter);
        }

        public <T> PanelConnectionBuilder put(@NotNull PanelConnection<T> panelConnection, @NotNull Supplier<T> getter) {
            map.put(panelConnection, getter);
            return this;
        }
    }
}
