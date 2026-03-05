package net.liukrast.deployer.lib.logistics.packager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.*;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.liukrast.deployer.lib.logistics.GenericPackageOrderData;
import net.liukrast.deployer.lib.logistics.OrderStockTypeData;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixin.PackagerBlockEntityAccessor;
import net.liukrast.deployer.lib.mixinExtensions.LLBExtension;
import net.liukrast.deployer.lib.mixinExtensions.RPQExtension;
import net.liukrast.deployer.lib.mixinExtensions.VITBExtension;
import net.liukrast.deployer.lib.registry.DeployerDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Generalization of {@link PackagerBlockEntity} to handler packages containing any other content,
 * making it able to pack/unpack non-item related packages 
 * */
public abstract class AbstractPackagerBlockEntity<K,V,H> extends PackagerBlockEntity {
    public CapManipulationBehaviourBase<H, ? extends CapManipulationBehaviourBase<?,?>> targetInventory;
    private AbstractInventorySummary<K,V> availableItems;

    /**
     * Main block entity constructor.
     * @param typeIn be type
     * @param pos BlockPosition
     * @param state BlockState
     * */
    public AbstractPackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    /**
     * @return Whether the packager can connect to another block entity or not
     * @param target The block entity involved
     * */
    public boolean supportsBlockEntity(BlockEntity target) {
        return ((PackagerBlockEntityAccessor)this).invokeSupportsBlockEntity(target);
    }

    /**
     * @return The capability manipulation behavior for your stock type
     * */
    protected abstract CapManipulationBehaviourBase<H, ? extends CapManipulationBehaviourBase<?,?>> createTargetInventory();

    /**
     * @return The {@link StockInventoryType} for this packager
     * */
    public abstract StockInventoryType<K,V,H> getStockType();

    /**
     * Defines the tray model for your packager.
     * @param original The default packager hatch model based on whether it's open or not
     * @param isHatchOpen Whether the hatch is open or not
     * @return The hatch model you want to use
     * Also see: {@link AbstractPackagerBlock#getTrayModel(BlockState, PartialModel)}
     * */
    public PartialModel getHatchModel(boolean isHatchOpen, PartialModel original) {
        return original;
    }

