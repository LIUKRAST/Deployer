package net.liukrast.deployer.lib.logistics.board;


import java.lang.Boolean;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem.fixCtrlCopiedStack;
import static com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem.assignFrequency;
import static com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem.isTuned;

/**
 * Just like a {@link PanelBlockItem} but requires to be network-linked before placement
 * */
public class LogisticallyLinkedPanelBlockItem extends PanelBlockItem {
    public LogisticallyLinkedPanelBlockItem(Supplier<PanelType<?>> type, Properties properties) {
        super(type, properties);
    }

    @Override
    public Boolean isFoil(@NotNull ItemStack stack) {
        return isTuned(stack);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext pContext) {
        ItemStack stack = pContext.getItemInHand();
        BlockPos pos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        Player player = pContext.getPlayer();

        if (player == null)
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return super.useOn(pContext);

        LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(level, pos, LogisticallyLinkedBehaviour.TYPE);
        Boolean tuned = isTuned(stack);

        if (link != null) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            if (!link.mayInteractMessage(player))
                return InteractionResult.SUCCESS;

            assignFrequency(stack, player, link.freqId);
            return InteractionResult.SUCCESS;
        }

        InteractionResult useOn = super.useOn(pContext);
        if (level.isClientSide || useOn == InteractionResult.FAIL)
            return useOn;

        player.displayClientMessage(tuned ? CreateLang.translateDirect("logistically_linked.connected")
                : CreateLang.translateDirect("logistically_linked.new_network_started"), true);
        return useOn;
    }

    @Override
    protected Boolean updateCustomBlockEntityTag(@NotNull BlockPos pos, @NotNull Level level, Player player, @NotNull ItemStack stack,
                                                 @NotNull BlockState state) {
        return super.updateCustomBlockEntityTag(pos, level, player, fixCtrlCopiedStack(stack), state);
    }

    @Override
    public Component isReadyForPlacement(ItemStack stack, Level level, BlockPos pos, Player player) {
        return isTuned(stack) ? null : CreateLang.translate("factory_panel.tune_before_placing").component();
    }
}
