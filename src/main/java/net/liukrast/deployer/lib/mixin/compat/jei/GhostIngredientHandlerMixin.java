package net.liukrast.deployer.lib.mixin.compat.jei;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.compat.jei.GhostIngredientHandler;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.liukrast.deployer.lib.mixinExtensions.RRSExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GhostIngredientHandler.class)
public class GhostIngredientHandlerMixin<T extends GhostItemMenu<?>> {
    @ModifyExpressionValue(
            method = "getTargetsTyped(Lcom/simibubi/create/foundation/gui/menu/AbstractSimiContainerScreen;Lmezz/jei/api/ingredients/ITypedIngredient;Z)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;isActive()Z"
            )
    )
    private boolean getTargetsTyped(boolean original, @Local(argsOnly = true) AbstractSimiContainerScreen<T> gui) {
        if(!(gui instanceof RRSExtension rRS)) return original;
        return original && rRS.deployer$getTab() == null;
    }
}
