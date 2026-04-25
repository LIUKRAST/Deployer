package net.liukrast.deployer.lib.logistics.board;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.*;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.ScreenOpener;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnectionBuilder;
import net.liukrast.deployer.lib.logistics.board.connection.PanelInteractionBuilder;
import net.liukrast.deployer.lib.logistics.board.connection.StockConnection;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlock;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.IdentifiedContainer;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.GenericRequestPromise;
import net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixinExtensions.RPQExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;

public abstract class StockPanelBehaviour<K, V> extends OrderingPanelBehaviour {
    private final StockInventoryType<K, V, ?> stockInventoryType;
    protected V filter;
    private int scale;

    public StockPanelBehaviour(StockInventoryType<K, V, ?> stockInventoryType, PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
        this.stockInventoryType = stockInventoryType;
        this.filter = stockInventoryType.valueHandler().empty();

    }

    public abstract Multiplier[] getMultiplierMode();

    public StockInventoryType<K, V, ?> getStockInventoryType() {
        return stockInventoryType;
    }

    @Override
    public void addConnections(PanelConnectionBuilder builder) {
        builder.registerBoth(DeployerPanelConnections.STOCK_CONNECTION.get(), () -> StockConnection.of(stockInventoryType, filter));
        super.addConnections(builder);
    }

    @Override
    public void addInteractions(PanelInteractionBuilder builder) {
        builder.registerEntity("restocker", be -> be instanceof AbstractPackagerBlockEntity<?,?,?> a && a.getStockType() == stockInventoryType);
    }



    @Override
    public void reset() {
        this.filter = stockInventoryType.valueHandler().empty();
    }

    @Override
    public BulbState getBulbState() {
        return getAmount() > 0 ? (redstonePowered || isMissingAddress() ? BulbState.RED : BulbState.GREEN) : BulbState.DISABLED;
    }

    @Override
    public void tryRestock() {
        V item = getStack();
        if (isFilterEmpty())
            return;

        if(!(getInteractionBlockEntity("restocker") instanceof AbstractPackagerBlockEntity<?,?,?> apb))
            return;
        if(apb.getStockType() != getStockInventoryType())
            return;
        @SuppressWarnings("unchecked")
        AbstractPackagerBlockEntity<K,V,?> packager = (AbstractPackagerBlockEntity<K, V, ?>) apb;
        if (!packager.targetInventory.hasInventory())
            return;

        int availableOnNetwork = packager.getStockOf(network, item);
        if (availableOnNetwork == 0) {
            sendEffect(getPanelPosition(), false);
            return;
        }

        int inStorage = getLevelInStorage();
        int promised = getPromised();
        int demand = getAmount() * getDemandMultiplier();
        int amountToOrder = org.joml.Math.clamp(demand - promised - inStorage, 0, getDemandMultiplier() * 9 /* TODO: Give this a look */);

        var orderedItem = getStockInventoryType().valueHandler().copyWithCount(item, Math.min(amountToOrder, availableOnNetwork));
        GenericOrderContained<V> order = getStockInventoryType().valueHandler().createContained(List.of(orderedItem));

        sendEffect(getPanelPosition(), true);

        if(!packager.pleaseBroadcast(network, LogisticallyLinkedBehaviour.RequestType.RESTOCK, order, recipeAddress))
            return;

        ((RPQExtension)restockerPromises).deployer$add(getStockInventoryType(), new GenericRequestPromise<>(orderedItem));
    }

    @Override
    public void easyWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.easyWrite(nbt, registries, clientPacket);

        stockInventoryType.valueHandler().codec().encodeStart(NbtOps.INSTANCE, filter)
                .result()
                .ifPresent(tag -> nbt.put("Stack", tag));
        nbt.putInt("Count", count);
        nbt.putInt("Scale", scale);
        nbt.putInt("FilterAmount", count);
    }

    @Override
    public void easyRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.easyRead(nbt, registries, clientPacket);
        if (nbt.contains("Stack")) {
            stockInventoryType.valueHandler().codec().parse(NbtOps.INSTANCE, nbt.get("Stack"))
                    .result()
                    .ifPresent(stack -> this.filter = stack);
        } else {
            this.filter = stockInventoryType.valueHandler().empty();
        }
        count = nbt.getInt("Count");
        scale = nbt.getInt("Scale");
    }

    @Override
    public String getCountName(int count) {
        return count + getMultiplierMode()[scale].key;
    }

    @Override
    public void addPromises(RequestPromiseQueue queue) {
        ((RPQExtension)queue).deployer$add(stockInventoryType, new GenericRequestPromise<>(stockInventoryType.valueHandler().copyWithCount(filter, recipeOutput)));
    }


    @Override
    public int getMultiplier() {
        return getMultiplierMode()[scale].value;
    }

    @Override
    public boolean isFilterEmpty() {
        return stockInventoryType.valueHandler().isEmpty(filter);
    }

    @Override
    public int getDemandMultiplier() {
        return getMultiplierMode()[scale].value;
    }

    @Override
    public String canConnect(FactoryPanelBehaviour from) {
        if(hasInteraction("restocker")) return "factory_panel.input_in_restock_mode";
        return super.canConnect(from);
    }

    @Override
    public int getActualLevelInStorage() {
        if(!hasInteraction("restocker"))
            return LogisticsGenericManager.getSummaryOfNetwork(stockInventoryType, network, false).getCountOf(filter);
        BlockEntity attached = getInteractionBlockEntity("restocker");
        if (attached == null)
            return 0;
        if(attached instanceof AbstractPackagerBlockEntity<?,?,?> apbe && apbe.getStockType() == stockInventoryType) {
            //noinspection unchecked
            return ((AbstractPackagerBlockEntity<K, V, Object>) apbe).getAvailableStacks().getCountOf(filter);
        }
        return 0;
    }

    @OnlyIn(Dist.CLIENT)
    public void displayScreen(Player player) {
        if (player instanceof LocalPlayer) {
            ScreenOpener.open(new FactoryPanelScreen(this));
        }
    }

    @Override
    protected void forceClearPromises(RequestPromiseQueue queue) {
        ((RPQExtension)(queue)).deployer$forceClear(stockInventoryType, filter);
    }

    @Override
    protected int getTotalPromisedAndRemoveExpired(RequestPromiseQueue queue, int promiseExpiryTime) {
        return ((RPQExtension)(queue)).deployer$getTotalPromisedAndRemoveExpired(stockInventoryType, filter, promiseExpiryTime);
    }

    public abstract V parseFromHeldItem(ItemStack heldItem);

    @Override
    public void setItem(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult, boolean client) {
        V stack = parseFromHeldItem(player.getItemInHand(hand));
        if(!stockInventoryType.valueHandler().isEmpty(stack)) {
            filter = stockInventoryType.valueHandler().copy(stack);
        }
    }

    public V getStack() {
        return filter;
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
