package net.liukrast.deployer.lib.logistics.packager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.packagerLink.GenericRequestPromise;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/*
* K -> key (for items, Item)
* V -> value (for items, ItemStack)
* H -> handler (for items, IItemHandler)
* */
public abstract class StockInventoryType<K,V,H> {

    protected StockInventoryType() {}

    @NotNull public abstract IValueHandler<K,V,H> valueHandler();
    @NotNull public abstract IStorageHandler<K,V,H> storageHandler();
    @NotNull public abstract INetworkHandler<K,V,H> networkHandler();
    @NotNull public abstract IPackageHandler<K,V,H> packageHandler();
    @NotNull public abstract ItemStack getIcon();

    public interface IValueHandler<K,V,H> {
        Codec<V> codec();
        StreamCodec<? extends ByteBuf, V> streamCodec();
        K fromValue(V key);
        boolean equalsIgnoreCount(V a, V b);
        boolean test(FilterItemStack filter, Level level, V value);
        int getCount(V value);
        void setCount(V value, int count);
        boolean isEmpty(V stack);
        V create(K key, int amount);
        void shrink(V stack, int amount);
        V copyWithCount(V stack, int amount);
        V copy(V stack);
        boolean isStackable(V stack);
        V empty();
    }

    public interface IStorageHandler<K,V,H> {
        int getSlots(H handler); // returns the slots/tanks for items/fluids
        V getStackInSlot(H handler, int slot);
        int maxCountPerSlot();
        V extract(H handler, V value, boolean simulate);
        int fill(H handler, V value, boolean simulate);
        V setInSlot(H handler, int slot, V value, boolean simulate);
        boolean isBulky(K key);
        H create(int i);
        int getMaxPackageSlots();
        V insertItem(H handler, int i, V stack, boolean simulate);
    }

    public interface INetworkHandler<K,V,H> {
        Codec<GenericRequestPromise<V>> requestCodec();
        AbstractInventorySummary<K, V> create();
        AbstractInventorySummary<K,V> empty();
        DataComponentType<? super GenericPackageOrderData<V>> getComponent();
    }

