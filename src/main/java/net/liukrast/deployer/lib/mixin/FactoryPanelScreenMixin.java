package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.screen.FakeStack;
import net.liukrast.deployer.lib.logistics.board.connection.ProvidesConnection;
import net.liukrast.deployer.lib.mixinExtensions.FPSExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FactoryPanelScreen.class)
public abstract class FactoryPanelScreenMixin extends AbstractSimiScreen implements FPSExtension {
    @Shadow private List<BigItemStack> inputConfig;
    @Shadow private BigItemStack outputConfig;
    @Shadow private FactoryPanelBehaviour behaviour;

    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;restocker:Z",
                    opcode = Opcodes.GETFIELD)
    )
    private boolean init(boolean original, @Local(argsOnly = true) FactoryPanelBehaviour fpb) {
        if(!(fpb instanceof AbstractPanelBehaviour apb)) return original;
        return deployer$isRestocker(apb);
    }

    @Override
    public boolean deployer$isRestocker(AbstractPanelBehaviour apb) {
        return apb.hasInteraction("restocker");
    }

    @WrapOperation(
            method = "updateConfigs",
            at = @At(
                    value = "NEW",
                    target = "com/simibubi/create/content/logistics/BigItemStack"
            )
    )
    private BigItemStack updateConfigs(ItemStack stack, int count, Operation<BigItemStack> original) {
        if (behaviour instanceof AbstractPanelBehaviour apb) return new FakeStack<>(apb, count, null, true);
        return original.call(stack, count);
    }

    @ModifyReturnValue(method = "lambda$updateConfigs$0", at = @At("RETURN"))
    private BigItemStack lambda$updateConfigs$0(BigItemStack original, @Local(name = "b") FactoryPanelBehaviour b, @Local(argsOnly = true) FactoryPanelConnection c) {
        var pc = ProvidesConnection.getCurrentConnection(c, () -> null);
        if(!(b instanceof AbstractPanelBehaviour apb)) {
            if(pc == DeployerPanelConnections.STOCK_CONNECTION.get()) return original;
            else return new FakeStack<>(null, c.amount, pc, false);
        }
        if(pc != DeployerPanelConnections.STOCK_CONNECTION.get()) return new FakeStack<>(apb, c.amount, pc, true);
        var opt = apb.getConnectionValue(DeployerPanelConnections.STOCK_CONNECTION.get());
        if(opt.isEmpty()) return new FakeStack<>(apb, c.amount, pc, true);
        ItemStack stack = opt.get().getStackValue();
        if(stack == null) return new FakeStack<>(apb, c.amount, pc, true);
        return new BigItemStack(stack, original.count);
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
        if(!original && outputConfig instanceof FakeStack<?> holder) {
            holder.renderAsOutput(graphics, mouseX, mouseY, x + 160, y + 48);
            return true;
        }
        return original;
    }

    /*@ModifyArg(method = "renderWindow", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/CreateLang;text(Ljava/lang/String;)Lnet/createmod/catnip/lang/LangBuilder;", ordinal = 2))
    private String renderWindow(String text) {
        if(outputConfig instanceof FakeStack<?> holder) {
            return holder.getTitle();
        }
        return text;
    }*/

    @ModifyExpressionValue(method = "renderWindow", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;asStack()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack renderWindow(ItemStack original) {
        if(behaviour instanceof AbstractPanelBehaviour apb) return apb.getItem().getDefaultInstance();
        return original;
    }

    @WrapOperation(
            method = "renderWindow",
            at = @At(
                    value = "NEW",
                    target = "com/simibubi/create/content/logistics/BigItemStack"
            )
    )
    private BigItemStack renderWindow(ItemStack stack, int count, Operation<BigItemStack> original) {
        if(!(behaviour instanceof AbstractPanelBehaviour apb)) return original.call(stack, count);
        return new FakeStack<>(apb, 1, null, true);
    }

    @Inject(method = "renderInputItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"), cancellable = true)
    private void renderInputItem(GuiGraphics graphics, int slot, BigItemStack itemStack, int mouseX, int mouseY, CallbackInfo ci, @Local(name = "inputX") int inputX, @Local(name = "inputY") int inputY) {
        if(itemStack instanceof FakeStack<?> holder) {
            holder.renderAsInput(graphics, mouseX, mouseY, inputX, inputY);
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseScrolled", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean mouseScrolled(boolean original, @Local(name = "itemStack") BigItemStack itemStack) {
        if(itemStack instanceof FakeStack<?>) return false;
        return original;
    }

    @WrapOperation(
            method = "mouseScrolled",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;clamp(III)I"
            )
    )
    private int mouseScrolled(
            int value,
            int min,
            int max,
            Operation<Integer> original,
            @Local(name = "itemStack") BigItemStack itemStack,
            @Local(argsOnly = true, ordinal = 0) double mouseX,
            @Local(argsOnly = true, ordinal = 1) double mouseY,
            @Local(argsOnly = true, ordinal = 2) double scrollX,
            @Local(argsOnly = true, ordinal = 3) double scrollY
    ) {
        if(!(itemStack instanceof FakeStack<?> fs)) return original.call(value, min, max);
        return fs.mouseScrolled(mouseX, mouseY, scrollX, scrollY, hasShiftDown(), hasControlDown(), hasAltDown());
    }

    @Inject(method = "searchForCraftingRecipe", at = @At("HEAD"), cancellable = true)
    private void searchForCraftingRecipe(CallbackInfo ci) {
        if(inputConfig.stream().anyMatch(big -> big instanceof FakeStack<?> fs && fs.locksCrafting())) ci.cancel();
    }
}
