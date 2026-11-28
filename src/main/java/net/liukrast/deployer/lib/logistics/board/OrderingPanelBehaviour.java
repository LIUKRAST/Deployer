package net.liukrast.deployer.lib.logistics.board;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.*;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.logistics.LogisticallyLinked;
import net.liukrast.deployer.lib.logistics.board.connection.*;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Math;

import java.util.*;

public abstract class OrderingPanelBehaviour extends AbstractPanelBehaviour implements RenderFilterSlot {
    private boolean promisePrimedForMarkDirty;
    private int lastReportedUnloadedLinks;
    private int lastReportedLevelInStorage;
    private int lastReportedPromises;
    private int timer;

    public OrderingPanelBehaviour(PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
        this.promisePrimedForMarkDirty = true;
    }

    public abstract boolean isFilterEmpty();

    @Override
    public void addConnections(PanelConnectionBuilder builder) {
        if(redstoneInput())
            builder.registerBoth(DeployerPanelConnections.REDSTONE, () -> this.satisfied && hasAmount());
        if(intInput())
            builder.registerBoth(DeployerPanelConnections.NUMBERS, () -> (float)this.getLevelInStorage());
        if(stringInput())
            builder.registerBoth(DeployerPanelConnections.STRING, () -> getDisplayLinkComponent(false).getString());
    }

    /**
     * @return Whether this gauge sends its satisfied state as redstone to other gauges
     */
    public boolean redstoneInput() {
        return true;
    }

    /**
     * @return Whether this gauge sends its stock level as a number to other gauges
     */
    public boolean intInput() {
        return true;
    }

    /**
     * @return Whether this gauge sends its display text to other gauges
     */
    public boolean stringInput() {
        return true;
    }

    @Override
    public boolean isMissingAddress() {
        return (!targetedBy.isEmpty() || hasInteraction("restocker")) && hasAmount() && recipeAddress.isBlank();
    }

    @Override
    public int overrideConnectionColor(int original, FactoryPanelConnection connection, float partialTicks) {
        var pc = ProvidesConnection.getCurrentConnection(connection, () -> null);
        if(pc != DeployerPanelConnections.STOCK_CONNECTION.get()) return original;
        int color = getIngredientStatusColor();
        float glow = bulb.getValue(partialTicks);
        if(!redstonePowered && !waitingForNetwork && glow > 0 && !satisfied) {
            float p = (1 - (1 - glow) * (1 - glow));
            color = Color.mixColors(color, connection.success ? 0xEAF2EC : 0xE5654B, p);
        }
        return ConnectionLine.pack(color, false, !isMissingAddress() && !waitingForNetwork && !satisfied && !redstonePowered);
    }

    @Override
    public void setNetwork(UUID network) {
        this.network = network;
    }

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
    public void notifiedFromInput() {
        if (!active)
            return;

        boolean anyChange = false;

        if(redstoneInput()) {

            boolean shouldPower;
            var li = getAllValues(DeployerPanelConnections.REDSTONE.get());
            if (li == null) shouldPower = false;
            else shouldPower = li.stream().anyMatch(k -> k);

            for (FactoryPanelConnection connection : targetedByLinks.values()) {
                if (!getWorld().isLoaded(connection.from.pos()))
                    return;
                FactoryPanelSupportBehaviour linkAt = linkAt(getWorld(), connection);
                if (linkAt == null)
                    return;
                shouldPower |= linkAt.shouldPanelBePowered();
            }

            if (shouldPower != redstonePowered) {
                anyChange = true;
                redstonePowered = shouldPower;
            }
        }
        if(intInput()) {
            var nums = getAllValues(DeployerPanelConnections.NUMBERS.get());
            if(nums != null) {
                Integer total = nums.isEmpty() ? null : (int)(float)nums.stream().reduce(0f, Float::sum);

                if(total != null && !total.equals(count)) {
                    anyChange = true;
                    count = total;
                }
            }
        }
        if(stringInput()) {
            var strs = getAllValues(DeployerPanelConnections.STRING.get());
            if(strs != null) {
                String fAddress = strs.isEmpty() ? null : String.join("", strs);
                if(fAddress != null && !fAddress.equals(recipeAddress)) {
                    anyChange = true;
                    recipeAddress = fAddress;
                }
            }
        }

        if(!anyChange) return;

        blockEntity.notifyUpdate();
        timer = 1;
    }

