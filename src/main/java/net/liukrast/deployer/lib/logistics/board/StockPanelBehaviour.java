package net.liukrast.deployer.lib.logistics.board;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.logistics.packagerLink.*;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.logistics.LogisticallyLinked;
import net.liukrast.deployer.lib.logistics.board.connection.ConnectionLine;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnectionBuilder;
import net.liukrast.deployer.lib.logistics.board.connection.PanelInteractionBuilder;
import net.liukrast.deployer.lib.logistics.board.connection.ProvidesConnection;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager;
import net.liukrast.deployer.lib.mixin.accessors.FactoryPanelBehaviourAccessor;
import net.liukrast.deployer.lib.mixinExtensions.RPQExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class StockPanelBehaviour<K, V> extends AbstractPanelBehaviour {
    //region Attributes & Constructor
    // Stock
    private final StockInventoryType<K, V, ?> stockInventoryType;
    private V filter;
    private int scale;

    // Factory gauge inherits
    private boolean promisePrimedForMarkDirty;
    private int lastReportedUnloadedLinks;
    private int lastReportedLevelInStorage;
    private int lastReportedPromises;
    private int timer;


    //endregion
    //region Stock
    public StockPanelBehaviour(StockInventoryType<K, V, ?> stockInventoryType, PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
        this.stockInventoryType = stockInventoryType;
        this.filter = stockInventoryType.valueHandler().empty();
        this.promisePrimedForMarkDirty = true;
    }

    public StockInventoryType<K, V, ?> getStockInventoryType() {
        return stockInventoryType;
    }
    public abstract Multiplier[] getMultiplierMode();

    //endregion
    //region AbstractPanel deps
    @Override
    public void addConnections(PanelConnectionBuilder builder) {
        builder.registerBoth(DeployerPanelConnections.STOCK_CONNECTION.get(), () -> provider -> provider.add(stockInventoryType, filter));
        builder.registerBoth(DeployerPanelConnections.REDSTONE, () -> this.satisfied && count != 0 ? 15 : 0);
        builder.registerBoth(DeployerPanelConnections.INTEGER, this::getLevelInStorage);
        builder.registerBoth(DeployerPanelConnections.STRING, () -> getDisplayLinkComponent(false).getString());
    }

    @Override
    public void addInteractions(PanelInteractionBuilder builder) {
        builder.registerEntity("restocker", be -> be instanceof AbstractPackagerBlockEntity<?,?,?> a && a.getStockType() == stockInventoryType);
    }

    @Override
    public @NotNull ConnectionLine overrideConnectionColor(ConnectionLine original, FactoryPanelConnection connection, float partialTicks) {
        var pc = ProvidesConnection.getCurrentConnection(connection, () -> null);
        if(pc != DeployerPanelConnections.STOCK_CONNECTION.get()) return original;
        int color = getIngredientStatusColor();
        float glow = bulb.getValue(partialTicks);
        if(redstonePowered && !waitingForNetwork && glow > 0 && !satisfied) {
            float p = (1 - (1 - glow) * (1 - glow));
            color = Color.mixColors(color, connection.success ? 0xEAF2EC : 0xE5654B, p);
        }
        return new ConnectionLine(color, false, !isMissingAddress() && !waitingForNetwork && !satisfied && !redstonePowered);
    }

    private void tickOutline() {
        CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> LogisticallyLinkedClientHandler.tickPanel(this));
    }
    //endregion

    @Override
    public void setNetwork(UUID network) {
        this.network = network;
    }

    //moveTo
    //moveToSlot
    //initialize

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClientSide()) {
            if (blockEntity.isVirtual())
                tickStorageMonitor();
            bulb.updateChaseTarget(redstonePowered || satisfied ? 1 : 0);
            bulb.tickChaser();
            if (active)
                tickOutline();
            return;
        }

        if (!promisePrimedForMarkDirty) {
            restockerPromises.setOnChanged(blockEntity::setChanged);
            promisePrimedForMarkDirty = true;
        }

        tickStorageMonitor();
        tickRequests();
    }

    @Override
    public void checkForRedstoneInput() {
        super.checkForRedstoneInput();
        timer = 1;
    }

    //notifyRedstoneOutputs

    private void tickStorageMonitor() {
        int unloadedLinkCount = getUnloadedLinks();
        if (!hasInteraction("restocker") && unloadedLinkCount == 0 && lastReportedLevelInStorage != 0) {
            // All links have been loaded, invalidate the cache so we can get an accurate summary!
            // Otherwise, we will have to wait for 20 ticks and unnecessary packages will be sent!
            LogisticsManager.SUMMARIES.invalidate(network);
        }
        int inStorage = getLevelInStorage();
        int promised = getPromised();
        int demand = getAmount() * getMultiplierMode()[scale].value;
        var val = stockInventoryType.valueHandler();
        boolean shouldSatisfy = val.isEmpty(filter) || inStorage >= demand;
        boolean shouldPromiseSatisfy = val.isEmpty(filter) || inStorage + promised >= demand;
        boolean shouldWait = unloadedLinkCount > 0;

        if (lastReportedLevelInStorage == inStorage && lastReportedPromises == promised
                && lastReportedUnloadedLinks == unloadedLinkCount && satisfied == shouldSatisfy
                && promisedSatisfied == shouldPromiseSatisfy && waitingForNetwork == shouldWait)
            return;

        if (!satisfied && shouldSatisfy && demand > 0) {
            AllSoundEvents.CONFIRM.playOnServer(getWorld(), getPos(), 0.075f, 1f);
            AllSoundEvents.CONFIRM_2.playOnServer(getWorld(), getPos(), 0.125f, 0.575f);
        }

        boolean notifyOutputs = satisfied != shouldSatisfy;
        lastReportedLevelInStorage = inStorage;
        satisfied = shouldSatisfy;
        lastReportedPromises = promised;
        promisedSatisfied = shouldPromiseSatisfy;
        lastReportedUnloadedLinks = unloadedLinkCount;
        waitingForNetwork = shouldWait;
        if (!getWorld().isClientSide)
            blockEntity.sendData();
        if (notifyOutputs)
            notifyOutputs();
    }

    private void tickRequests() {
        ((FactoryPanelBehaviourAccessor)this).deployer$tickRequests();
    }

    // TODO On short interaction



    @Override
    public void reset() {
        this.filter = stockInventoryType.valueHandler().empty();
    }

    @Override
    public BulbState getBulbState() {
        return getAmount() > 0 ? (redstonePowered || isMissingAddress() ? BulbState.RED : BulbState.GREEN) : BulbState.DISABLED;
    }

    @Override
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        nbt.putString("RecipeAddress", recipeAddress);
        nbt.putInt("RecipeOutput", recipeOutput);
        nbt.putInt("PromiseClearingInterval", promiseClearingInterval);
        nbt.putUUID("Freq", network);
        nbt.put("Craft", NBTHelper.writeItemList(activeCraftingArrangement, registries));
        nbt.putInt("Timer", timer);
        nbt.putInt("LastLevel", lastReportedLevelInStorage);
        nbt.putInt("LastPromised", lastReportedPromises);
        nbt.putInt("LastUnloadedLinks", lastReportedUnloadedLinks);
        stockInventoryType.valueHandler().codec().encodeStart(NbtOps.INSTANCE, filter)
                .result()
                .ifPresent(tag -> nbt.put("Fluid", tag));
        nbt.putInt("Count", count);
        nbt.putInt("Scale", scale);
        nbt.putInt("FilterAmount", count);
        nbt.putBoolean("UpTo", upTo);
        lastReportedLevelInStorage = nbt.getInt("LastLevel");
        lastReportedPromises = nbt.getInt("LastPromised");
        lastReportedUnloadedLinks = nbt.getInt("LastUnloadedLinks");
    }

    @Override
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        if (nbt.contains("Fluid")) {
            stockInventoryType.valueHandler().codec().parse(NbtOps.INSTANCE, nbt.get("Fluid"))
                    .result()
                    .ifPresent(stack -> this.filter = stack);
        } else {
            this.filter = stockInventoryType.valueHandler().empty();
        }
        count = nbt.getInt("Count");
        scale = nbt.getInt("Scale");
        timer = nbt.getInt("Timer");
        lastReportedLevelInStorage = nbt.getInt("LastLevel");
        lastReportedPromises = nbt.getInt("LastPromised");
        lastReportedUnloadedLinks = nbt.getInt("LastUnloadedLinks");
    }

    @Override
    public String canConnect(FactoryPanelBehaviour from) {
        if(stockInventoryType.valueHandler().isEmpty(filter)) return "factory_panel.no_item";
        if(hasInteraction("restocker")) return "factory_panel.input_in_restock_mode";
        return super.canConnect(from);
    }



    @Override
    public int getLevelInStorage() {
        if (blockEntity.isVirtual())
            return 1;
        if (getWorld().isClientSide())
            return lastReportedLevelInStorage;
        if (stockInventoryType.valueHandler().isEmpty(filter))
            return 0;

        AbstractInventorySummary<K, V> summary = getRelevantSummary();
        return summary.getCountOf(filter);
    }

    private AbstractInventorySummary<K, V> getRelevantSummary() {
        if(!hasInteraction("restocker"))
            return LogisticsGenericManager.getSummaryOfNetwork(stockInventoryType, network, false);
        BlockEntity attached = getInteractionBlockEntity("restocker");
        if (attached == null)
            return stockInventoryType.networkHandler().empty();
        if(attached instanceof AbstractPackagerBlockEntity<?,?,?> apbe && apbe.getStockType() == stockInventoryType) {
            //noinspection unchecked
            return ((AbstractPackagerBlockEntity<K, V, Object>) apbe).getAvailableStacks();
        }
        return stockInventoryType.networkHandler().empty();
    }

    private int getPromiseExpiryTimeInTicks() {
        if (promiseClearingInterval == -1)
            return -1;
        if (promiseClearingInterval == 0)
            return 20 * 30;

        return promiseClearingInterval * 20 * 60;
    }

    @Override
    public int getPromised() {
        if (getWorld().isClientSide())
            return lastReportedPromises;
        if (stockInventoryType.valueHandler().isEmpty(filter))
            return 0;
        var restockerPromises = (RPQExtension)(this.restockerPromises);

        if (hasInteraction("restocker")) {
            if (forceClearPromises) {
                restockerPromises.deployer$forceClear(stockInventoryType, filter);
                resetTimerSlightly();
            }
            forceClearPromises = false;
            return restockerPromises.deployer$getTotalPromisedAndRemoveExpired(stockInventoryType, filter, getPromiseExpiryTimeInTicks());
        }

        var promises = (RPQExtension)(Create.LOGISTICS.getQueuedPromises(network));
        if (promises == null)
            return 0;

        if (forceClearPromises) {
            promises.deployer$forceClear(stockInventoryType, filter);
            resetTimerSlightly();
        }
        forceClearPromises = false;

        return promises.deployer$getTotalPromisedAndRemoveExpired(stockInventoryType, filter, getPromiseExpiryTimeInTicks());
    }

    @OnlyIn(Dist.CLIENT)
    public void displayScreen(Player player) {
        if (player instanceof LocalPlayer) {
            ScreenOpener.open(new FactoryPanelScreen(this));
        }
    }

    public abstract V parseFromHeldItem(ItemStack heldItem);

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        if (!Create.LOGISTICS.mayInteract(network, player)) {
            player.displayClientMessage(CreateLang.translate("logistically_linked.protected")
                    .style(ChatFormatting.RED)
                    .component(), true);
            return;
        }
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

        ItemStack heldItem = player.getItemInHand(hand);
        if (stockInventoryType.valueHandler().isEmpty(filter)) {
            // Open screen for setting an item through JEI
            if (heldItem.isEmpty()) {
                if (!isClientSide && player instanceof ServerPlayer sp)
                    sp.openMenu(this, buf -> FactoryPanelPosition.STREAM_CODEC.encode(buf, getPanelPosition()));
                return;
            }

            // Use regular filter interaction for setting the item
            V stack = parseFromHeldItem(heldItem);
            if(!stockInventoryType.valueHandler().isEmpty(stack)) {
                filter = stockInventoryType.valueHandler().copy(stack);
            }
            return;
        }

        // Bind logistics items to this panels' frequency
        if (heldItem.getItem() instanceof LogisticallyLinkedBlockItem || heldItem.getItem() instanceof LogisticallyLinked) {
            if (!isClientSide)
                LogisticallyLinkedBlockItem.assignFrequency(heldItem, player, network);
            return;
        }

        // Open configuration screen
        if (isClientSide)
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> displayScreen(player));
    }

    public V getStack() {
        return filter;
    }

    public abstract Component getHoverName();

    @Override
    public MutableComponent getLabel() {
        String key;

        if (!targetedBy.isEmpty() && count == 0)
            return CreateLang.translate("gui.factory_panel.no_target_amount_set")
                    .style(ChatFormatting.RED)
                    .component();

        if (isMissingAddress())
            return CreateLang.translate("gui.factory_panel.address_missing")
                    .style(ChatFormatting.RED)
                    .component();

        if (getFilter().isEmpty())
            key = "factory_panel.new_factory_task";
        else if (waitingForNetwork)
            key = "factory_panel.some_links_unloaded";
        else if (getAmount() == 0 || targetedBy.isEmpty())
            return getHoverName().plainCopy();
        else {
            key = getFilter().getHoverName()
                    .getString();
            if (redstonePowered)
                key += " " + CreateLang.translate("factory_panel.redstone_paused")
                        .string();
            else if (!satisfied)
                key += " " + CreateLang.translate("factory_panel.in_progress")
                        .string();
            return CreateLang.text(key)
                    .component();
        }

        return CreateLang.translate(key)
                .component();
    }



    @Override
    public MutableComponent getCountLabelForValueBox() {
        if (stockInventoryType.valueHandler().isEmpty(filter))
            return Component.empty();
        if (waitingForNetwork) {
            return Component.literal("?");
        }

        int levelInStorage = getLevelInStorage();
        boolean inf = levelInStorage >= BigItemStack.INF;
        int inStorage = levelInStorage / getMultiplierMode()[scale].value;
        int promised = getPromised();
        String stacks = getMultiplierMode()[scale].key;

        if (count == 0) {
            return CreateLang.text(inf ? "  ∞" : inStorage + stacks)
                    .color(0xF1EFE8)
                    .component();
        }

        return CreateLang.text(inf ? "  ∞" : "   " + inStorage + stacks)
                .color(satisfied ? 0xD7FFA8 : promisedSatisfied ? 0xffcd75 : 0xFFBFA8)
                .add(CreateLang.text(promised == 0 ? "" : "⏶"))
                .add(CreateLang.text("/")
                        .style(ChatFormatting.WHITE))
                .add(CreateLang.text(count + stacks + "  ")
                        .color(0xF1EFE8))
                .component();
    }

    @Override
    public boolean isCountVisible() {
        return !stockInventoryType.valueHandler().isEmpty(filter);
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        if(getValueSettings().equals(settings))
            return;
        var modes = getMultiplierMode();
        scale = Mth.clamp(settings.row(), 0, modes.length-1);
        count = Math.max(0, settings.value() * modes[scale].step);
        blockEntity.setChanged();
        blockEntity.sendData();
        playFeedbackSound(this);
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(scale, count);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        int maxAmount = 100;
        return new ValueSettingsBoard(CreateLang.translate("factory_panel.target_amount")
                .component(), maxAmount, 10,
                Arrays.stream(getMultiplierMode()).map(mul -> (Component)Component.literal(mul.key)).toList(),
                new ValueSettingsFormatter(this::formatValue));
    }

    @Override
    public MutableComponent formatValue(ValueSettings value) {
        if (value.value() == 0) {
            return CreateLang.translateDirect("gui.factory_panel.inactive");
        } else {
            var mode = getMultiplierMode()[value.row()];
            return Component.literal(Math.max(0, value.value() * mode.step) + mode.key);
        }
    }

    public abstract ValueBox createBox(Component label, AABB bb, BlockPos pos);

    public abstract void render(float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay);

    public static class Multiplier {
        private final String key;
        private final int value;
        private final int step;

        public Multiplier(String key, int value, int step) {
            this.key = key;
            this.value = value;
            this.step = step;
        }

        public Multiplier(String key, int value) {
            this(key, value, 1);
        }
    }
}
