package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.events.ClientEvents;
import net.liukrast.deployer.lib.event.PanelClientEvent;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ClientEvents.class)
public class ClientEventsMixin {
    @Unique
    private static final List<Consumer<AbstractPanelBehaviour>> deployer$TICKERS = PanelClientEvent.fireTicker();

    @Inject(method = "onTick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/filtering/FilteringRenderer;tick()V", shift = At.Shift.AFTER))
    private static void onTick(boolean isPreEvent, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        HitResult target = mc.hitResult;
        if (!(target instanceof BlockHitResult result))
            return;

        ClientLevel world = mc.level;
        BlockPos pos = result.getBlockPos();
        assert world != null;
        BlockState state = world.getBlockState(pos);

        assert mc.player != null;
        if (mc.player.isShiftKeyDown())
            return;
        if (!(world.getBlockEntity(pos) instanceof SmartBlockEntity sbe))
            return;

        for (BlockEntityBehaviour b : sbe.getAllBehaviours()) {
            if (!(b instanceof AbstractPanelBehaviour behaviour))
                continue;

            if (!behaviour.isActive())
                continue;
            if (behaviour.getSlotPositioning() instanceof ValueBoxTransform.Sided)
                ((ValueBoxTransform.Sided) behaviour.getSlotPositioning()).fromSide(result.getDirection());
            if (!behaviour.getSlotPositioning().shouldRender(world, pos, state))
                continue;
            if (!behaviour.mayInteract(mc.player))
                continue;

            deployer$TICKERS.forEach(con -> con.accept(behaviour));
        }
    }
}
