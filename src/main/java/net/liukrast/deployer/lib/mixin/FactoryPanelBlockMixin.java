package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.block.IBE;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.PanelBlockItem;
import net.liukrast.deployer.lib.mixinExtensions.FPBEExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(FactoryPanelBlock.class)
public abstract class FactoryPanelBlockMixin extends Block implements IBE<FactoryPanelBlockEntity> {


    public FactoryPanelBlockMixin(Properties properties) {
        super(properties);
    }

    @Shadow
    public static FactoryPanelBlock.PanelSlot getTargetedSlot(BlockPos pos, BlockState blockState, Vec3 clickLocation) {
        throw new AssertionError("Mixin injection failed");
    }

    /* OVERRIDES */
    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity be) {
            var location = target.getLocation();
            FactoryPanelBlock.PanelSlot slot = getTargetedSlot(pos, state, location);
            if(be.panels.get(slot) instanceof AbstractPanelBehaviour ab && ab.active) {
                return ab.getItem().getDefaultInstance();
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        if(params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof FactoryPanelBlockEntity fpbe) {
            List<ItemStack> out = new ArrayList<>();
            for(var panel : fpbe.panels.values()) {
                if(!panel.active) continue;
                if(panel instanceof AbstractPanelBehaviour ab) out.addAll(ab.getItemDrops());
                else out.add(AllBlocks.FACTORY_GAUGE.asStack());
            }
            return out.isEmpty() ? ((FPBEExtension)fpbe).deployer$getExtraDrops() : out;
        }
        return List.of();
    }

    /* <INIT> */
    /* CREATE BLOCK STATE DEFINITION */
    /* CAN SURVIVE */
    /* CAN ATTACH LENIENT */
    /* GET STATE FOR PLACEMENT */
    @ModifyExpressionValue(method = "getStateForPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z"))
    private boolean getStateForPlacement(boolean original, @Local(argsOnly = true) BlockPlaceContext context, @Local(name = "fpbe") FactoryPanelBlockEntity blockEntity, @Local(name = "blockState") BlockState state, @Local(name = "location") Vec3 location) {
        if(original) return true;
        if(!(context.getItemInHand().getItem() instanceof PanelBlockItem panelBlockItem)) return false;
        panelBlockItem.applyExtraPlacementData(context, blockEntity, getTargetedSlot(context.getClickedPos(), state, location));
        return true;
    }
    /* ON SNEAK WRENCHED */
    @ModifyArg(method = "lambda$onSneakWrenched$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;)V"))
    private static ItemStack asStack(ItemStack original, @Local(name = "behaviour") FactoryPanelBehaviour behaviour) {
        if(!(behaviour instanceof AbstractPanelBehaviour ab)) return original;
        var extra = ab.getItemDrops();
        var drop = ab.getItem().getDefaultInstance();
        int size = extra.size();
        extra = extra.stream().filter(i -> !ItemStack.isSameItemSameComponents(i, drop)).toList();
        for(ItemStack stack : extra)
            popResource(ab.getWorld(), ab.getPos(), stack);
        if(size != extra.size()) {
            return drop;
        }
        return ItemStack.EMPTY;
    }

    @SuppressWarnings("SameReturnValue")
    @Definition(id = "world", local = @Local(type = Level.class, name = "world"))
    @Definition(id = "ServerLevel", type = ServerLevel.class)
    @Expression("world instanceof ServerLevel")
    @ModifyExpressionValue(method = "onSneakWrenched", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean onSneakWrenched(boolean original, @Local(name = "slot") FactoryPanelBlock.PanelSlot slot, @Local(name = "world") Level world, @Local(name = "pos") BlockPos pos) {
        if(original) return true;
        onBlockEntityUse(world, pos, be -> {
            FactoryPanelBehaviour behaviour = be.panels.get(slot);
            if(behaviour == null || !behaviour.isActive())
                return null;
            if(!be.removePanel(slot))
                return null;
            return null;
        });

        return true;
    }
    /* SET PLACED BY */
    @WrapWithCondition(method = "setPlacedBy", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlock;withBlockEntityDo(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Ljava/util/function/Consumer;)V"))
    private boolean withBlockEntityDo(FactoryPanelBlock instance, BlockGetter blockGetter, BlockPos pos, Consumer<FactoryPanelBlockEntity> consumer, @Local(argsOnly = true) ItemStack stack, @Local(name = "initialSlot") FactoryPanelBlock.PanelSlot initialSlot) {
        if(!(stack.getItem() instanceof PanelBlockItem panelBlockItem)) return true;
        withBlockEntityDo(blockGetter, pos, blockEntity -> panelBlockItem.applyToSlot(blockEntity, initialSlot, LogisticallyLinkedBlockItem.networkFromStack(FactoryPanelBlockItem.fixCtrlCopiedStack(stack))));
        return false;
    }
    /* USE ITEM ON */
    @Inject(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"), cancellable = true)
    private void useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if(!(stack.getItem() instanceof PanelBlockItem panelBlockItem)) return;
        if(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity panel && panel.restocker) {
            AllSoundEvents.DENY.playOnServer(level, pos);
            //player.displayClientMessage(Component.translatable("extra_gauges.panel.custom_panel_on_restocker"), true);
            cir.setReturnValue(ItemInteractionResult.CONSUME);
            cir.cancel();
            return;
        }
        var error = panelBlockItem.isReadyForPlacement(stack, level, pos, player);
        if(error != null) {
            AllSoundEvents.DENY.playOnServer(level, pos);
            player.displayClientMessage(error, true);
            cir.setReturnValue(ItemInteractionResult.CONSUME);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean useItemOn(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || stack.getItem() instanceof PanelBlockItem;
    }

    @ModifyExpressionValue(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockItem;isTuned(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean useItemOn$$1(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || stack.getItem() instanceof PanelBlockItem;
    }

    @WrapOperation(method = "lambda$useItemOn$2", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;addPanel(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlock$PanelSlot;Ljava/util/UUID;)Z"))
    private boolean lambda$useItemOn$2(FactoryPanelBlockEntity instance, FactoryPanelBlock.PanelSlot panelSlot, UUID slot, Operation<Boolean> original, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) FactoryPanelBlockEntity blockEntity, @Local(argsOnly = true) FactoryPanelBlock.PanelSlot newSlot) {
        if(stack.getItem() instanceof PanelBlockItem blockItem) return blockItem.applyToSlot(blockEntity, newSlot, LogisticallyLinkedBlockItem.networkFromStack(FactoryPanelBlockItem.fixCtrlCopiedStack(stack)));
        return original.call(instance, panelSlot, slot);
    }

    @ModifyArg(method = "lambda$useItemOn$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private Component lambda$useItemOn$2(Component message, @Local(argsOnly = true) ItemStack stack) {
        if(!(stack.getItem() instanceof PanelBlockItem blockItem)) return message;
        return blockItem.getPlacedMessage();
    }
    /* ON DESTROYED BY PLAYER */
    /* TRY DESTROY SUB PANEL FIRST */
    @Inject(method = "lambda$tryDestroySubPanelFirst$3", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;activePanels()I"))
    private static void lambda$tryDestroySubPanelFirst$3(
            FactoryPanelBlock.PanelSlot destroyedSlot,
            Player player,
            Level level,
            BlockPos pos,
            FactoryPanelBlockEntity fpbe,
            CallbackInfoReturnable<InteractionResult> cir,
            @Share("item_stack") LocalRef<List<ItemStack>> localStack
    ) {
        var behaviour = fpbe.panels.get(destroyedSlot);
        if(behaviour instanceof AbstractPanelBehaviour panelBehaviour) localStack.set(panelBehaviour.getItemDrops());
    }

    @ModifyArg(method = "lambda$tryDestroySubPanelFirst$3", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlock;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private static ItemStack lambda$tryDestroySubPanelFirst$3(
            ItemStack stack,
            @Share("item_stack") LocalRef<List<ItemStack>> localStack,
            @Local(argsOnly = true) FactoryPanelBlockEntity be
    ) {
        if(localStack.get() == null) return stack;
        var drop = localStack.get();
        if(drop.isEmpty()) return ItemStack.EMPTY;
        assert be.getLevel() != null;
        for(int i = 1; i < drop.size(); i++) popResource(be.getLevel(), be.getBlockPos(), drop.get(i));
        return drop.getFirst();
    }
    /* IS SIGNAL SOURCE */
    /* GET SIGNAL */
    /* GET DIRECT SIGNAL */
    /* CAN BE REPLACED */
    @ModifyExpressionValue(method = "canBeReplaced", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean canBeReplaced(boolean original, @Local(argsOnly = true) BlockPlaceContext context) {
        return original || context.getItemInHand().getItem() instanceof PanelBlockItem;
    }

    @WrapOperation(method = "canBeReplaced", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;isActive()Z"))
    private boolean canBeReplaced(FactoryPanelBehaviour instance, Operation<Boolean> original) {
        if(instance == null) return false;
        return original.call(instance);
    }
    /* GET COLLISION SHAPE */
    /* GET SHAPE */
    /* UPDATE SHAPE */
    /* GET FLUID STATE */
    /* CONNECTED DIRECTION */
    /* GET TARGETED SLOT */
    /* GET BLOCK ENTITY CLASS */
    /* GET BLOCK ENTITY TYPE */
    /* GET X ROT */
    /* GET Y ROT */
    /* GET REQUIRED ITEMS */
    /* CODEC */










}