    private void tickStorageMonitor() {
        int unloadedLinkCount = getUnloadedLinks();
        if (!hasInteraction("restocker") && unloadedLinkCount == 0 && lastReportedUnloadedLinks != 0)
            invalidateNetworkSummary();
        int inStorage = getLevelInStorage();
        int promised = getPromised();
        int demand = getDemand();
        boolean shouldSatisfy = isFilterEmpty() || inStorage >= demand;
        boolean shouldPromiseSatisfy = isFilterEmpty() || inStorage + promised >= demand;
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
            notifyOutputs(true);
    }

    protected void invalidateNetworkSummary() {
        LogisticsManager.SUMMARIES.invalidate(network);
    }

    /**
     * @return The amount this gauge wants in storage, in base units
     */
    public int getDemand() {
        return getAmount() * getDemandMultiplier();
    }

    /**
     * @return How many base units one unit of the amount set by the player is worth
     */
    public int getDemandMultiplier() {
        return 1;
    }

    private int getConfigRequestIntervalInTicks() {
        return AllConfigs.server().logistics.factoryGaugeTimer.get();
    }

    private void tickRequests() {
        boolean restocker = hasInteraction("restocker");
        if (targetedBy.isEmpty() && !restocker)
            return;
        if (restocker)
            restockerPromises.tick();
        if (satisfied || promisedSatisfied || waitingForNetwork || redstonePowered)
            return;
        if (timer > 0) {
            timer = Math.min(timer, getConfigRequestIntervalInTicks());
            timer--;
            return;
        }

        resetTimer();

        if (recipeAddress.isBlank())
            return;

        if (restocker) {
            tryRestock();
            return;
        }

        var targets = PanelRequestHelper.collectStockTargets(this);
        if (targets == null || targets.isEmpty())
            return;

        Map<UUID, Map<ItemStack, ItemStackConnections>> consolidated = new HashMap<>(targets.items());
        for (FactoryPanelConnection connection : targets.vanillaTargets().values()) {
            FactoryPanelBehaviour source = at(getWorld(), connection);
            if (source == null)
                return;

            ItemStack item = source.getFilter();
            Map<ItemStack, ItemStackConnections> networkItemCounts = consolidated.computeIfAbsent(source.network, $ -> new Object2ObjectOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG));
            ItemStackConnections existingConnections = networkItemCounts.computeIfAbsent(item, $ -> new ItemStackConnections(item));
            existingConnections.add(connection);
            existingConnections.totalAmount += connection.amount;
        }

        MutableBoolean failed = new MutableBoolean(false);
        var genericToRequest = PanelRequestHelper.collectAvailableGenericStock(targets.generic(), this::sendEffect, failed::setTrue);

        Multimap<UUID, BigItemStack> toRequest = HashMultimap.create();
        for (Map.Entry<UUID, Map<ItemStack, ItemStackConnections>> entry : consolidated.entrySet()) {
            UUID network = entry.getKey();
            InventorySummary summary = LogisticsManager.getSummaryOfNetwork(network, true);

            for (ItemStackConnections connections : entry.getValue().values()) {
                boolean available = connections.totalAmount != 0 && !connections.item.isEmpty() && summary.getCountOf(connections.item) >= connections.totalAmount;
                for (FactoryPanelConnection connection : connections)
                    sendEffect(connection.from, available);
                if (!available) {
                    failed.setTrue();
                    continue;
                }
                toRequest.put(network, new BigItemStack(connections.item, connections.totalAmount));
            }
        }

        if (failed.isTrue())
            return;

        PackageOrderWithCrafts craftingContext = activeCraftingArrangement.isEmpty()
                ? PackageOrderWithCrafts.empty()
                : PackageOrderWithCrafts.singleRecipe(activeCraftingArrangement.stream()
                        .map(stack -> new BigItemStack(stack.copyWithCount(1)))
                        .toList());

        List<Multimap<PackagerBlockEntity, PackagingRequest>> requests = new ArrayList<>();
        Map<UUID, Multimap<PackagerBlockEntity, PackagingRequest>> requestsByNetwork = new HashMap<>();
        Map<UUID, Integer> orderIds = new HashMap<>();
        for (Map.Entry<UUID, Collection<BigItemStack>> entry : toRequest.asMap().entrySet()) {
            PackageOrderWithCrafts order = new PackageOrderWithCrafts(new PackageOrder(new ArrayList<>(entry.getValue())), craftingContext.orderedCrafts());
            Multimap<PackagerBlockEntity, PackagingRequest> request = LogisticsManager.findPackagersForRequest(entry.getKey(), order, null, recipeAddress);
            requests.add(request);
            requestsByNetwork.put(entry.getKey(), request);
            orderIds.put(entry.getKey(), PanelRequestHelper.orderIdOf(request));
        }

