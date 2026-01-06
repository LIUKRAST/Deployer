package net.liukrast.deployer.lib.logistics.packager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.logistics.stockTicker.LogisticalStockGenericResponsePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.*;
import java.util.function.Predicate;

/**
 * Generalizes the concept of a {@link com.simibubi.create.content.logistics.packager.InventorySummary}.<br>
 * The entire Stock Inventory Type is a very complex topic,
 * so please follow it straight from the source {@link StockInventoryType}
 * @param <K> The summary keys. For ItemStacks, the key is Items
 * @param <V> The summary stacks. The default summary is on item stacks
 * */
@SuppressWarnings("unused")
public abstract class AbstractInventorySummary<K, V> {
    private final Map<K, List<V>> items = new IdentityHashMap<>();
    private List<V> stacksByCount;
    private int totalCount;
    public int contributingLinks;
    private final StockInventoryType<K,V,?> type;

    /**
     * @param type Constructs the inventory summary, given the stock inventory type source
     * */
    public AbstractInventorySummary(StockInventoryType<K,V,?> type) {
        this.type = type;
    }

    /**
     * @param stack The stack involved
     * @return The count of that stack
     * */
    public int getCount(V stack) {
        return type.valueHandler().getCount(stack);
    }
    /**
     * @param stack The stack involved
     * @return The key of that stack
     * */
    public K keyFrom(V stack) {
        return type.valueHandler().fromValue(stack);
    }

    /**
     * @param stack1 The first stack
     * @param stack2 The second stack
     * @return Whether the stacks are considered "equal" or not
     * */
    public boolean isSameKeySameComponents(V stack1, V stack2) {
        return type.valueHandler().equalsIgnoreCount(stack1, stack2);
    }

    /**
     * Sets the count of a stack
     * @param stack The stack involved
     * @param count The count to set
     * */
    public void setCount(V stack, int count) {
        type.valueHandler().setCount(stack, count);
    }

    /**
     * @param stack The stack to copy
     * @return Copies a stack
     * */
    public V copy(V stack) {
        return type.valueHandler().copy(stack);
    }

    /**
     * @return Whether the summary is empty or not
     * */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Adds all the items from another summary to this
     * @param summary The summary to add
     * */
    public void add(AbstractInventorySummary<K, V> summary) {
        summary.items.forEach((t,k) -> k.forEach(this::add));
    }

    /**
     * Adds a stack to the summary
     * @param stack The stack to add
     * */
    public void add(V stack) {
        add(stack, getCount(stack));
    }

    /**
     * @return The map of all objects in this summary
     * */
    public Map<K, List<V>> getItemMap() {
        return items;
    }

    /**
     * Adds all the stacks from a list
     * @param list The list of stacks to add
     * */
    public void addAllStacks(List<V> list) {
        for(V stack : list) add(stack, getCount(stack));
    }

    /**
     * @return A duplicate of this summary
     * */
    public AbstractInventorySummary<K, V> copy() {
        AbstractInventorySummary<K, V> summary = type.networkHandler().create();
        items.forEach((i, list) -> list.forEach(summary::add));
        return summary;
    }

    /**
     * Adds the specified amount of the given stack to this summary.
     * Increases the total item count, unless the limit has been reached.
     * If an entry with matching item and components already exists, its count is incremented.
     * If no matching entry is found, a new copy of the stack is added to the summary.
     *
     * @param stack
     *     The item stack to add.
     *
     * @param count
     *     The quantity to be added.
     */
    public void add(V stack, int count) {
        if(count == 0 || type.valueHandler().isEmpty(stack)) return;
        if(totalCount < BigItemStack.INF) totalCount+=count; //TODO: Allow users to set the limit for this summary

        List<V> stacks = items.computeIfAbsent(keyFrom(stack),$ -> Lists.newArrayList());
        for(V existing : stacks) {
            if(isSameKeySameComponents(existing, stack)) {
                if(getCount(existing) < BigItemStack.INF)
                    setCount(existing, getCount(existing) + count);
                return;
            }
        }

        stacks.add(copy(stack));
    }

    /**
     * Removes the matching stack from this summary.
     * Searches the entry bucket associated with the key of the provided stack.
     * If a stack with identical item and components is found, it is removed and its count is subtracted from the total.
     * Returns {@code true} if a matching stack was successfully erased.
     * Returns {@code false} if no matching entry existed.
     *
     * @param stack
     *     The stack to remove from the summary.
     *
     * @return
     *     {@code true} if the stack was found and removed.
     *     {@code false} otherwise.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean erase(V stack) {
        List<V> stacks = items.get(keyFrom(stack));
        if(stacks == null) return false;
        for(Iterator<V> iterator = stacks.iterator(); iterator.hasNext();) {
            V existing = iterator.next();
            if(!isSameKeySameComponents(existing, stack)) continue;
            totalCount -= getCount(existing);
            iterator.remove();
            return true;
        }
        return false;
    }

    /**
     * @param stack The stack to track
     * @return the number of stacks for this type inside the summary
     * */
    public int getCountOf(V stack) {
        List<V> list = items.get(keyFrom(stack));
        if(list == null) return 0;
        for(V entry : list) if(isSameKeySameComponents(entry, stack)) return getCount(entry);
        return 0;
    }

    /**
     * @param filter The predicate to check stacks
     * @return the number of stacks matching the predicate inside the summary
     * */
    public int getTotalOfMatching(Predicate<V> filter) {
        MutableInt sum = new MutableInt();
        items.forEach(($, list) -> {
            for(V stack : list) if(filter.test(stack)) sum.add(getCount(stack));
        });
        return sum.getValue();
    }

    /**
     * @return The list of stacks inside this summary
     * */
    public List<V> getStacks() {
        if(stacksByCount == null) {
            List<V> stacks = new ArrayList<>();
            items.forEach((i, list) -> stacks.addAll(list));
            return stacks;
        }
        return stacksByCount;
    }

    /**
     * @return The list of stacks inside the summary, sorted
     * */
    //TODO: Cant it just call #getStacks????
    public List<V> getStacksByCount() {
        if(stacksByCount == null) {
            stacksByCount = new ArrayList<>();
            items.forEach((i, list) -> stacksByCount.addAll(list));
            stacksByCount.sort(Comparator.comparingInt(this::getCount));
        }
        return stacksByCount;
    }

    /**
     * @return The total count of items inside the summary
     * */
    public int getTotalCount() {
        return totalCount;
    }

    public void divideAndSendTo(ServerPlayer player, BlockPos pos) {
        List<V> stacks = getStacksByCount();
        int remaining = stacks.size();

        List<V> currentList = null;

        if(stacks.isEmpty()) //TODO: Check later
            CatnipServices.NETWORK.sendToClient(player, new LogisticalStockGenericResponsePacket<>(true, pos, type, Collections.emptyList()));

        for(V entry : stacks) {
            if(currentList == null)
                currentList = new ArrayList<>(Math.min(100, remaining));

            currentList.add(entry);
            remaining--;

            if(remaining == 0)
                break;
            if(currentList.size() < 100)
                continue;
            //TODO: Check later
            CatnipServices.NETWORK.sendToClient(player, new LogisticalStockGenericResponsePacket<>(false, pos, type, currentList));
            currentList = null;
        }

        if (currentList != null)
            CatnipServices.NETWORK.sendToClient(player, new LogisticalStockGenericResponsePacket<>(true, pos, type, currentList));
    }
}
