package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packager.screen.RequesterTabScreen;
import net.liukrast.deployer.lib.logistics.packager.screen.RequesterTabWidget;
import net.liukrast.deployer.lib.logistics.packager.screen.TabsWidget;
import net.liukrast.deployer.lib.logistics.requester.RequesterExtraDataC2SPacket;
import net.liukrast.deployer.lib.logistics.requester.RequesterRequestDataPacket;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixinExtensions.RRSExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(RedstoneRequesterScreen.class)
public abstract class RedstoneRequesterScreenMixin extends AbstractSimiContainerScreen<RedstoneRequesterMenu> implements RRSExtension {

    @Unique private static final ResourceLocation deployer$TEXTURE = Deployer.CONSTANTS.id("textures/gui/stock_keeper_tabs.png");
    @Unique private List<RequesterTabScreen<?>> deployer$tabs;
    @Unique private TabsWidget<RequesterTabScreen<?>> deployer$tabsWidget;
    @Unique private Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> deployer$extraOrders;

    @Unique private final ItemStack deployer$DEFAULT_ICON = PackageStyles.STANDARD_BOXES.getFirst().getDefaultInstance();
    @Unique private static final Component deployer$DEFAULT_TITLE = Component.translatable("stock_inventory_type.items");

    @Unique private static final int[] deployer$INTERNAL_DIM = {
            5, // X offset of the inner area
            16, // Y offset of the inner area
            220, // Width of the inner area
            42 // Height of the inner area
    };

