package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FactoryPanelSupportBehaviour.class)
public class FactoryPanelSupportBehaviourMixin {
    @Definition(id = "behaviour", local = @Local(type = FactoryPanelBehaviour.class))
    @Definition(id = "satisfied", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;satisfied:Z")
    @Expression("behaviour.satisfied")
    @ModifyExpressionValue(method = "shouldBePoweredTristate", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean shouldBePoweredTristate(boolean original, @Local(name = "behaviour") FactoryPanelBehaviour behaviour) {
        if(behaviour instanceof AbstractPanelBehaviour apb) return apb.getConnectionValue(DeployerPanelConnections.REDSTONE.get()).orElse(0) > 0;
        return original;
    }

    @Definition(id = "behaviour", local = @Local(type = FactoryPanelBehaviour.class))
    @Definition(id = "count", field = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;count:I")
    @Expression("behaviour.count != 0")
    @ModifyExpressionValue(method = "shouldBePoweredTristate", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean shouldBePoweredTriState$1(boolean original, @Local(name = "behaviour") FactoryPanelBehaviour behaviour) {
        return behaviour instanceof AbstractPanelBehaviour panel ? panel.getOutputConnections().contains(DeployerPanelConnections.REDSTONE.get()) : original;
    }
}
