package net.liukrast.deployer.lib.logistics.packager;

import com.mojang.serialization.Codec;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import it.unimi.dsi.fastutil.Hash;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.packagerLink.GenericRequestPromise;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrder;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    public abstract static class IValueHandler<K,V,H> {
        private final Codec<V> codec;
        private final Codec<GenericOrder<V>> orderCodec;
        private final Codec<GenericOrderContained<V>> orderContainedCodec;
        private final StreamCodec<? super RegistryFriendlyByteBuf, V> streamCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, GenericOrder<V>> orderStreamCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, GenericOrderContained<V>> orderContainedStreamCodec;


        public IValueHandler(
                Codec<V> codec,
                StreamCodec<? super RegistryFriendlyByteBuf, V> streamCodec
        ) {
            this(codec,
                    GenericOrder::simpleCodec,
                    GenericOrderContained::fromOrderCodec,
                    streamCodec,
                    GenericOrder::simpleStreamCodec,
                    GenericOrderContained::fromOrderStreamCodec
            );
        }


        public IValueHandler(
                Codec<V> codec,
                BiFunction<Codec<V>, Hash.Strategy<? super V>, Codec<GenericOrder<V>>> orderFactory,
                TriFunction<Codec<GenericOrder<V>>, Codec<V>, Hash.Strategy<? super V>, Codec<GenericOrderContained<V>>> orderContainedFactory,
                StreamCodec<? super RegistryFriendlyByteBuf, V> streamCodec,
                BiFunction<StreamCodec<? super RegistryFriendlyByteBuf, V>, Hash.Strategy<? super V>, StreamCodec<RegistryFriendlyByteBuf, GenericOrder<V>>> orderStreamFactory,
                Function<StreamCodec<RegistryFriendlyByteBuf, GenericOrder<V>>, StreamCodec<RegistryFriendlyByteBuf, GenericOrderContained<V>>> orderContainedStreamFactory
        ) {
            this.codec = codec;
            this.streamCodec = streamCodec;

            this.orderCodec = orderFactory.apply(codec, hashStrategy());
            this.orderContainedCodec = orderContainedFactory.apply(orderCodec, codec, hashStrategy());
            this.orderStreamCodec = orderStreamFactory.apply(streamCodec, hashStrategy());
            this.orderContainedStreamCodec = orderContainedStreamFactory.apply(orderStreamCodec);
        }

        public Codec<V> codec() {
            return codec;
        }
        public Codec<GenericOrder<V>> orderCodec() {
            return orderCodec;
        }
        public Codec<GenericOrderContained<V>> orderContainedCodec() {
            return orderContainedCodec;
        }
        public StreamCodec<? super RegistryFriendlyByteBuf, V> streamCodec() {
            return streamCodec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, GenericOrder<V>> orderStreamCodec() {
            return orderStreamCodec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, GenericOrderContained<V>> orderContainedStreamCodec() {
            return orderContainedStreamCodec;
        }

        public GenericOrderContained<V> createContained(List<V> itemsToOrder) {
            boolean allEmpty = true;
            List<V> real = new ArrayList<>();
            for(V v : itemsToOrder) {
                if(isEmpty(v)) continue;
                allEmpty = false;
                real.add(v);
            }
            if(allEmpty) return GenericOrderContained.empty();
            return GenericOrderContained.simple(real, hashStrategy());
        }
        public abstract Hash.Strategy<? super V> hashStrategy();
        public abstract K fromValue(V key);
        public abstract boolean test(FilterItemStack filter, Level level, V value);
        public abstract int getCount(V value);
        public abstract void setCount(V value, int count);
        public abstract boolean isEmpty(V stack);
        public abstract V create(K key, int amount);
        public abstract void shrink(V stack, int amount);
        public abstract V copyWithCount(V stack, int amount);
        public abstract V copy(V stack);
        public abstract boolean isStackable(V stack);
        public abstract V empty();
    }

    public interface IStorageHandler<K,V,H> {
        int getSlots(H handler); // returns the slots/tanks for items/fluids
        V getStackInSlot(H handler, int slot);
        int maxCountPerSlot();
        V extract(H handler, V value, boolean simulate, AbstractPackagerBlockEntity<K,V,H> packager);
        int fill(H handler, V value, boolean simulate, AbstractPackagerBlockEntity<K,V,H> packager);
        V setInSlot(H handler, int slot, V value, boolean simulate);
        boolean isBulky(K key);
        H create(int i);
        int getMaxPackageSlots();
        V insertItem(H handler, int i, V stack, boolean simulate);
    }

    public interface INetworkHandler<K,V,H> {
        Codec<GenericRequestPromise<V>> requestCodec();
        AbstractInventorySummary<K, V> createSummary();
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
        default void setOrder(ItemStack box, int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, @Nullable GenericOrderContained<V> orderContext) {
            GenericPackageOrderData<V> order = new GenericPackageOrderData<>(orderId, linkIndex, isFinalLink, fragmentIndex, isFinal, orderContext);
            box.set(packageOrderData(), order);
        }
        void appendHoverText(ItemStack stack, Item.TooltipContext tooltipContext, List<Component> tooltipComponents,
                             TooltipFlag tooltipFlag, H handler);
    }

    public abstract BlockCapability<H, @Nullable Direction> getBlockCapability();

    public final SimpleRegistry<Block, GenericUnpackingHandler<K,V,H>> registry = SimpleRegistry.create();

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
                    if(valueHandler.hashStrategy().equals(slot, stack)) {
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

    public GenericUnpackingHandler<K,V,H> defaultUnpackProcedure = (level, pos, state, side, items, orderContext, simulate, packager) -> {
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

                if (!valueHandler.hashStrategy().equals(toInsert, itemInSlot))
                    continue;
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
