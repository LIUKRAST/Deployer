package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.GaugeSlot;
import net.liukrast.deployer.lib.logistics.board.StockInventoryHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FactoryPanelScreen.class)
public abstract class FactoryPanelScreenMixin extends AbstractSimiScreen {

    @Shadow private boolean restocker;
    @Shadow private List<BigItemStack> inputConfig;
    @Shadow private BigItemStack outputConfig;
    @Shadow private FactoryPanelBehaviour behaviour;

    @WrapOperation(
            method = "updateConfigs",
            at = @At(
                    value = "NEW",
                    target = "com/simibubi/create/content/logistics/BigItemStack"
            )
    )
    @SuppressWarnings("unchecked")
    private BigItemStack updateConfigs(ItemStack stack, int count, Operation<BigItemStack> original) {
        if (behaviour instanceof AbstractPanelBehaviour apb) {
            GaugeSlot<?, AbstractPanelBehaviour> slot = (GaugeSlot<?, AbstractPanelBehaviour>) ClientRegisterHelpers.getSlot(apb.getPanelType());
            if (slot == null)
                return new BigItemStack(ItemStack.EMPTY, 0);
            return slot.createHolder(apb);
        }
        return original.call(stack, count);
    }

    @ModifyReturnValue(method = "lambda$updateConfigs$0", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private BigItemStack lambda$updateConfigs$0(BigItemStack original, @Local(name = "b") FactoryPanelBehaviour b) {
        if(!(b instanceof AbstractPanelBehaviour apb)) return original;
        GaugeSlot<?, AbstractPanelBehaviour> slot = (GaugeSlot<?, AbstractPanelBehaviour>) ClientRegisterHelpers.getSlot(apb.getPanelType());
        if (slot == null)
            return new BigItemStack(ItemStack.EMPTY, 0);
        return slot.createHolder(apb);
    }

    @ModifyExpressionValue(
            method = "renderWindow",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelScreen;restocker:Z",
                    ordinal = 6,
                    opcode = Opcodes.GETFIELD)
    )
    private boolean renderWindow(
            boolean original,
            @Local(argsOnly = true) GuiGraphics graphics,
            @Local(name = "mouseX") int mouseX, @Local(name = "mouseY") int mouseY,
            @Local(name = "x") int x, @Local(name = "y") int y
    ) {
        if(!original && outputConfig instanceof StockInventoryHolder<?> holder) {
            holder.renderAsOutput(graphics, mouseX, mouseY, x + 160, y + 48, font);
            return true;
        }
        return original;
    }

    @ModifyArg(method = "renderWindow", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/CreateLang;text(Ljava/lang/String;)Lnet/createmod/catnip/lang/LangBuilder;", ordinal = 2))
    private String renderWindow(String text) {
        if(outputConfig instanceof StockInventoryHolder<?> holder) {
            return holder.getTitle();
        }
        return text;
    }

    @ModifyExpressionValue(method = "renderWindow", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;asStack()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack renderWindow(ItemStack original) {
        if(behaviour instanceof AbstractPanelBehaviour apb) return apb.getItem().getDefaultInstance();
        return original;
    }

    @Inject(method = "renderInputItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"), cancellable = true)
    private void renderInputItem(GuiGraphics graphics, int slot, BigItemStack itemStack, int mouseX, int mouseY, CallbackInfo ci, @Local(name = "inputX") int inputX, @Local(name = "inputY") int inputY) {
        if(itemStack instanceof StockInventoryHolder<?> holder) {
            holder.renderAsInput(graphics, mouseX, mouseY, inputX, inputY, restocker, font);
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseScrolled", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean mouseScrolled(boolean original, @Local(name = "itemStack") BigItemStack itemStack) {
        if(itemStack instanceof StockInventoryHolder<?> holder) return holder.isValueEmpty();
        return original;
    }

    @ModifyArg(method = "mouseScrolled", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"), index = 0)
    private int mouseScrolled(int original, @Local(name = "itemStack") BigItemStack stack, @Local(argsOnly = true, ordinal = 3) double scrollY) {
        if(stack instanceof StockInventoryHolder<?>) return (int) (stack.count + Math.signum(scrollY) * 1);//holder.getStockInventoryType().packageHandler().scrollAmount(hasControlDown(), hasShiftDown(), hasAltDown()));
        return original;
    }

    @ModifyArg(method = "mouseScrolled", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"), index = 2)
    private int mouseScrolled$1(int original, @Local(name = "itemStack") BigItemStack stack) {
        if(stack instanceof StockInventoryHolder<?>) return Integer.MAX_VALUE;
        return original;
    }

    @Inject(method = "searchForCraftingRecipe", at = @At("HEAD"), cancellable = true)
    private void searchForCraftingRecipe(CallbackInfo ci) {
        if(inputConfig.stream().anyMatch(big -> big instanceof StockInventoryHolder<?>)) ci.cancel();
    }
}