    public interface IPackageHandler<K,V,H> {
        void setBoxContent(ItemStack stack, H inventory);
        ItemStack getRandomBox();
        H getContents(ItemStack box);
        DataComponentType<GenericPackageOrderData<V>> packageOrderData();
        DataComponentType<GenericOrderContained<V>> packageOrderContext();
        default ItemStack containing(H handler) {
            ItemStack box = getRandomBox();
            setBoxContent(box, handler);
            return box;
        }
        int clickAmount(boolean ctrlDown, boolean shiftDown, boolean altDown);
        int scrollAmount(boolean ctrlDown, boolean shiftDown, boolean altDown);
        boolean matchesModSearch(V stack, String searchValue);
        boolean matchesTagSearch(V stack, String searchValue);
        boolean matchesSearch(V stack, String searchValue);
        void renderCategory(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<V> categoryStacks, List<V> itemsToOrder, AbstractInventorySummary<K, V> forcedEntries, CategoryRenderData data);
        void renderOrderedItems(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, List<V> itemsToOrder, AbstractInventorySummary<K, V> forcedEntries, OrderRenderData data);
        void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, V entry, Font font);
        default void setOrder(ItemStack box, int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, @Nullable GenericOrderContained<V> orderContext) {
            GenericPackageOrderData<V> order = new GenericPackageOrderData<>(orderId, linkIndex, isFinalLink, fragmentIndex, isFinal, orderContext);
            box.set(packageOrderData(), order);
        }
    }

    public record CategoryRenderData(int x, int y, int itemsX, int itemsY, int categoryY, int rowHeight, int colWidth, int cols, List<StockKeeperRequestScreen.CategoryEntry> categories, float currentScroll, int windowHeight, Couple<Integer> hoveredSlot, int categoryIndex, PoseStack ms, Font font) {}
    public record OrderRenderData(int x, int y, int itemsX, int itemsY, int rowHeight, int colWidth, int orderY, int cols, Couple<Integer> hoveredSlot, PoseStack ms) {}
    public record SlotClickedData(int cols, boolean lmb, boolean rmb, boolean orderClicked) {}

    public abstract BlockCapability<H, @Nullable Direction> getBlockCapability();

    public final SimpleRegistry<Block, GenericUnpackingHandler<V>> registry = SimpleRegistry.create();

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    private V insertItemStacked(H inventory, V stack, boolean simulate) {
        var valueHandler = valueHandler();
        var storageHandler = storageHandler();
        if (inventory != null && !valueHandler.isEmpty(stack)) {
            if (!valueHandler.isStackable(stack)) {
                return insertItem(inventory, stack, simulate);
            } else {
                int sizeInventory = storageHandler.getSlots(inventory);
                for(int i = 0; i < sizeInventory; ++i) {
                    V slot = storageHandler.getStackInSlot(inventory, i);
                    if(valueHandler.equalsIgnoreCount(slot, stack)) {
                        stack = storageHandler.insertItem(inventory, i, stack, simulate);
                        if (valueHandler.isEmpty(stack)) {
                            break;
                        }
                    }
                }

                if (!valueHandler.isEmpty(stack)) {
                    for(int i = 0; i < sizeInventory; ++i) {
                        if (valueHandler.isEmpty(storageHandler.getStackInSlot(inventory, i))) {
                            stack = storageHandler.insertItem(inventory, i, stack, simulate);
                            if (valueHandler.isEmpty(stack)) {
                                break;
                            }
                        }
                    }
                }

                return stack;
            }
        } else {
            return stack;
        }
    }

    private V insertItem(H dest, V stack, boolean simulate) {
        var valueHandler = valueHandler();
        var storageHandler = storageHandler();
        if (dest != null && !valueHandler.isEmpty(stack)) {
            for(int i = 0; i < storageHandler.getSlots(dest); ++i) {
                stack = storageHandler.insertItem(dest, i, stack, simulate);
                if (valueHandler.isEmpty(stack)) {
                    return valueHandler.empty();
                }
            }

            return stack;
        } else {
            return stack;
        }
    }

    public GenericUnpackingHandler<V> defaultUnpackProcedure = (level, pos, state, side, items, orderContext, simulate) -> {
        BlockEntity targetBE = level.getBlockEntity(pos);
        if (targetBE == null)
            return false;

        H targetInv = level.getCapability(getBlockCapability(), pos, state, targetBE, null);
        if (targetInv == null)
            return false;

        if (!simulate) {
            /*
             * Some mods do not support slot-by-slot precision during simulate = false.
             * Faulty interactions may lead to voiding of items, but the simulate pass should
             * already have correctly identified there to be enough space for everything.
             */
            for (V itemStack : items)
                insertItemStacked(targetInv, valueHandler().copy(itemStack), false);
            return true;
        }

        var storageHandler = storageHandler();
        var valueHandler = valueHandler();

        for (int slot = 0; slot < storageHandler.getSlots(targetInv); slot++) {
            V itemInSlot = storageHandler.getStackInSlot(targetInv, slot);

            for (int boxSlot = 0; boxSlot < items.size(); boxSlot++) {
                V toInsert = items.get(boxSlot);
                if(valueHandler.isEmpty(toInsert))
                    continue;
                if(valueHandler.getCount(storageHandler.setInSlot(targetInv, slot, toInsert, true)) == valueHandler.getCount(toInsert))
                    continue;

                if (valueHandler.isEmpty(itemInSlot)) {
                    int maxStackSize = storageHandler.maxCountPerSlot();
                    if (maxStackSize < valueHandler.getCount(toInsert)) {
                        valueHandler.shrink(toInsert, maxStackSize);
                        toInsert = valueHandler.copyWithCount(toInsert, maxStackSize);
                    } else
                        items.set(boxSlot, valueHandler.empty());

                    itemInSlot = toInsert;
                    storageHandler.insertItem(targetInv, slot, toInsert, simulate);
                    continue;
                }

                if (!valueHandler.equalsIgnoreCount(toInsert, itemInSlot))
                    continue;

                //TODO: Implement slot limit
                int added = valueHandler.getCount(toInsert) - valueHandler.getCount(storageHandler.insertItem(targetInv, slot, toInsert, simulate));

                items.set(boxSlot, valueHandler.copyWithCount(toInsert, valueHandler.getCount(toInsert) - added));
            }
        }

        for (V stack : items) {
            if (!valueHandler.isEmpty(stack)) {
                // something failed to be inserted
                return false;
            }
        }

        return true;
    };
}
