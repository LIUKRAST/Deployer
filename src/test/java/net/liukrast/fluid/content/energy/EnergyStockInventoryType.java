package net.liukrast.fluid.content.energy;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import io.netty.buffer.ByteBuf;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.GenericPackageItem;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.GenericRequestPromise;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.fluid.registry.RegisterDataComponents;
import net.liukrast.fluid.registry.RegisterItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class EnergyStockInventoryType extends StockInventoryType<Energy, EnergyStack, IEnergyStorage> {
    private static final Codec<GenericRequestPromise<EnergyStack>> REQUEST_CODEC = GenericRequestPromise.simpleCodec(EnergyStack.CODEC);

    private static final IValueHandler<Energy, EnergyStack, IEnergyStorage> VALUE_HANDLER = new IValueHandler<>() {

        @Override
        public Codec<EnergyStack> codec() {
            return EnergyStack.CODEC;
        }

        @Override
        public StreamCodec<ByteBuf, EnergyStack> streamCodec() {
            return EnergyStack.STREAM_CODEC;
        }

        @Override
        public Energy fromValue(EnergyStack key) {
            return Energy.INSTANCE;
        }

        @Override
        public boolean equalsIgnoreCount(EnergyStack a, EnergyStack b) {
            return true;
        }

        @Override
        public boolean test(FilterItemStack filter, Level level, EnergyStack value) {
            return true;
        }

        @Override
        public int getCount(EnergyStack value) {
            return value.getAmount();
        }

        @Override
        public void setCount(EnergyStack value, int count) {
            value.setAmount(count);
        }

        @Override
        public boolean isEmpty(EnergyStack stack) {
            return stack.isEmpty();
        }

        @Override
        public EnergyStack create(Energy key, int amount) {
            return new EnergyStack(amount);
        }

        @Override
        public void shrink(EnergyStack stack, int amount) {
            stack.setAmount(stack.getAmount() - amount);
        }

        @Override
        public EnergyStack copyWithCount(EnergyStack stack, int amount) {
            return new EnergyStack(amount);
        }

        @Override
        public EnergyStack copy(EnergyStack stack) {
            return new EnergyStack(stack.getAmount());
        }

        @Override
        public boolean isStackable(EnergyStack stack) {
            return true;
        }

        @Override
        public EnergyStack empty() {
            return EnergyStack.EMPTY;
        }
    };

    private static final IStorageHandler<Energy, EnergyStack, IEnergyStorage> STORAGE_HANDLER = new IStorageHandler<>() {
        @Override
        public int getSlots(IEnergyStorage handler) {
            return 1; //TODO: Check
        }

        @Override
        public EnergyStack getStackInSlot(IEnergyStorage handler, int slot) {
            return new EnergyStack(handler.getEnergyStored()); //TODO
        }

        @Override
        public int maxCountPerSlot() {
            return 0; //TODO: Check
        }

        @Override
        public EnergyStack extract(IEnergyStorage handler, EnergyStack value, boolean simulate) {
            if(!handler.canExtract()) return EnergyStack.EMPTY;
            return new EnergyStack(handler.extractEnergy(value.getAmount(), simulate));
        }

        @Override
        public int fill(IEnergyStorage handler, EnergyStack value, boolean simulate) {
            if(handler.canReceive()) return 0;
            return handler.receiveEnergy(value.getAmount(), simulate);
        }

        @Override
        public EnergyStack setInSlot(IEnergyStorage handler, int slot, EnergyStack value, boolean simulate) {
            return null; //TODO: Hopefully this should never be called in the energy context
        }

        @Override
        public boolean isBulky(Energy key) {
            return false;
        }

        @Override
        public IEnergyStorage create(int i) {
            return new EnergyStorage(1000);
        }

        @Override
        public int getMaxPackageSlots() {
            return 1;
        }

        @Override
        public EnergyStack insertItem(IEnergyStorage handler, int i, EnergyStack stack, boolean simulate) {
            if(!handler.canReceive()) return EnergyStack.EMPTY;
            return new EnergyStack(handler.receiveEnergy(stack.getAmount(), simulate));
        }
    };

    private static final INetworkHandler<Energy, EnergyStack, IEnergyStorage> NETWORK_HANDLER = new INetworkHandler<>() {
        @Override
        public Codec<GenericRequestPromise<EnergyStack>> requestCodec() {
            return REQUEST_CODEC;
        }

        @Override
        public AbstractInventorySummary<Energy, EnergyStack> create() {
            return new EnergyInventorySummary();
        }

        @Override
        public AbstractInventorySummary<Energy, EnergyStack> empty() {
            return EnergyInventorySummary.EMPTY.get();
        }

        @Override
        public DataComponentType<? super GenericPackageOrderData<EnergyStack>> getComponent() {
            return RegisterDataComponents.BATTERY_ORDER_DATA.get();
        }
    };

    private static final IPackageHandler<Energy, EnergyStack, IEnergyStorage> PACKAGE_HANDLER = new IPackageHandler<>() {
        @Override
        public void setBoxContent(ItemStack stack, IEnergyStorage inventory) {
            stack.set(RegisterDataComponents.BATTERY_CONTENTS, inventory.getEnergyStored());
        }

        private static final Random STYLE_PICKER = new Random();
        private static final int RARE_CHANCE = 7500;

        @Override
        public ItemStack getRandomBox() {
            List<DeferredItem<GenericPackageItem>> pool = STYLE_PICKER.nextInt(RARE_CHANCE) == 0 ? RegisterItems.RARE_BATTERIES : RegisterItems.STANDARD_BATTERIES;
            return new ItemStack(pool.get(STYLE_PICKER.nextInt(pool.size())).get());
        }

        @Override
        public IEnergyStorage getContents(ItemStack box) {
            return box.getCapability(Capabilities.EnergyStorage.ITEM);
        }

        @Override
        public DataComponentType<GenericPackageOrderData<EnergyStack>> packageOrderData() {
            return RegisterDataComponents.BATTERY_ORDER_DATA.get();
        }

        @Override
        public DataComponentType<GenericOrderContained<EnergyStack>> packageOrderContext() {
            return RegisterDataComponents.BATTERY_ORDER_CONTEXT.get();
        }

        @Override
        public int clickAmount(boolean ctrlDown, boolean shiftDown, boolean altDown) {
            return 0;
        }

        @Override
        public int scrollAmount(boolean ctrlDown, boolean shiftDown, boolean altDown) {
            return 0;
        }

        //TODO: Block the searchbar if it's not necessary

        @Override
        public boolean matchesModSearch(EnergyStack stack, String searchValue) {
            return true;
        }

        @Override
        public boolean matchesTagSearch(EnergyStack stack, String searchValue) {
            return true;
        }

        @Override
        public boolean matchesSearch(EnergyStack stack, String searchValue) {
            return true;
        }

        @Override
        public void renderCategory(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<EnergyStack> categoryStacks, List<EnergyStack> itemsToOrder, AbstractInventorySummary<Energy, EnergyStack> forcedEntries, CategoryRenderData data) {

        }

        @Override
        public void renderOrderedItems(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<EnergyStack> itemsToOrder, AbstractInventorySummary<Energy, EnergyStack> forcedEntries, OrderRenderData data) {

        }

        @Override
        public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, EnergyStack entry, Font font) {

        }
    };

    @Override
    public @NotNull IValueHandler<Energy, EnergyStack, IEnergyStorage> valueHandler() {
        return VALUE_HANDLER;
    }

    @Override
    public @NotNull IStorageHandler<Energy, EnergyStack, IEnergyStorage> storageHandler() {
        return STORAGE_HANDLER;
    }

    @Override
    public @NotNull INetworkHandler<Energy, EnergyStack, IEnergyStorage> networkHandler() {
        return NETWORK_HANDLER;
    }

    @Override
    public @NotNull IPackageHandler<Energy, EnergyStack, IEnergyStorage> packageHandler() {
        return PACKAGE_HANDLER;
    }

    private static final ItemStack ICON = Items.LIGHTNING_ROD.getDefaultInstance();

    @Override
    public @NotNull ItemStack getIcon() {
        return ICON;
    }

    @Override
    public BlockCapability<IEnergyStorage, @Nullable Direction> getBlockCapability() {
        return Capabilities.EnergyStorage.BLOCK;
    }
}
