package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(FactoryPanelConnectionHandler.class)
public class FactoryPanelConnectionHandlerMixin {
    @Shadow static FactoryPanelPosition connectingFrom;
    @Shadow static AABB connectingFromBox;

    @ModifyReturnValue(
            method = "checkForIssues(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;)Ljava/lang/String;",
            at = @At("RETURN")
    )
    private static String checkForIssues(
            String error,
            @Local(argsOnly = true, ordinal = 0) FactoryPanelBehaviour from,
            @Local(argsOnly = true, ordinal = 1) FactoryPanelBehaviour to
    ) {
        // Errors that shouldn't be ignored
        if("factory_panel.connection_aborted".equals(error)) return error;
        if("factory_panel.already_connected".equals(error)) return error;
        if("factory_panel.cannot_add_more_inputs".equals(error)) return error;
        if("factory_panel.same_orientation".equals(error)) return error;
        if("factory_panel.same_surface".equals(error)) return error;
        if("factory_panel.too_far_apart".equals(error)) return error;

        // Skip restocker mode normal gauges
        if(!(to instanceof AbstractPanelBehaviour) && "factory_panel.input_in_restock_mode".equals(error))
            return error;
        
        if("factory_panel.no_item".equals(error)) {
            if(!(from instanceof AbstractPanelBehaviour) && from.getFilter().isEmpty())
                return error;
            if(!(to instanceof AbstractPanelBehaviour) && to.getFilter().isEmpty())
                return error;
        }

        if(!(from instanceof AbstractPanelBehaviour) && !(to instanceof AbstractPanelBehaviour))
            return error;

        if(from instanceof AbstractPanelBehaviour apb) {
            var error1 = apb.canPoint(to);
            if(error1 != null) return error1;
        }
        if(to instanceof AbstractPanelBehaviour apb) return apb.canBePointed(from);
        return null;
    }

    @ModifyArg(method = "checkForIssues(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelSupportBehaviour;)Ljava/lang/String;", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElse(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static <T> T checkForIssues(T other, @Local(name = "state2") BlockState state2) {
        //noinspection unchecked
        return (T) PanelConnection.makeContext(state2);
    }

    @ModifyArg(
            method = "panelClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V", ordinal = 2)
    )
    private static Component panelClicked(Component original, @Local(ordinal = 0, argsOnly = true) FactoryPanelBehaviour from, @Local(name = "at") FactoryPanelBehaviour to) {
        if(from instanceof AbstractPanelBehaviour || to instanceof AbstractPanelBehaviour) return Component.translatable("deployer.panel.panels_connected").withStyle(ChatFormatting.GREEN);
        return original;
    }

    @Inject(
            method = "onRightClick",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;"),
            cancellable = true
    )
    private static void onRightClick(CallbackInfoReturnable<Boolean> cir, @Local(name = "mc") Minecraft mc, @Local(name = "bhr") BlockHitResult bhr) {
        assert mc.level != null;
        var fromState = mc.level.getBlockState(connectingFrom.pos());
        if(PanelConnection.makeContext(mc.level.getBlockState(bhr.getBlockPos())) == PanelConnection.makeContext(fromState) && DeployerRegistries.PANEL_CONNECTION
                .stream()
                .map(c -> c.getListener(mc.level.getBlockState(bhr.getBlockPos()).getBlock()))
                .anyMatch(Objects::nonNull)
        ) {
            FactoryPanelBehaviour at = FactoryPanelBehaviour.at(mc.level, connectingFrom);
            String checkForIssues = extra_gauges$checkForSpecialIssues(at, bhr.getBlockPos());
            if (checkForIssues != null) {
                assert mc.player != null;
                mc.player.displayClientMessage(CreateLang.translate(checkForIssues)
                        .style(ChatFormatting.RED)
                        .component(), true);
                connectingFrom = null;
                connectingFromBox = null;
                AllSoundEvents.DENY.playAt(mc.level, mc.player.blockPosition(), 1, 1, false);
                cir.setReturnValue(true);
                return;
            }
            FactoryPanelPosition bestPosition = null;
            double bestDistance = Double.POSITIVE_INFINITY;

            for (FactoryPanelBlock.PanelSlot slot : FactoryPanelBlock.PanelSlot.values()) {
                FactoryPanelPosition panelPosition = new FactoryPanelPosition(bhr.getBlockPos(), slot);
                FactoryPanelConnection connection = new FactoryPanelConnection(panelPosition, 1);
                Vec3 diff =
                        connection.calculatePathDiff(mc.level.getBlockState(connectingFrom.pos()), connectingFrom);
                if (bestDistance < diff.lengthSqr())
                    continue;
                bestDistance = diff.lengthSqr();
                bestPosition = panelPosition;
            }

            CatnipServices.NETWORK.sendToServer(new FactoryPanelConnectionPacket(bestPosition, connectingFrom, false));

            assert mc.player != null;
            mc.player.displayClientMessage(CreateLang
                    .translate("factory_panel.link_connected", mc.level.getBlockState(bhr.getBlockPos())
                            .getBlock()
                            .getName())
                    .style(ChatFormatting.GREEN)
                    .component(), true);

            connectingFrom = null;
            connectingFromBox = null;
            mc.player.level()
                    .playLocalSound(mc.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS,
                            0.5f, 0.5f, false);
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static String extra_gauges$checkForSpecialIssues(FactoryPanelBehaviour from, BlockPos toPos) {
        if (from == null)
            return "factory_panel.connection_aborted";

        BlockState state1 = from.blockEntity.getBlockState();
        BlockPos diff = toPos
                .subtract(from.getPos());
        Direction connectedDirection = FactoryPanelBlock.connectedDirection(state1);

        if (connectedDirection != PanelConnection.makeContext(state1))
            return "factory_panel.same_orientation";

        if (connectedDirection.getAxis()
                .choose(diff.getX(), diff.getY(), diff.getZ()) != 0)
            return "factory_panel.same_surface";

        if (!diff.closerThan(BlockPos.ZERO, 16))
            return "factory_panel.too_far_apart";

        return null;
    }
}