    /**
     * Registers behaviors to the block entity
     * */
    @ApiStatus.OverrideOnly
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(targetInventory = createTargetInventory());
    }

    /**
     * <strong>Do not invoke this, use {@link AbstractPackagerBlockEntity#getAvailableStacks()} instead</strong>
     * */
    @ApiStatus.Internal
    @Deprecated
    @Override
    public InventorySummary getAvailableItems() {
        throw new IllegalCallerException("This function should not be invoked on abstract packagers");
    }

    /**
     * <strong>Do not invoke this, use {@link AbstractPackagerBlockEntity#attemptToSendSpecial(List)} instead</strong>
     * */
    @ApiStatus.Internal
    @Deprecated
    @Override
    public void attemptToSend(List<PackagingRequest> queuedRequests) {
        throw new IllegalCallerException("This function should not be invoked on abstract packagers");
    }

    /**
     * Triggers a check of the available stock in the connected inventory.
     * <p>
     * This method forces an immediate refresh of the cached inventory state by calling
     * {@link #getAvailableStacks()}, which scans the target inventory and updates the
     * internal {@link AbstractInventorySummary}.
     * <p>
     * This is typically called after packaging operations or when the inventory state
     * may have changed and needs to be re-evaluated (e.g., after extracting items for
     * a package, or when new items arrive in the connected inventory).
     */
    @Override
    public void triggerStockCheck() {
        getAvailableStacks();
    }

    /**
     * @return A summary of all stacks inside this packager
     * */
    @SuppressWarnings("UnusedReturnValue")
    public AbstractInventorySummary<K,V> getAvailableStacks() {
        if (availableItems != null && ((VITBExtension)((PackagerBlockEntityAccessor)this).getInvVersionTracker()).deployer$stillWaiting(targetInventory.getInventory()))
            return availableItems;
        var type = getStockType();
        var handler = type.storageHandler();

        AbstractInventorySummary<K,V> availableItems = type.networkHandler().createSummary();

        H targetInv = targetInventory.getInventory();
        if (targetInv == null || targetInv instanceof PackagerItemHandler) {
            this.availableItems = availableItems;
            return availableItems;
        }
        for(int slot = 0; slot < handler.getSlots(targetInv); slot++) {
            availableItems.add(handler.getStackInSlot(targetInv, slot));
        }

        ((VITBExtension)((PackagerBlockEntityAccessor)this).getInvVersionTracker()).deployer$awaitNewVersion(targetInventory.getInventory());
        submitNewArrivals(type, this.availableItems, availableItems);
        this.availableItems = availableItems;
        return availableItems;
    }

    private void submitNewArrivals(StockInventoryType<K,V,H> type, AbstractInventorySummary<K,V> before, AbstractInventorySummary<K,V> after) {
        if (before == null || after.isEmpty())
            return;
        assert level != null;
        var handler = type.valueHandler();

        Set<RequestPromiseQueue> promiseQueues = new HashSet<>();

        for (Direction d : Iterate.directions) {

            if (!level.isLoaded(worldPosition.relative(d)))
                continue;

            BlockState adjacentState = level.getBlockState(worldPosition.relative(d));
            if (AllBlocks.FACTORY_GAUGE.has(adjacentState)) {
                if (FactoryPanelBlock.connectedDirection(adjacentState) != d)
                    continue;
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof FactoryPanelBlockEntity fpbe))
                    continue;
                if (!fpbe.restocker)
                    continue;
                for (FactoryPanelBehaviour behaviour : fpbe.panels.values()) {
                    if (!behaviour.isActive())
                        continue;
                    promiseQueues.add(behaviour.restockerPromises);
                }
            }

            if (AllBlocks.STOCK_LINK.has(adjacentState)) {
                if (PackagerLinkBlock.getConnectedDirection(adjacentState) != d)
                    continue;
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof PackagerLinkBlockEntity plbe))
                    continue;
                UUID freqId = plbe.behaviour.freqId;
                if (!Create.LOGISTICS.hasQueuedPromises(freqId))
                    continue;
                promiseQueues.add(Create.LOGISTICS.getQueuedPromises(freqId));
            }
        }

        if (promiseQueues.isEmpty())
            return;

        // Instead of using negative values, we use another approach
        for (V entry : after.getStacks()) {
            int beforeCount = before.getCountOf(entry);
            int afterCount  = handler.getCount(entry);

            if (afterCount > beforeCount) {
                int entered = afterCount - beforeCount;

                for (RequestPromiseQueue queue : promiseQueues) {
                    ((RPQExtension) queue)
                            .deployer$genericEnteredSystem(type, entry, entered);
                }
            }
        }
    }

    /**
     * Tries to package a request from the stock inventory
     * @param queuedRequests A list of requests from the storage
     * */
    public void attemptToSendSpecial(@Nullable List<GenericPackagingRequest<V>> queuedRequests) {
        attemptToSendSpecial(queuedRequests, 0, true);
    }

    /**
     * Same as {@link AbstractPackagerBlockEntity#attemptToSendSpecial(List)}
     * @param queuedRequests A list of requests from the storage
     * @param index The package type index,
     *              learn more at {@link net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager#broadcastAllPackageRequest(PackageOrderWithCrafts, UUID, LogisticallyLinkedBehaviour.RequestType, Map, String)}
     * @param isFinal Whether this is the last package type index, learn more at {@link net.liukrast.deployer.lib.logistics.packagerLink.LogisticsGenericManager#broadcastAllPackageRequest(PackageOrderWithCrafts, UUID, LogisticallyLinkedBehaviour.RequestType, Map, String)}
     * */
    public void attemptToSendSpecial(@Nullable List<GenericPackagingRequest<V>> queuedRequests, int index, boolean isFinal) {
        if (queuedRequests == null && (!heldBox.isEmpty() || animationTicks != 0 || buttonCooldown > 0))
            return;

        H targetInv = targetInventory.getInventory();
        if (targetInv == null || targetInv instanceof PackagerItemHandler)
            return;
        var type = getStockType();

        boolean anyItemPresent = false;
        H extractedItems = type.storageHandler().create(type.storageHandler().getMaxPackageSlots());
        ItemStack extractedPackageItem = ItemStack.EMPTY;
        GenericPackagingRequest<V> nextRequest = null;
        String fixedAddress = null;
        int fixedOrderId = 0;

        // Data written to packages for defrags
        int linkIndexInOrder = 0;
        boolean finalLinkInOrder = false;
        int packageIndexAtLink = 0;
        boolean finalPackageAtLink = false;
        GenericOrderContained<V> orderContext = null;
        boolean requestQueue = queuedRequests != null;

        if (requestQueue && !queuedRequests.isEmpty()) {
            nextRequest = queuedRequests.getFirst();
            fixedAddress = nextRequest.address();
            fixedOrderId = nextRequest.orderId();
            linkIndexInOrder = nextRequest.linkIndex();
            finalLinkInOrder = nextRequest.finalLink()
                    .booleanValue();
            packageIndexAtLink = nextRequest.packageCounter()
                    .getAndIncrement();
            orderContext = nextRequest.context();
        }

        //From here it's black magic
        var handler = type.storageHandler();
        var valueHandler = type.valueHandler();
        Outer:
        for (int i = 0; i < handler.getMaxPackageSlots(); i++) {
            boolean continuePacking = true;

            while (continuePacking) {
                continuePacking = false;
                for (int slot = 0; slot < handler.getSlots(targetInv); slot++) {
                    //noinspection DataFlowIssue
                    int initialCount = requestQueue ? Math.min(handler.maxCountPerSlot(), nextRequest.getCount()) : handler.maxCountPerSlot();

                    V extracted = handler.extract(targetInv, valueHandler.copyWithCount(handler.getStackInSlot(targetInv, slot), initialCount), true);
                    if (valueHandler.isEmpty(extracted))
                        continue;
                    if(requestQueue && !valueHandler.equalsIgnoreCount(extracted, nextRequest.item()))
                        continue;

                    boolean bulky = !handler.isBulky(valueHandler.fromValue(extracted));
                    if (bulky && anyItemPresent)
                        continue;

                    anyItemPresent = true;
                    int leftovers = handler.fill(extractedItems, valueHandler.copyWithCount(extracted, valueHandler.getCount(extracted)), false);
                    int transferred = valueHandler.getCount(extracted) -leftovers;
                    handler.extract(targetInv, valueHandler.copyWithCount(handler.getStackInSlot(targetInv, slot), transferred), false);

                    if (!requestQueue) {
                        if (bulky)
                            break Outer;
                        continue;
                    }

                    nextRequest.subtract(transferred);

                    if (!nextRequest.isEmpty()) {
                        if (bulky)
                            break Outer;
                        continue;
                    }

                    finalPackageAtLink = true;
                    queuedRequests.removeFirst();
                    if (queuedRequests.isEmpty())
                        break Outer;
                    int previousCount = nextRequest.packageCounter()
                            .intValue();
                    nextRequest = queuedRequests.getFirst();
                    assert fixedAddress != null;
                    if (!fixedAddress.equals(nextRequest.address()))
                        break Outer;
                    if (fixedOrderId != nextRequest.orderId())
                        break Outer;

                    nextRequest.packageCounter()
                            .setValue(previousCount);
                    finalPackageAtLink = false;
                    continuePacking = true;
                    if (nextRequest.context() != null)
                        orderContext = nextRequest.context();

                    if (bulky)
                        break Outer;
                    break;
                }
            }
        }

        if (!anyItemPresent) {
            if (nextRequest != null)
                queuedRequests.removeFirst();
            return;
        }

        ItemStack createdBox =
                extractedPackageItem.isEmpty() ? type.packageHandler().containing(extractedItems) : extractedPackageItem.copy();
        PackageItem.clearAddress(createdBox);

        if (fixedAddress != null)
            PackageItem.addAddress(createdBox, fixedAddress);
        if (requestQueue) {
            GenericPackageItem.setOrder(type, createdBox, fixedOrderId, linkIndexInOrder, finalLinkInOrder, packageIndexAtLink,
                    finalPackageAtLink, orderContext);
            createdBox.set(DeployerDataComponents.ORDER_STOCK_TYPE_DATA, new OrderStockTypeData(index, isFinal));
        }
        if (!requestQueue && !signBasedAddress.isBlank())
            PackageItem.addAddress(createdBox, signBasedAddress);

        BlockPos linkPos = ((PackagerBlockEntityAccessor)this).invokeGetLinkPos();
        assert level != null;
        if (extractedPackageItem.isEmpty() && linkPos != null
                && level.getBlockEntity(linkPos) instanceof PackagerLinkBlockEntity pLBE)
            ((LLBExtension)pLBE.behaviour).deployer$deductFromAccurateSummary(type, extractedItems);

        if (!heldBox.isEmpty() || animationTicks != 0) {
            queuedExitingPackages.add(new BigItemStack(createdBox, 1));
            return;
        }

        heldBox = createdBox;
        animationInward = false;
        animationTicks = CYCLE;

        ((PackagerBlockEntityAccessor)this).getAdvancement().awardPlayer(AllAdvancements.PACKAGER);
        triggerStockCheck();
        notifyUpdate();
    }

    /**
     * <strong>Do not invoke this, use {@link AbstractPackagerBlockEntity#isTargetingSameContainer(IdentifiedContainer)} instead</strong>
     * */
    @Deprecated
    @ApiStatus.Internal
    @Override
    public boolean isTargetingSameInventory(@Nullable IdentifiedInventory inventory) {
        throw new IllegalCallerException("Should not be invoked in FluidPackagerBlockEntity");
    }

    /**
     * @return whether this packager is matching the same inventory as specified in the parameter
     * @param inventory The inventory to check
     * */
    public boolean isTargetingSameContainer(@Nullable IdentifiedContainer<H> inventory) {
        if (inventory == null)
            return false;

        H targetHandler = this.targetInventory.getInventory();
        if (targetHandler == null)
            return false;

        if (inventory.identifier() != null) {
            BlockFace face = this.targetInventory.getTarget().getOpposite();
            return inventory.identifier().contains(face);
        } else {
            return isSameInventoryFallback(targetHandler, inventory.handler());
        }
    }

    private boolean isSameInventoryFallback(H first, H second) {
        if (first == second)
            return true;

        var sh = getStockType().storageHandler();
        var vh = getStockType().valueHandler();

        // If a contained ItemStack instance is the same, we can be pretty sure these
        // inventories are the same (works for compound inventories)
        //TODO: We cannot be sure of this for other stack types
        for (int i = 0; i < sh.getSlots(second); i++) {
            V stackInSlot = sh.getStackInSlot(second, i);
            if (vh.isEmpty(stackInSlot))
                continue;
            for (int j = 0; j < sh.getSlots(first); j++)
                if (stackInSlot == sh.getStackInSlot(first, j))
                    return true;
            break;
        }

        return false;
    }

    private List<V> getNonEmptyStacks(H handler) {
        var storage = getStockType().storageHandler();
        var value = getStockType().valueHandler();
        List<V> stacks = new ArrayList<>();
        for (int i = 0; i < storage.getSlots(handler); i++) {
            V stack = storage.getStackInSlot(handler, i);
            if (!value.isEmpty(stack)) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private GenericOrderContained<V> getOrderContext(ItemStack box) {
        var type = getStockType().packageHandler();
        var packageOrderData = type.packageOrderData();
        var packageOrderContext = type.packageOrderContext();
        if (box.has(packageOrderData)) {
            GenericPackageOrderData<V> data = box.get(packageOrderData);
            assert data != null; //Might not be correct
            return data.orderContext();
        } else if (box.has(packageOrderContext)) {
            return box.get(packageOrderContext);
        } else {
            return null;
        }
    }

    /**
     * Unwraps a box and puts it inside the packager storage
     * @return Whether the unwrapping went successfully
     * @param box the box to unwrap
     * @param simulate Whether you actually want to perform the action or you only want to check if the action is possible
     * */
    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        /* We avoid unpacking boxes that are not for this packager.
        A fluid packager cannot pack/unpack normal packages unless it's composite */
        if(!(box.getItem() instanceof GenericPackageItem generic)) {
            heldBox = box;
            animationInward = false;
            animationTicks = 20;
            notifyUpdate();
            return false;
        }
        var type = getStockType();
        if(generic.getType() != type) {
            heldBox = box;
            animationInward = false;
            animationTicks = 20;
            notifyUpdate();
            return false;
        }
        if (animationTicks > 0)
            return false;

        Objects.requireNonNull(this.level);

        var ph = type.packageHandler();

        H contents = ph.getContents(box);
        List<V> items = getNonEmptyStacks(contents);
        if (items.isEmpty())
            return true;

        GenericOrderContained<V> orderContext = getOrderContext(box);
        Direction facing = getBlockState().getOptionalValue(PackagerBlock.FACING).orElse(Direction.UP);
        BlockPos target = worldPosition.relative(facing.getOpposite());
        BlockState targetState = level.getBlockState(target);

        GenericUnpackingHandler<V> handler = type.registry.get(targetState);
        GenericUnpackingHandler<V> toUse = handler != null ? handler : type.defaultUnpackProcedure;
        // note: handler may modify the passed items
        boolean unpacked = toUse.unpack(level, target, targetState, facing, items, orderContext, simulate);

        if (unpacked && !simulate) {
            previouslyUnwrapped = box;
            animationInward = true;
            animationTicks = CYCLE;
            notifyUpdate();
        }

        return unpacked;
    }
}