    public RedstoneRequesterScreenMixin(RedstoneRequesterMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(RedstoneRequesterMenu container, Inventory inv, Component title, CallbackInfo ci) {
        CatnipServices.NETWORK.sendToServer(new RequesterRequestDataPacket(container.contentHolder.getBlockPos()));
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if(deployer$tabs == null || deployer$tabs.isEmpty()) return;
        this.addWidget(deployer$tabsWidget = new TabsWidget<>(
                getGuiLeft() - RequesterTabWidget.TAB_WIDTH + 5,
                getGuiTop() + 3,
                RequesterTabWidget.MAX_TABS,
                RequesterTabWidget.TAB_WIDTH,
                RequesterTabWidget.TAB_HEIGHT,
                RequesterTabWidget::new,
                deployer$tabs,
                deployer$tabsWidget == null ? null : deployer$tabsWidget.getSelected()
        ) {
            @Override
            public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                graphics.blit(deployer$TEXTURE, getX(), getY() + 16, 64, this.getSelected() == null ? tabHeight : 0, tabWidth, tabHeight);
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
        assert minecraft != null;
        deployer$tabs.forEach(screen -> screen.init(minecraft, deployer$INTERNAL_DIM[2], deployer$INTERNAL_DIM[3]));
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        int x = getGuiLeft();
        int y = getGuiTop();
        if(deployer$tabsWidget == null) return;
        if (deployer$tabsWidget.getSelected() == null) return;
        graphics.blit(deployer$TEXTURE, x + 3, y + 17, 16, 160, 224, 41);
        graphics.pose().pushPose();
        graphics.pose().translate(x+ deployer$INTERNAL_DIM[0],y+ deployer$INTERNAL_DIM[1],0);
        deployer$tabsWidget.getSelected().render(graphics, mouseX-x-deployer$INTERNAL_DIM[0], mouseY-y-deployer$INTERNAL_DIM[1], partialTicks);
        graphics.pose().popPose();
    }

    @ModifyExpressionValue(method = "renderForeground", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int renderForeground(int original) {
        return deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null ? original : 0;
    }

    @Inject(method = "renderForeground", at = @At("TAIL"))
    private void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        int x = getGuiLeft();
        int y = getGuiTop();

        /* RENDER SELECTOR WIDGET */

        // Loading animation
        if(deployer$tabsWidget == null && deployer$tabs == null) {
            assert Minecraft.getInstance().level != null;
            int frame = ((int)Minecraft.getInstance().level.getGameTime()) % 8;
            graphics.blit(deployer$TEXTURE, x - RequesterTabWidget.TAB_WIDTH + 5, y + 30, 0, 80 + frame*16, 16, 16);
        }
        // Actual widget
        if(deployer$tabsWidget == null) return;
        deployer$tabsWidget.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean deployer$mouseClicked(double mouseX, double mouseY, int button) {
        if (deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return false;
        double mX = mouseX -getGuiLeft()-deployer$INTERNAL_DIM[0];
        double mY = mouseY -getGuiTop()-deployer$INTERNAL_DIM[1];
        if(deployer$isInArea(mX, mY)) return deployer$tabsWidget.getSelected().mouseClicked(mX, mY, button);
        return false;
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        double mX = mouseX -getGuiLeft()-deployer$INTERNAL_DIM[0];
        double mY = mouseY -getGuiTop()-deployer$INTERNAL_DIM[1];
        if(deployer$isInArea(mX, mY)) {
            cir.setReturnValue(deployer$tabsWidget.getSelected().mouseScrolled(mX, mY, scrollX, scrollY));
            cir.cancel();
        }
    }

    /* Uniques & Interface*/
    @Unique
    private boolean deployer$isInArea(double mouseX, double mouseY) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return false;
        return mouseX > 0 && mouseX < deployer$INTERNAL_DIM[2] && mouseY > 0 && mouseY < deployer$INTERNAL_DIM[3];
    }

    @Unique
    @SuppressWarnings("unchecked")
    private <V> void deployer$processTab(ClientRegisterHelpers.RequesterBuilder<V> e) {
        var type = e.type();
        var orderData = (GenericOrderContained<V>) deployer$extraOrders.get(type);
        deployer$tabs.add(e.factory().create(menu, type, Objects.requireNonNullElseGet(orderData, GenericOrderContained::empty)));
    }

    @Override
    public @Nullable RequesterTabScreen<?> deployer$getTab() {
        return deployer$tabsWidget == null ? null : deployer$tabsWidget.getSelected();
    }

    @Override
    public void deployer$insertStack(ItemStack stack) {
        if(deployer$tabsWidget == null || deployer$tabsWidget.getSelected() == null) return;
        deployer$tabsWidget.getSelected().quickMoveItemEvent(stack);
    }

    @Override
    public void deployer$receiveData(Map<StockInventoryType<?, ?, ?>, GenericOrderContained<?>> dataMap) {
        if(deployer$extraOrders != null) return;
        deployer$extraOrders = dataMap;
        deployer$tabs = new ArrayList<>();
        ClientRegisterHelpers.getRequesterTabs().forEach(this::deployer$processTab);
        if(deployer$tabsWidget != null) removeWidget(deployer$tabsWidget);
        if(deployer$tabs.isEmpty()) return;
        this.addWidget(deployer$tabsWidget = new TabsWidget<>(
                getGuiLeft() - RequesterTabWidget.TAB_WIDTH + 5,
                getGuiTop() + 3,
                RequesterTabWidget.MAX_TABS,
                RequesterTabWidget.TAB_WIDTH,
                RequesterTabWidget.TAB_HEIGHT,
                RequesterTabWidget::new,
                deployer$tabs,
                deployer$tabsWidget == null ? null : deployer$tabsWidget.getSelected()
        ) {
            @Override
            public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                graphics.blit(deployer$TEXTURE, getX(), getY() + 16, 64, this.getSelected() == null ? tabHeight : 0, tabWidth, tabHeight);
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
        assert minecraft != null;
        deployer$tabs.forEach(screen -> screen.init(minecraft, deployer$INTERNAL_DIM[2], deployer$INTERNAL_DIM[3]));
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void removed(CallbackInfo ci) {
        if(deployer$tabs == null || deployer$tabs.isEmpty()) return;
        Map<StockInventoryType<?,?,?>, GenericOrderContained<?>> exportData = new HashMap<>();
        for(var tab : deployer$tabs){
            var data = tab.addToSendQueue();
            if(data == null || data.isEmpty()) continue;
            exportData.put(tab.getType(), data);
        }
        CatnipServices.NETWORK.sendToServer(new RequesterExtraDataC2SPacket(menu.contentHolder.getBlockPos(), exportData));
    }
}
