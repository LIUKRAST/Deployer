package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FactoryPanelConfigurationPacket.class)
public class FactoryPanelConfigurationPacketMixin {
    @Inject(
            method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;disconnectAll()V")
    )
    private void applySettings(ServerPlayer player, FactoryPanelBlockEntity be, CallbackInfo ci, @Local(name = "behaviour") FactoryPanelBehaviour fpb) {
        if(!(fpb instanceof AbstractPanelBehaviour apb)) return;
        apb.reset();
    }
}
