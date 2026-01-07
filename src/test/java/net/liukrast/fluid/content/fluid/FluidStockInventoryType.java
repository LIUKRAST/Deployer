package net.liukrast.fluid.content.fluid;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import io.netty.buffer.ByteBuf;
import net.liukrast.deployer.lib.helper.GuiRenderingHelper;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.GenericPackageItem;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.GenericRequestPromise;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.fluid.TestConstants;
import net.liukrast.fluid.registry.RegisterDataComponents;
import net.liukrast.fluid.registry.RegisterItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import static com.simibubi.create.foundation.gui.AllGuiTextures.NUMBERS;

public class FluidStockInventoryType extends StockInventoryType<Fluid, FluidStack, IFluidHandler> {
    private static final Codec<GenericRequestPromise<FluidStack>> REQUEST_CODEC =  GenericRequestPromise.simpleCodec(FluidStack.CODEC);
    private static final IValueHandler<Fluid, FluidStack, IFluidHandler> VALUE_HANDLER = new IValueHandler<>() {
        @Override
        public Codec<FluidStack> codec() {
            return FluidStack.CODEC;
        }

        @Override
        public StreamCodec<? extends ByteBuf, FluidStack> streamCodec() {
            return FluidStack.STREAM_CODEC;
        }

        @Override
        public Fluid fromValue(FluidStack key) {
            return key.getFluid();
        }

        @Override
        public boolean equalsIgnoreCount(FluidStack a, FluidStack b) {
            return FluidStack.isSameFluidSameComponents(a, b);
        }

        @Override
        public boolean test(FilterItemStack filter, Level level, FluidStack value) {
            return filter.test(level, value);
        }

        @Override
        public int getCount(FluidStack value) {
            return value.getAmount();
        }

        @Override
        public void setCount(FluidStack value, int count) {
            value.setAmount(count);
        }

        @Override
        public boolean isEmpty(FluidStack stack) {
            return stack.isEmpty();
        }

        @Override
        public FluidStack create(Fluid key, int amount) {
            return new FluidStack(key, amount);
        }

        @Override
        public void shrink(FluidStack stack, int amount) {
            stack.shrink(amount);
        }

        @Override
        public FluidStack copyWithCount(FluidStack stack, int amount) {
            return stack.copyWithAmount(amount);
        }

        @Override
        public FluidStack copy(FluidStack stack) {
            return stack.copy();
        }

        @Override
        public boolean isStackable(FluidStack stack) {
            return true; //TODO: Check this but it should be correct
        }

        @Override
        public FluidStack empty() {
            return FluidStack.EMPTY;
        }
    };
    private static final IStorageHandler<Fluid, FluidStack, IFluidHandler> STORAGE_HANDLER = new IStorageHandler<>() {
        @Override
        public int getSlots(IFluidHandler handler) {
            return handler.getTanks();
        }

        @Override
        public FluidStack getStackInSlot(IFluidHandler handler, int slot) {
            return handler.getFluidInTank(slot);
        }

        @Override
        public int maxCountPerSlot() {
            return 1000; //TODO: Check
        }

        @Override
        public FluidStack extract(IFluidHandler handler, FluidStack value, boolean simulate) {
            return handler.drain(value, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public int fill(IFluidHandler handler, FluidStack stack, boolean simulate) {
            return stack.getAmount() - handler.fill(stack.copy(), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public FluidStack setInSlot(IFluidHandler handler, int slot, FluidStack value, boolean simulate) {
            //TODO: Check if this is correct
            int result = fill(handler, value, simulate);
            return new FluidStack(value.getFluid(), result);
        }

        @Override
        public boolean isBulky(Fluid key) {
            return false;
        }

        @Override
        public IFluidHandler create(int slots) {
            return new FluidTank(1000);
        }

        @Override
        public int getMaxPackageSlots() {
            return 1;
        }

        @Override
        public FluidStack insertItem(IFluidHandler handler, int i, FluidStack value, boolean simulate) {
            return new FluidStack(value.getFluid(), fill(handler, value, simulate));
        }
    };

    private static final INetworkHandler<Fluid, FluidStack, IFluidHandler> NETWORK_HANDLER = new INetworkHandler<>() {
        @Override
        public Codec<GenericRequestPromise<FluidStack>> requestCodec() {
            return REQUEST_CODEC;
        }

        @Override
        public AbstractInventorySummary<Fluid, FluidStack> create() {
            return new FluidInventorySummary();
        }

        @Override
        public AbstractInventorySummary<Fluid, FluidStack> empty() {
            return FluidInventorySummary.EMPTY.get();
        }

        @Override
        public DataComponentType<? super GenericPackageOrderData<FluidStack>> getComponent() {
            return RegisterDataComponents.BOTTLE_ORDER_DATA.get();
        }
    };

    private static final IPackageHandler<Fluid, FluidStack, IFluidHandler> PACKAGE_HANDLER = new IPackageHandler<>() {
        @Override
        public void setBoxContent(ItemStack stack, IFluidHandler inventory) {
            stack.set(RegisterDataComponents.BOTTLE_CONTENTS, SimpleFluidContent.copyOf(inventory.getFluidInTank(0))); //TODO: Check this
        }

        private static final Random STYLE_PICKER = new Random();
        private static final int RARE_CHANCE = 7500;

        @Override
        public ItemStack getRandomBox() {
            List<DeferredItem<GenericPackageItem>> pool = STYLE_PICKER.nextInt(RARE_CHANCE) == 0 ? RegisterItems.RARE_BOTTLES : RegisterItems.STANDARD_BOTTLES;
            return new ItemStack(pool.get(STYLE_PICKER.nextInt(pool.size())).get());
        }

        @Override
        public IFluidHandler getContents(ItemStack box) {
            FluidTank newInv = new FluidTank(1000); //TODO: Check this
            SimpleFluidContent contents = box.getOrDefault(RegisterDataComponents.BOTTLE_CONTENTS.get(), SimpleFluidContent.EMPTY);
            newInv.fill(contents.copy(), IFluidHandler.FluidAction.EXECUTE); //TODO: Check this again
            return newInv;
        }

        @Override
        public DataComponentType<GenericPackageOrderData<FluidStack>> packageOrderData() {
            return RegisterDataComponents.BOTTLE_ORDER_DATA.get();
        }

        @Override
        public DataComponentType<GenericOrderContained<FluidStack>> packageOrderContext() {
            return RegisterDataComponents.BOTTLE_ORDER_CONTEXT.get();
        }

        @Override
        public int clickAmount(boolean ctrlDown, boolean shiftDown, boolean altDown) {
            return ctrlDown ? 100 : shiftDown ? 1000 : altDown ? 1 : 10;
        }

        @Override
        public int scrollAmount(boolean ctrlDown, boolean shiftDown, boolean altDown) {
            return ctrlDown ? 100 : shiftDown ? 1000 : altDown ? 1 : 10;
        }

        @Override
        public boolean matchesModSearch(FluidStack stack, String searchValue) {
            return BuiltInRegistries.FLUID.getKey(stack.getFluid()).getNamespace().contains(searchValue);
        }

        @Override
        public boolean matchesTagSearch(FluidStack stack, String searchValue) {
            //noinspection deprecation
            return stack.getFluid().builtInRegistryHolder().tags().anyMatch(key -> key.location().toString().contains(searchValue));
        }

        @Override
        public boolean matchesSearch(FluidStack stack, String searchValue) {
            return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(searchValue) || BuiltInRegistries.FLUID.getKey(stack.getFluid()).getPath().contains(searchValue);
        }

        @Override
        public void renderCategory(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<FluidStack> category, List<FluidStack> itemsToOrder, AbstractInventorySummary<Fluid, FluidStack> forcedEntries, CategoryRenderData data) {
            for (int index = 0; index < category.size(); index++) {
                int pY = data.itemsY() + data.categoryY() + (data.categories().isEmpty() ? 4 : data.rowHeight()) + (index / data.cols()) * data.rowHeight();
                float cullY = pY - data.currentScroll() * data.rowHeight();

                if (cullY < data.y())
                    continue;
                if (cullY > data.y() + data.windowHeight() - 72)
                    break;

                boolean isStackHovered = index == data.hoveredSlot().getSecond() && data.categoryIndex() == data.hoveredSlot().getFirst();
                FluidStack entry = category.get(index);

                data.ms().pushPose();
                data.ms().translate(data.itemsX() + (index % data.cols()) * data.colWidth(), pY, 0);
                renderFluidEntry(graphics, entry, isStackHovered, false, data.colWidth(), data.rowHeight(), itemsToOrder, forcedEntries);
                data.ms().popPose();
            }
        }

        private void renderFluidEntry(GuiGraphics graphics, FluidStack entry, boolean isStackHovered,
                                      boolean isRenderingOrders, int colWidth, int rowHeight, List<FluidStack> itemsToOrder, AbstractInventorySummary<Fluid, FluidStack> forcedEntries) {

            int customCount = entry.getAmount();
            if (!isRenderingOrders) {
                FluidStack order = getOrderForFluid(entry, itemsToOrder);
                if (entry.getAmount() < BigItemStack.INF) {
                    int forcedCount = forcedEntries.getCountOf(entry);
                    if (forcedCount != 0)
                        customCount = Math.min(customCount, -forcedCount - 1);
                    if (order != null)
                        customCount -= order.getAmount();
                    customCount = Math.max(0, customCount);
                }
                AllGuiTextures.STOCK_KEEPER_REQUEST_SLOT.render(graphics, 0, 0);
            }

            //entry instanceof CraftableBigItemStack;
            PoseStack ms = graphics.pose();
            ms.pushPose();

            float scaleFromHover = 1;
            if (isStackHovered)
                scaleFromHover += .075f;

            ms.translate((colWidth - 18) / 2.0, (rowHeight - 18) / 2.0, 0);
            ms.translate(18 / 2.0, 18 / 2.0, 0);
            ms.scale((float) 1, (float) 1, (float) 1);
            ms.scale(scaleFromHover, scaleFromHover, scaleFromHover);
            ms.translate(-18 / 2.0, -18 / 2.0, 0);
            if(customCount != 0) GuiRenderingHelper.renderFluid(graphics, entry, 0, 0, 16,16);
            ms.popPose();

            ms.pushPose();
            ms.translate(0, 0, 190);
            ms.translate(0, 0, 10);
            if (customCount > 1)
                drawItemCount(graphics, customCount);
            ms.popPose();
        }

        private FluidStack getOrderForFluid(FluidStack stack, List<FluidStack> itemsToOrder) {
            for (FluidStack entry : itemsToOrder)
                if (FluidStack.isSameFluidSameComponents(stack, entry))
                    return entry;
            return null;
        }


        private void drawItemCount(GuiGraphics graphics, int customCount) {
            String text = customCount >= 1000000 ? (customCount / 1000000) + "m"
                    : customCount >= 10000 ? (customCount / 1000) + "k"
                    : customCount >= 1000 ? ((customCount * 10) / 1000) / 10f + "k" : customCount >= 100 ? customCount + "" : " " + customCount;

            if (customCount >= BigItemStack.INF)
                text = "+";

            if (text.isBlank())
                return;

            int x = (int) Math.floor(-text.length() * 2.5);
            for (char c : text.toCharArray()) {
                int index = c - '0';
                int xOffset = index * 6;
                int spriteWidth = NUMBERS.getWidth();

                switch (c) {
                    case ' ':
                        x += 4;
                        continue;
                    case '.':
                        spriteWidth = 3;
                        xOffset = 60;
                        break;
                    case 'k':
                        xOffset = 64;
                        break;
                    case 'm':
                        spriteWidth = 7;
                        xOffset = 70;
                        break;
                    case '+':
                        spriteWidth = 9;
                        xOffset = 84;
                        break;
                }

                RenderSystem.enableBlend();
                graphics.blit(NUMBERS.location, 14 + x, 10, 0, NUMBERS.getStartX() + xOffset, NUMBERS.getStartY(),
                        spriteWidth, NUMBERS.getHeight(), 256, 256);
                x += spriteWidth - 1;
            }

        }

        private static final ResourceLocation TEXTURE = TestConstants.id("textures/gui/fluid_stock_inventory.png");

        @Override
        public void renderOrderedItems(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<FluidStack> itemsToOrder, AbstractInventorySummary<Fluid, FluidStack> forcedEntries, OrderRenderData data) {
            graphics.blit(TEXTURE, data.itemsX()-39, data.orderY() - 8, 0, 0, 256, 48);
            var ms = data.ms();
            for(int index = 0; index < data.cols(); index++) {
                if(itemsToOrder.size() <= index) break;
                FluidStack entry = itemsToOrder.get(index);
                boolean isStackHovered = index == data.hoveredSlot().getSecond() && data.hoveredSlot().getFirst() == -1;

                ms.pushPose();
                ms.translate(data.itemsX() + index * data.colWidth(), data.orderY(), 0);
                renderFluidEntry(graphics, entry, isStackHovered, true, data.colWidth(), data.rowHeight(), itemsToOrder, forcedEntries);
                ms.popPose();
            }
        }

        @Override
        public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, FluidStack entry, Font font) {
            GuiRenderingHelper.renderTooltip(graphics, entry, mouseX, mouseY, font);
        }


    };

    @Override
    public @NotNull IValueHandler<Fluid, FluidStack, IFluidHandler> valueHandler() {
        return VALUE_HANDLER;
    }

    @Override
    public @NotNull IStorageHandler<Fluid, FluidStack, IFluidHandler> storageHandler() {
        return STORAGE_HANDLER;
    }

    @Override
    public @NotNull INetworkHandler<Fluid, FluidStack, IFluidHandler> networkHandler() {
        return NETWORK_HANDLER;
    }

    @Override
    public @NotNull IPackageHandler<Fluid, FluidStack, IFluidHandler> packageHandler() {
        return PACKAGE_HANDLER;
    }

    private static final ItemStack ICON = Items.WATER_BUCKET.getDefaultInstance();

    @Override
    public @NotNull ItemStack getIcon() {
        return ICON;
    }

    @Override
    public BlockCapability<IFluidHandler, @Nullable Direction> getBlockCapability() {
        return Capabilities.FluidHandler.BLOCK;
    }
}
