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
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.platform.services.NetworkHelper;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packager.screen.*;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderRequestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin extends AbstractSimiContainerScreen<StockKeeperRequestMenu> {
    /* SHADOWS */
    @Shadow StockTickerBlockEntity blockEntity;
    @Shadow int windowHeight;
    @Shadow @Final Couple<Integer> noneHovered;
    @Shadow @Final int rowHeight;
    @Shadow @Final private static AllGuiTextures HEADER;
    @Shadow @Final private boolean isAdmin;
    @Shadow public LerpedFloat itemScroll;
    @Shadow int lockX;
    @Shadow public AddressEditBox addressBox;
    @Shadow boolean encodeRequester;
    @Shadow
    public List<BigItemStack> itemsToOrder;
    @Shadow
    int besideSearchButtonY;
    @Shadow
    int jeiSyncX;
    /* UNIQUES */
    @Unique private static final ResourceLocation deployer$TEXTURE = Deployer.CONSTANTS.id("textures/gui/stock_keeper_tabs.png");
    @Unique private List<KeeperTabScreen> deployer$tabs;
    @Unique private TabsWidget<KeeperTabScreen> deployer$tabsWidget;

    @Unique private final ItemStack deployer$DEFAULT_ICON = PackageStyles.STANDARD_BOXES.getFirst().getDefaultInstance();
    @Unique private static final Component deployer$DEFAULT_TITLE = Component.translatable("stock_inventory_type.items");
    @Unique private static final List<Component> deployer$UNFINISHED_ORDER = List.of(
            Component.translatable("stock_inventory_type.unfinished_order").withStyle(style -> style.withColor(0x5391e1)),
            Component.translatable("stock_inventory_type.unfinished_order_line_1").withStyle(ChatFormatting.GRAY),
            Component.translatable("stock_inventory_type.unfinished_order_line_2").withStyle(ChatFormatting.GRAY)
    );
    @Unique private KeeperSourceContext deployer$context = null;

    private StockKeeperRequestScreenMixin(StockKeeperRequestMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/HashSet;<init>(Ljava/util/Collection;)V", ordinal = 0))
    private void init(StockKeeperRequestMenu container, Inventory inv, Component title, CallbackInfo ci) {
        if(deployer$context == null) deployer$context = KeeperSourceContext.of(blockEntity);
        deployer$tabs = ClientRegisterHelpers.getKeeperTabs()
                .map(func -> func.apply(deployer$context, menu))
                .toList();
    }

    /* INIT */
    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        assert minecraft != null;
        deployer$tabs.forEach(tab -> {
            tab.init(minecraft, 190, windowHeight-HEADER.getHeight()-22);
            tab.setGui(getGuiLeft() + 18, getGuiTop() + 16);
        });
        if(deployer$tabs.isEmpty()) return;
        this.addWidget(deployer$tabsWidget = new TabsWidget<>(
                getGuiLeft()- KeeperTabWidget.TAB_WIDTH+10,
                getGuiTop()+21,
                KeeperTabWidget.MAX_TABS,
                KeeperTabWidget.TAB_WIDTH,
                KeeperTabWidget.TAB_HEIGHT,
                KeeperTabWidget::new,
                deployer$tabs,
                deployer$tabsWidget == null ? null : deployer$tabsWidget.getSelected()
        ) {
            @Override
            public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                boolean warn = this.getSelected() != null && !itemsToOrder.isEmpty();
                graphics.blit(deployer$TEXTURE, getX() - (warn ? 13 : 0), getY() + 16, warn ? 0 : 13, this.getSelected() == null ? tabHeight : 0, tabWidth + (warn ? 13 : 0), tabHeight);
                if(warn && isInArea(getX()-11, getY() + 5+16, mouseX, mouseY)) {
                    graphics.renderTooltip(Minecraft.getInstance().font, deployer$UNFINISHED_ORDER, Optional.empty(), mouseX, mouseY);
                }
            }

            private boolean isInArea(int x, int y, int px, int py) {
                return px > x && px <= x+ 10 && py > y && py <= y+ 10;
            }

            @Override
            public ItemStack getIcon() {
                return deployer$DEFAULT_ICON;
            }

            @Override
            public Component getTitle() {
                return deployer$DEFAULT_TITLE;
            }
        });
    }

    /* REFRESH SEARCH RESULTS */
    /* CONTAINER TICK */
    @Inject(method = "containerTick", at = @At("HEAD"))
    private void containerTick(CallbackInfo ci) {
        deployer$tabs.forEach(KeeperTabScreen::containerTick);
    }
    @ModifyExpressionValue(method = "containerTick", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;displayedItems:Ljava/util/List;", opcode = Opcodes.GETFIELD))
    private List<List<BigItemStack>> containerTick(List<List<BigItemStack>> original) {
        return deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null ? original : Collections.emptyList();
    }
    /* RENDER BACKGROUND */
    /* RENDER BG */
    @WrapWithCondition(method = "renderBg", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/gui/AllGuiTextures;render(Lnet/minecraft/client/gui/GuiGraphics;II)V", ordinal = 7))
    private boolean renderBg(AllGuiTextures instance, GuiGraphics graphics, int x, int y) {
        return deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null;
    }

    @WrapWithCondition(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private boolean renderBg(EditBox instance, GuiGraphics graphics, int i, int j, float v) {
        return deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null;
    }

    @ModifyExpressionValue(method = "renderBg", at = @At(value = "INVOKE", target = "Ljava/lang/String;isBlank()Z", ordinal = 1))
    private boolean renderBg(boolean original) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return original;
        return false;
    }

    /**
     * Reason: Remove items from rendering when you're in a different category (e.g., fluids)
     * */
    @ModifyExpressionValue(method = "renderBg", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;displayedItems:Ljava/util/List;", opcode = Opcodes.GETFIELD))
    private List<List<BigItemStack>> renderBg(List<List<BigItemStack>> original) {
        return deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null ? original : Collections.emptyList();
    }

    @ModifyExpressionValue(method = "renderBg", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;itemsToOrder:Ljava/util/List;", opcode = Opcodes.GETFIELD))
    private List<BigItemStack> renderBg$1(List<BigItemStack> original) {
        return deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null ? original : Collections.emptyList();
    }

    @Inject(method = "renderBg", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/gui/AllGuiTextures;render(Lnet/minecraft/client/gui/GuiGraphics;II)V", ordinal = 2, shift = At.Shift.AFTER))
    private void renderBg$1(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci, @Local(name = "x") int x, @Local(name = "y") int y) {
        if (deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        graphics.blit(deployer$TEXTURE, x, y, 15, 80, 226, 37);
    }

    @Definition(id = "allEmpty", local = @Local(type = boolean.class, name = "allEmpty"))
    @Expression("allEmpty")
    @ModifyExpressionValue(method = "renderBg", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean renderBg$1(boolean original) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() != null) return false;
        return original;
    }

    /* RENDER FOREGROUND */

    @Definition(id = "hoveredSlot", local = @Local(type = Couple.class))
    @Definition(id = "noneHovered", field = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;noneHovered:Lnet/createmod/catnip/data/Couple;")
    @Expression("hoveredSlot != this.noneHovered")
    @ModifyExpressionValue(method = "renderForeground", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean renderForeground(boolean original,
                                     @Local(name = "hoveredSlot") Couple<Integer> hoveredSlot,
                                     @Local(argsOnly = true) GuiGraphics graphics,
                                     @Local(argsOnly = true, ordinal = 0) int mouseX,
                                     @Local(argsOnly = true, ordinal = 1) int mouseY,
                                     @Local(argsOnly = true) float partialTicks
    ) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null || hoveredSlot.getFirst() == -2 /*TODO: Check*/) return original;
        graphics.pose().pushPose();
        graphics.pose().translate(getGuiLeft() + 18, getGuiTop() + 16, 0);
        deployer$tabsWidget.getSelected().render(graphics, mouseX - getGuiLeft() - 18, mouseY - getGuiTop() - 16, partialTicks);
        graphics.pose().popPose();
        return false;
    }

    @Inject(method = "renderForeground", at = @At("TAIL"))
    private void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if(deployer$tabsWidget == null) return;
        deployer$tabsWidget.render(graphics, mouseX, mouseY, partialTicks);
    }
    /* RENDER ITEM ENTRY */
    /* DRAW ITEM COUNT */
    /* GET ORDER FOR V */
    /* REVALIDATE ORDERS */
    /* GET HOVERED SLOT */
    @Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true)
    private void getHoveredSlot(int x, int y, CallbackInfoReturnable<Couple<Integer>> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        cir.setReturnValue(noneHovered);
        cir.cancel();
    }
    /* GET HOVERED INGREDIENT */
    /**
     * Completely ignored if you're not in the item TAB.
     * This is used for JEI compat so might need a future implementation for fluid stacks
     *
     */
    @Inject(method = "getHoveredIngredient", at = @At("HEAD"), cancellable = true)
    private void getHoveredIngredient(int mouseX, int mouseY, CallbackInfoReturnable<Optional<Pair<ItemStack, Rect2i>>> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        cir.setReturnValue(Optional.empty());
        cir.cancel();
    }

    /* IS CONFIRM HOVERED */
    /* TROUBLESHOOTING MESSAGE */

    /* INTERNAL */
    @Unique
    private boolean deployer$skipArea(double mouseX, double mouseY) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return true;
        if(itemScroll.getChaseTarget() == 0 && mouseY > besideSearchButtonY && mouseY <= besideSearchButtonY + 15) {
            if(mouseX > jeiSyncX && mouseX <= jeiSyncX + 15)
                return true;
            if(isAdmin && mouseX > lockX && mouseX <= lockX + 15)
                return true;
        }
        int x = getGuiLeft();
        int y = getGuiTop();
        return mouseX < x + 18 || mouseY < y + 16 || mouseX > x + 18 + deployer$tabsWidget.getSelected().width || mouseY > y + 16 + deployer$tabsWidget.getSelected().height;
    }
    /* MOUSE CLICKED */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(double pMouseX, double pMouseY, int pButton, CallbackInfoReturnable<Boolean> cir) {
        if (deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        if(deployer$skipArea(pMouseX, pMouseY)) return;
        cir.setReturnValue(deployer$tabsWidget.getSelected().mouseClicked(pMouseX - getGuiLeft() - 18, pMouseY - getGuiTop() - 16, pButton));
        cir.cancel();
    }

    /* MOUSE RELEASED */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void mouseReleased(double pMouseX, double pMouseY, int pButton, CallbackInfoReturnable<Boolean> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        if(deployer$skipArea(pMouseX, pMouseY)) return;
        cir.setReturnValue(deployer$tabsWidget.getSelected().mouseReleased(pMouseX - getGuiLeft() - 18, pMouseY - getGuiTop() - 16, pButton));
        cir.cancel();
    }

    /* MOUSE SCROLLED */
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        if(deployer$skipArea(mouseX, mouseY)) return;
        cir.setReturnValue(deployer$tabsWidget.getSelected().mouseScrolled( mouseX - getGuiLeft() - 18, mouseY - getGuiTop() - 16, scrollX, scrollY));
        cir.cancel();
    }
    /* CLAMP SCROLL BAR */
    /* GET MAX SCROLL */
    @Inject(method = "getMaxScroll", at = @At("HEAD"), cancellable = true)
    private void getMaxScroll(CallbackInfoReturnable<Integer> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        cir.setReturnValue(Math.max(0, (2 * rowHeight - windowHeight + 84 + 50) / rowHeight));
        cir.cancel();
    }

    /* MOUSE DRAGGED */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY, CallbackInfoReturnable<Boolean> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        if(deployer$skipArea(pMouseX, pMouseY)) return;
        cir.setReturnValue(deployer$tabsWidget.getSelected().mouseDragged( pMouseX - getGuiLeft() - 18, pMouseY - getGuiTop() - 16, pButton, pDragX, pDragY));
        cir.cancel();
    }

    /* CHAR TYPED */
    @Inject(method = "charTyped", at = @At("RETURN"))
    private void charTyped(char pCodePoint, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null || cir.getReturnValueZ()) return;
        deployer$tabsWidget.getSelected().charTyped(pCodePoint, pModifiers);
    }
    /* KEY PRESSED */
    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void keyPressed(int pKeyCode, int pScanCode, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null || cir.getReturnValueZ()) return;
        deployer$tabsWidget.getSelected().keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed$1(int pKeyCode, int pScanCode, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        if (deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        assert minecraft != null;
        if (!minecraft.options.keyChat.matches(pKeyCode, pScanCode)) return;
        deployer$tabsWidget.getSelected().switchFocused();
        cir.setReturnValue(true);
        cir.cancel();
    }

    /* REMOVED */
    /* SEND IT */
    @Inject(method = "sendIt", at = @At("HEAD"))
    private void sendIt(CallbackInfo ci, @Share("extra_orders") LocalRef<Map<StockInventoryType<?,?,?>, GenericOrderContained<?>>> map) {
        Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> temp = new HashMap<>();
        for(var tab : deployer$tabs) {
            if(!(tab instanceof ProvidesOrder<?> po)) continue;
            var toAdd = po.addToSendQueue();
            if(toAdd == null || toAdd.isEmpty()) continue;
            temp.put(po.getType(), toAdd);
        }
        map.set(temp);
    }

    @ModifyExpressionValue(
            method = "sendIt",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/List;isEmpty()Z",
                            ordinal = 0
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/List;isEmpty()Z",
                            ordinal = 1
                    )
            }
    )
    private boolean sendIt(boolean original, @Share("extra_orders") LocalRef<Map<StockInventoryType<?,?,?>, GenericOrderContained<?>>> map) {
        return original && map.get().isEmpty();
    }

    @WrapOperation(method = "sendIt", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"))
    private void sendIt(
            NetworkHelper instance,
            CustomPacketPayload customPacketPayload,
            Operation<Void> original,
            @Local(name = "order") PackageOrderWithCrafts order,
            @Share("extra_orders") LocalRef<Map<StockInventoryType<?,?,?>, GenericOrderContained<?>>> map
    ) {
        CatnipServices.NETWORK.sendToServer(new GenericOrderRequestPacket(blockEntity.getBlockPos(), order, map.get(), addressBox.getValue(), encodeRequester));
    }

    @Inject(method = "sendIt", at = @At("TAIL"))
    private void sendIt(CallbackInfo ci) {
        deployer$tabs.forEach(KeeperTabScreen::onSendIt);
    }



    /* KEY RELEASED */
    @Inject(method = "keyReleased", at = @At("TAIL"))
    private void keyReleased(int pKeyCode, int pScanCode, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        deployer$tabsWidget.getSelected().keyReleased(pKeyCode, pScanCode, pModifiers);
    }
    /* GET EXTRA AREAS */
    /* IS SCHEMATIC LIST MODE */
    /* REQUEST SCHEMATIC LIST */
    /* REQUEST CRAFTABLE */
    /* UPDATE CRAFTABLE AMOUNTS */
    /* MAX CRAFTABLE */
    /* REMOVE LEAST ESSENTIAL ITEMSTACK */
    /* REMOVE INGREDIENT AMOUNTS */
    /* SYNC JEI */ //TODO
}