        for (Multimap<PackagerBlockEntity, PackagingRequest> request : requests)
            for (PackagerBlockEntity packager : request.keySet())
                if (packager.isTooBusyFor(LogisticallyLinkedBehaviour.RequestType.RESTOCK))
                    return;

        var genericRequests = PanelRequestHelper.findGenericPackagers(genericToRequest, recipeAddress, orderIds);
        if (genericRequests.anyPackagerBusy())
            return;
        var genericNetworks = genericRequests.send(PanelRequestHelper.networksWithRequests(requestsByNetwork));
        PanelRequestHelper.flagVanillaRequests(requestsByNetwork, genericNetworks);

        for (Multimap<PackagerBlockEntity, PackagingRequest> request : requests)
            LogisticsManager.performPackageRequests(request);

        RequestPromiseQueue promises = Create.LOGISTICS.getQueuedPromises(network);
        if (promises != null)
            addPromises(promises);
        panelBE().advancements.awardPlayer(AllAdvancements.FACTORY_GAUGE);
    }

    @Override
    public void resetTimer() {
        timer = getConfigRequestIntervalInTicks();
    }

    @Override
    public void resetTimerSlightly() {
        timer = getConfigRequestIntervalInTicks() / 2;
    }

    /**
     * Adds the promise of what this gauge just ordered to the queue of its network
     */
    public abstract void addPromises(RequestPromiseQueue queue);

    /**
     * Called when the gauge is placed on a packager of its type and should restock it
     */
    public void tryRestock() {
    }

    protected void sendEffect(FactoryPanelPosition fromPos, boolean success) {
        if (getWorld() instanceof ServerLevel serverLevel)
            CatnipServices.NETWORK.sendToClientsAround(serverLevel, getPos(), 64, new FactoryPanelEffectPacket(fromPos, getPanelPosition(), success));
    }

    private void tickOutline() {
        CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> LogisticallyLinkedClientHandler.tickPanel(this));
    }

    @Override
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        nbt.putInt("Timer", timer);
        nbt.putInt("LastLevel", lastReportedLevelInStorage);
        nbt.putInt("LastPromised", lastReportedPromises);
        nbt.putInt("LastUnloadedLinks", lastReportedUnloadedLinks);
        nbt.putBoolean("Satisfied", satisfied);
        nbt.putBoolean("PromisedSatisfied", promisedSatisfied);
        nbt.putString("RecipeAddress", recipeAddress);
        nbt.putInt("RecipeOutput", recipeOutput);
        nbt.putInt("PromiseClearingInterval", promiseClearingInterval);
        nbt.putUUID("Freq", network);
        nbt.put("Craft", NBTHelper.writeItemList(activeCraftingArrangement, registries));
    }

    @Override
    public void easyWriteSafe(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putString("RecipeAddress", recipeAddress);
        nbt.putUUID("Freq", network);
    }

    @Override
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        timer = nbt.getInt("Timer");
        lastReportedLevelInStorage = nbt.getInt("LastLevel");
        lastReportedPromises = nbt.getInt("LastPromised");
        lastReportedUnloadedLinks = nbt.getInt("LastUnloadedLinks");
    }

    @Override
    public String canConnect(FactoryPanelBehaviour from) {
        if(isFilterEmpty()) return "factory_panel.no_item";
        return super.canConnect(from);
    }

    @Override
    public int getLevelInStorage() {
        if (blockEntity.isVirtual())
            return 1;
        if (getWorld().isClientSide())
            return lastReportedLevelInStorage;
        if (isFilterEmpty())
            return 0;

        return getActualLevelInStorage();
    }

    /**
     * @return The amount of the filter currently on the network, in base units
     */
    protected abstract int getActualLevelInStorage();

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
        if (isFilterEmpty())
            return 0;

        if (hasInteraction("restocker")) {
            if (forceClearPromises) {
                forceClearPromises(this.restockerPromises);
                resetTimerSlightly();
            }
            forceClearPromises = false;
            return getTotalPromisedAndRemoveExpired(this.restockerPromises, getPromiseExpiryTimeInTicks());
        }

        var promises = Create.LOGISTICS.getQueuedPromises(network);
        if (promises == null)
            return 0;

        if (forceClearPromises) {
            forceClearPromises(promises);
            resetTimerSlightly();
        }
        forceClearPromises = false;

        return getTotalPromisedAndRemoveExpired(promises, getPromiseExpiryTimeInTicks());
    }

    /**
     * Removes every promise of this gauge's filter from the queue
     */
    protected abstract void forceClearPromises(RequestPromiseQueue queue);

    /**
     * @return The promised amount of this gauge's filter, after removing promises older than the expiry time
     */
    protected abstract int getTotalPromisedAndRemoveExpired(RequestPromiseQueue queue, int promiseExpiryTime);

    @Override
    public MutableComponent getLabel() {
        String key;

        if (!targetedBy.isEmpty() && !hasAmount())
            return CreateLang.translate("gui.factory_panel.no_target_amount_set")
                    .style(ChatFormatting.RED)
                    .component();

        if (isMissingAddress())
            return CreateLang.translate("gui.factory_panel.address_missing")
                    .style(ChatFormatting.RED)
                    .component();

        if (isFilterEmpty())
            key = "factory_panel.new_factory_task";
        else if (waitingForNetwork)
            key = "factory_panel.some_links_unloaded";
        else if (getDemand() == 0 || targetedBy.isEmpty())
            return getHoverName().plainCopy();
        else {
            key = getHoverName()
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

    /**
     * @return The name of the filter
     */
    public abstract Component getHoverName();

    @Override
    public MutableComponent getCountLabelForValueBox() {
        if (isFilterEmpty())
            return Component.empty();
        if (waitingForNetwork) {
            return Component.literal("?");
        }

        int levelInStorage = getLevelInStorage() / getMultiplier();
        boolean inf = levelInStorage >= BigItemStack.INF;

        int promised = getPromised();

        var inStorageName = getCountName(levelInStorage);

        if (!hasAmount()) {
            return CreateLang.text(inf ? "  ∞" : inStorageName)
                    .color(0xF1EFE8)
                    .component();
        }

        return CreateLang.text(inf ? "  ∞" : "   " + inStorageName)
                .color(satisfied ? 0xD7FFA8 : promisedSatisfied ? 0xffcd75 : 0xFFBFA8)
                .add(CreateLang.text(promised == 0 ? "" : "⏶"))
                .add(CreateLang.text("/")
                        .style(ChatFormatting.WHITE))
                .add(CreateLang.text(formatCount(count) + "  ")
                        .color(0xF1EFE8))
                .component();
    }

    /**
     * @return Whether the player set an amount
     */
    public boolean hasAmount() {
        return count != 0;
    }

    /**
     * @return The size of one display unit, used to format the stock level
     */
    public abstract int getMultiplier();

    public abstract void setItem(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult, boolean client);

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult, boolean client) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (isFilterEmpty()) {
            // Open screen for setting an item through JEI
            if (heldItem.isEmpty()) {
                if (!client && player instanceof ServerPlayer sp)
                    sp.openMenu(this, buf -> FactoryPanelPosition.STREAM_CODEC.encode(buf, getPanelPosition()));
                return;
            }

            // Use regular filter interaction for setting the item
            setItem(player, hand, side, hitResult, client);
            return;
        }

        // Bind logistics items to this panels' frequency
        if (heldItem.getItem() instanceof LogisticallyLinkedBlockItem || heldItem.getItem() instanceof LogisticallyLinked) {
            if (!client)
                LogisticallyLinkedBlockItem.assignFrequency(heldItem, player, network);
            return;
        }
        super.onShortInteract(player, hand, side, hitResult, client);
    }

    /**
     * @return The amount formatted for display
     */
    public String formatCount(int count) {
        return getCountName(count);
    }

    /**
     * @return The display text of an amount, with its unit
     */
    public abstract String getCountName(int inStorage);

    @Override
    public boolean isCountVisible() {
        return !isFilterEmpty();
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        int maxAmount = 100;
        return new ValueSettingsBoard(CreateLang.translate("factory_panel.target_amount")
                .component(), maxAmount, 10,
                List.of(CreateLang.translate("schedule.condition.threshold.items")
                                .component(),
                        CreateLang.translate("schedule.condition.threshold.stacks")
                                .component()),
                new ValueSettingsFormatter(this::formatValue));
    }
}
