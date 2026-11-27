package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.liukrast.deployer.lib.mixinExtensions.RRSExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Final protected AbstractContainerMenu menu;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void renderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if(!deployer$isHiddenGhostSlot(slot)) return;
        ci.cancel();
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void isHovering(Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if(!deployer$isHiddenGhostSlot(slot)) return;
        cir.setReturnValue(false);
        cir.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if(!(this instanceof RRSExtension rRS)) return;
        if(rRS.deployer$mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if(slot == null || type != ClickType.QUICK_MOVE) return;
        if(!(this instanceof RRSExtension rRS)) return;
        if(slotId >= 36 || rRS.deployer$getTab() == null) return;
        rRS.deployer$insertStack(slot.getItem());
        ci.cancel();
    }

    @Unique
    private boolean deployer$isHiddenGhostSlot(Slot slot) {
        if(!(this instanceof RRSExtension rrs) || rrs.deployer$getTab() == null) return false;
        if(!(menu instanceof GhostItemMenu<?> ghostMenu)) return false;
        return slot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() == ghostMenu.ghostInventory;
    }
}
