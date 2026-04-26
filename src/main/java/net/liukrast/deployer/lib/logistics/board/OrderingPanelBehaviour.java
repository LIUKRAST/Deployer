package net.liukrast.deployer.lib.logistics.board;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.sugar.Local;
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
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.logistics.LogisticallyLinked;
import net.liukrast.deployer.lib.logistics.board.connection.*;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.GenericPackagingRequest;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager;
import net.liukrast.deployer.lib.mixin.accessors.LogisticsManagerAccessor;
import net.liukrast.deployer.lib.mixinExtensions.PRExtension;
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
import org.joml.Math;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class OrderingPanelBehaviour extends AbstractPanelBehaviour implements RenderFilterSlot {
    // Factory gauge inherits
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
            builder.registerBoth(DeployerPanelConnections.REDSTONE, () -> this.satisfied && count != 0);
        if(intInput())
            builder.registerBoth(DeployerPanelConnections.NUMBERS, () -> (float)this.getLevelInStorage());
        if(stringInput())
            builder.registerBoth(DeployerPanelConnections.STRING, () -> getDisplayLinkComponent(false).getString());
    }

    public boolean redstoneInput() {
        return true;
    }

    public boolean intInput() {
        return true;
    }

    public boolean stringInput() {
        return true;
    }

    @Override
    public boolean isMissingAddress() {
        return (!targetedBy.isEmpty() || hasInteraction("restocker")) && count != 0 && recipeAddress.isBlank();
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

    //moveTo
    //moveToSlot
    //initialize

    @Override
    public void notifiedFromInput() {
        if (!active)
            return;

        boolean anyChange = false;

        if(redstoneInput()) {

            boolean shouldPower;
            //Injected
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
        if (!hasInteraction("restocker") && unloadedLinkCount == 0 && lastReportedLevelInStorage != 0) {
            // All links have been loaded, invalidate the cache so we can get an accurate summary!
            // Otherwise, we will have to wait for 20 ticks and unnecessary packages will be sent!
            LogisticsManager.SUMMARIES.invalidate(network);
        }
        int inStorage = getLevelInStorage();
        int promised = getPromised();
        int demand = getAmount() * getDemandMultiplier();
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
            notifyOutputs();
    }

    public int getDemandMultiplier() {
        return 1;
    }

    //notifyRedstoneOutputs

    private int getConfigRequestIntervalInTicks() {
        return AllConfigs.server().logistics.factoryGaugeTimer.get();
    }

    private void tickRequests() {
        //region Injected
        Map<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> future$consolidated = new HashMap<>();
        Map<StockInventoryType<?,?,?>, Map<UUID, Map<?, GenericConnections<?>>>> consolidated$1 = new HashMap<>();
        AtomicBoolean realTargetedEmpty = new AtomicBoolean(true);
        Map<FactoryPanelPosition, FactoryPanelConnection> realTargeted = targetedBy
                .entrySet()
                .stream()
                .filter(e -> {
                    var connection = e.getValue();
                    FactoryPanelBehaviour source = at(getWorld(), connection);
                    var pc = ProvidesConnection.getCurrentConnection(connection, () -> ProvidesConnection.getPossibleConnections(source, (ProvidesConnection) this).stream().findFirst().orElse(null));
                    if(pc == null || pc != DeployerPanelConnections.STOCK_CONNECTION.get()) return false;
                    PanelValue<StockConnection<?>> result = AbstractPanelBehaviour.getValue(connection, DeployerPanelConnections.STOCK_CONNECTION.get(), this);
                    if(result instanceof PanelValue.Present<StockConnection<?>>(StockConnection<?> value))
                        value.register(future$consolidated, consolidated$1, source, connection);
                    realTargetedEmpty.set(false);
                    return !(source instanceof AbstractPanelBehaviour);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        //endregion
        FactoryPanelBlockEntity panelBE = panelBE();
        if (realTargetedEmpty.get() /*Injected*/ && !hasInteraction("restocker"))
            return;
        if (hasInteraction("restocker"))
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

        if (hasInteraction("restocker")) {
            tryRestock();
            return;
        }

        // Injected boolean -> AtomicBoolean
        AtomicBoolean failed = new AtomicBoolean(false);

        Map<UUID, Map<ItemStack, ItemStackConnections>> consolidated = new HashMap<>(/* Injected */future$consolidated);

        for (FactoryPanelConnection connection : realTargeted.values() /* Injected */) {
            FactoryPanelBehaviour source = at(getWorld(), connection);
            if (source == null)
                return;

            ItemStack item = source.getFilter();


            Map<ItemStack, ItemStackConnections> networkItemCounts = consolidated.computeIfAbsent(source.network, $ -> new Object2ObjectOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG));
            networkItemCounts.computeIfAbsent(item, $ -> new ItemStackConnections(item));
            ItemStackConnections existingConnections = networkItemCounts.get(item);
            existingConnections.add(connection);
            existingConnections.totalAmount += connection.amount;
        }

        Multimap<UUID, BigItemStack> toRequest = HashMultimap.create();

        //region Injected
        Map<StockInventoryType<?,?,?>, Multimap<UUID, ?>> toRequest$1 = new HashMap<>();
        Map<UUID, Integer> savedUUIDs = new HashMap<>();
        for (var e : consolidated$1.entrySet()) deployer$tickRequests(e.getKey(), e.getValue(), failed::set, toRequest$1.computeIfAbsent(e.getKey(), k -> HashMultimap.create()));

        //endregion

        for (Map.Entry<UUID, Map<ItemStack, ItemStackConnections>> entry : consolidated.entrySet()) {
            UUID network = entry.getKey();
            InventorySummary summary = LogisticsManager.getSummaryOfNetwork(network, true);

            for (ItemStackConnections connections : entry.getValue().values()) {
                if (connections.totalAmount == 0 || connections.item.isEmpty() || summary.getCountOf(connections.item) < connections.totalAmount) {
                    for (FactoryPanelConnection connection : connections)
                        sendEffect(connection.from, false);
                    failed.set(true);
                    continue;
                }

                BigItemStack stack = new BigItemStack(connections.item, connections.totalAmount);
                toRequest.put(network, stack);
                for (FactoryPanelConnection connection : connections)
                    sendEffect(connection.from, true);
            }
        }

        if (failed.get())
            return;

        // Input items may come from differing networks
        Map<UUID, Collection<BigItemStack>> asMap = toRequest.asMap();
        PackageOrderWithCrafts craftingContext = PackageOrderWithCrafts.empty();
        List<Multimap<PackagerBlockEntity, PackagingRequest>> requests = new ArrayList<>();

        // Panel may enforce item arrangement
        if (!activeCraftingArrangement.isEmpty())
            craftingContext = PackageOrderWithCrafts.singleRecipe(activeCraftingArrangement.stream()
                    .map(stack -> new BigItemStack(stack.copyWithCount(1)))
                    .toList());

        //Injected
        boolean isValsEmpty = false;

        // Collect request distributions
        for (Map.Entry<UUID, Collection<BigItemStack>> entry : asMap.entrySet()) {
            PackageOrderWithCrafts order =
                    new PackageOrderWithCrafts(new PackageOrder(new ArrayList<>(entry.getValue())), craftingContext.orderedCrafts());
            Multimap<PackagerBlockEntity, PackagingRequest> request =
                    LogisticsManager.findPackagersForRequest(entry.getKey(), order, null, recipeAddress);
            requests.add(request);
            //region Injected
            var vals = request.values();
            int id = vals.isEmpty() ? LogisticsManagerAccessor.getR().nextInt() :
                    Optional.ofNullable(vals.iterator().next()).map(PackagingRequest::orderId)
                    .orElseGet(() -> LogisticsManagerAccessor.getR().nextInt());
            savedUUIDs.put(entry.getKey(), id);
            isValsEmpty = vals.isEmpty();
            //endregion
        }

        //region Injected
        Map<StockInventoryType<?,?,?>, Map<UUID, Collection<?>>> asMap$1 = new HashMap<>();
        Map<StockInventoryType<?,?,?>, List<Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>>>> d$requests = new HashMap<>();
        // Map init
        for(var e : toRequest$1.entrySet()) {
            //noinspection unchecked,rawtypes
            asMap$1.put(e.getKey(), (Map)e.getValue().asMap());
            d$requests.put(e.getKey(), new ArrayList<>());
        }

        // Collect request distributions
        for(var e : asMap$1.entrySet()) {
            deployer$tickRequests(e.getKey(), e.getValue(), d$requests.get(e.getKey()), savedUUIDs);
        }

        // Check if any packager is busy, cancel all
        for(var e : d$requests.values()) {
            for(var entry : e) {
                for(AbstractPackagerBlockEntity<?,?,?> packager : entry.keySet())
                    if(packager.isTooBusyFor(LogisticallyLinkedBehaviour.RequestType.RESTOCK)) {
                        return;
                    }
            }
        }

        // Send it

        int index = isValsEmpty ? 0 : 1;
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
        //endregion
        // Check if any packager is busy - cancel all
        for (Multimap<PackagerBlockEntity, PackagingRequest> entry : requests)
            for (PackagerBlockEntity packager : entry.keySet())
                if (packager.isTooBusyFor(LogisticallyLinkedBehaviour.RequestType.RESTOCK))
                    return;

        // Send it
        for (Multimap<PackagerBlockEntity, PackagingRequest> entry : requests)
            LogisticsManager.performPackageRequests(entry);

        // Keep the output promise
        RequestPromiseQueue promises = Create.LOGISTICS.getQueuedPromises(network);

        if (promises != null)
            addPromises(promises);
        panelBE.advancements.awardPlayer(AllAdvancements.FACTORY_GAUGE);
    }

    @Override
    public void resetTimer() {
        timer = getConfigRequestIntervalInTicks();
    }

    @Override
    public void resetTimerSlightly() {
        timer = getConfigRequestIntervalInTicks() / 2;
    }

    public abstract void addPromises(RequestPromiseQueue queue);

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <K, V, H> void deployer$tickRequests(
            StockInventoryType<K, V, H> type,
            Map<UUID, Map<?, GenericConnections<?>>> map,
            @Local BooleanConsumer failed,
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
                    failed.accept(true);
                    continue;
                }

                V stack = type.valueHandler().copyWithCount(connections.item, connections.totalAmount);
                ((Multimap<UUID, V>) toRequest).put(network, stack);
                for (FactoryPanelConnection connection : connections)
                    sendEffect(connection.from, true);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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
    private <K,V,H> void deployer$tickRequests(Multimap<AbstractPackagerBlockEntity<?,?,?>, GenericPackagingRequest<?>> entry$raw, int index, boolean isLast) {
        Multimap<AbstractPackagerBlockEntity<K,V,H>, GenericPackagingRequest<V>> entry = (Multimap)entry$raw;
        LogisticsGenericManager.performPackageRequests(entry, index, isLast);
    }


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
        nbt.putString("RecipeAddress", recipeAddress);
        nbt.putInt("RecipeOutput", recipeOutput);
        nbt.putInt("PromiseClearingInterval", promiseClearingInterval);
        nbt.putUUID("Freq", network);
        nbt.put("Craft", NBTHelper.writeItemList(activeCraftingArrangement, registries));
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

    protected abstract void forceClearPromises(RequestPromiseQueue queue);

    protected abstract int getTotalPromisedAndRemoveExpired(RequestPromiseQueue queue, int promiseExpiryTime);

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

        if (isFilterEmpty())
            key = "factory_panel.new_factory_task";
        else if (waitingForNetwork)
            key = "factory_panel.some_links_unloaded";
        else if (getAmount() == 0 || targetedBy.isEmpty())
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

        if (count == 0) {
            return CreateLang.text(inf ? "  ∞" : inStorageName)
                    .color(0xF1EFE8)
                    .component();
        }

        return CreateLang.text(inf ? "  ∞" : "   " + inStorageName)
                .color(satisfied ? 0xD7FFA8 : promisedSatisfied ? 0xffcd75 : 0xFFBFA8)
                .add(CreateLang.text(promised == 0 ? "" : "⏶"))
                .add(CreateLang.text("/")
                        .style(ChatFormatting.WHITE))
                .add(CreateLang.text(getCountName(count) + "  ")
                        .color(0xF1EFE8))
                .component();
    }

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
