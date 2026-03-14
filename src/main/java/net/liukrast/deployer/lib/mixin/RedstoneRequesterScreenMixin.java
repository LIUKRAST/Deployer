package net.liukrast.deployer.lib.mixin;

import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.packager.screen.StockRequesterPage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RedstoneRequesterScreen.class)
public abstract class RedstoneRequesterScreenMixin extends AbstractSimiContainerScreen<RedstoneRequesterMenu> {

    @Shadow private List<Integer> amounts;

    @Unique private static final ResourceLocation deployer$TEXTURE = DeployerConstants.id("textures/gui/stock_keeper_tabs.png");
    @Unique private static final Component deployer$DEFAULT_ICON_TITLE = Component.translatable("stock_inventory_type.items");
    @Unique private static final List<Component> deployer$UNFINISHED_ORDER = List.of(
            Component.translatable("stock_inventory_type.unfinished_order").withStyle(style -> style.withColor(0x5391e1)),
            Component.translatable("stock_inventory_type.unfinished_order_line_1").withStyle(ChatFormatting.GRAY),
            Component.translatable("stock_inventory_type.unfinished_order_line_2").withStyle(ChatFormatting.GRAY)
    );
    @Unique private List<? extends StockRequesterPage<?>> deployer$tabs;
    @Unique private StockRequesterPage<?> deployer$selected = null;
    @Unique private static final int deployer$SECTION_SIZE = 3;
    @Unique private int deployer$section = 0;
    @Unique private static final int deployer$TAB_Y = 6;
    @Unique private static final int deployer$TAB_X = -5;

    public RedstoneRequesterScreenMixin(RedstoneRequesterMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(RedstoneRequesterMenu container, Inventory inv, Component title, CallbackInfo ci) {
        deployer$tabs = ClientRegisterHelpers.getRequesterTabs()
                .map(e -> e.create(menu))
                .toList();
    }

    /*@Inject(method = "renderBg", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/gui/AllGuiTextures;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void renderBg(GuiGraphics graphics, float partialTicks, int pMouseX, int pMouseY, CallbackInfo ci) {
        if(deployer$selected == null) return;
        //deployer$selected.packageHandler().renderOrderedItems(graphics, partialTicks, getGuiLeft() + 3, getGuiTop(), List.of(), );
    }*/

    @Inject(method = "renderForeground", at = @At("TAIL"))
    private void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (deployer$tabs.isEmpty()) return;
        int x = getGuiLeft() + deployer$TAB_X;
        int y = getGuiTop() + deployer$TAB_Y;
        if(deployer$section == 0) {
            graphics.blit(deployer$TEXTURE, x - 8, y + 20, 14, deployer$selected == null ? 60 : 40, 18, 20);
            graphics.renderItem(PackageStyles.STANDARD_BOXES.getFirst().getDefaultInstance(), x - 7, y + 22);
            if (mouseX > x - 8 && mouseX < x + 12 && mouseY > y + 20 && mouseY < y + 40)
                graphics.renderTooltip(font, deployer$DEFAULT_ICON_TITLE, mouseX, mouseY);
        }
        if(deployer$tabs.size() + 1 > deployer$SECTION_SIZE) {
            if(deployer$section > 0) graphics.blit(deployer$TEXTURE, x - 6, y+5, 32 + (mouseX > x-5 && mouseX < x-5+14 && mouseY > y+7 && mouseY < y+5+12 ? 16 : 0), 0, 16, 16);
            if((deployer$section + 1) * deployer$SECTION_SIZE < deployer$tabs.size() + 1)
                graphics.blit(deployer$TEXTURE, x - 6, y + deployer$SECTION_SIZE * 20 + 20, 32 + (mouseX > x-5 && mouseX < x-5+14 && mouseY > y+ deployer$SECTION_SIZE * 20 + 21 && mouseY < y+ deployer$SECTION_SIZE * 20 + 21+12 ? 16 : 0), 16, 16, 16);
        }
        for(int i = 0; i < deployer$SECTION_SIZE - (deployer$section == 0 ? 1 : 0); i++) {
            int t = i + deployer$section * deployer$SECTION_SIZE;
            if(t >= deployer$tabs.size()) break;
            var tab = deployer$tabs.get(t);
            int iY = i;
            if(deployer$section != 0) iY--;
            graphics.blit(deployer$TEXTURE, x - 8, y + 40 + iY * 20, 14, deployer$selected == tab ? 60 : 40, 18, 20);
            graphics.renderItem(tab.getIcon(), x - 7, y + 42 + iY * 20);
            if (mouseX > x - 8 && mouseX < x + 12 && mouseY > y + 40 + iY * 20 && mouseY < y + 60 + iY * 20)
                graphics.renderTooltip(font, tab.getTitle(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
        if (deployer$tabs.isEmpty()) return super.mouseClicked(mouseX, mouseY, pButton);
        int x = getGuiLeft() + deployer$TAB_X;
        int y = getGuiTop() + deployer$TAB_Y;
        if(mouseX > x - 5 && mouseX < x - 5 + 14) {
            if(deployer$section > 0 && mouseY > y+7 && mouseY < y+5+12) {
                deployer$section--;
                return true;
            }
            else if((deployer$section + 1) * deployer$SECTION_SIZE < (deployer$tabs.size() + 1) && mouseY > y+ deployer$SECTION_SIZE * 20 + 21 && mouseY < y+ deployer$SECTION_SIZE * 20 + 21+12) {
                deployer$section++;
                return true;
            }
        }
        if(mouseX > x - 8 && mouseX < x + 12) {
            if (deployer$section == 0 && mouseY > y + 20 && mouseY < y + 40) {
                deployer$selected = null;
                init();
                return true;
            }
            for (int i = 0; i < deployer$SECTION_SIZE - (deployer$section == 0 ? 1 : 0); ++i) {
                int t = i + deployer$section * deployer$SECTION_SIZE;
                if(t >= deployer$tabs.size()) break;
                var tab = deployer$tabs.get(t);
                int iY = i;
                if(deployer$section != 0) iY--;
                if (mouseY > y + 40 + iY * 20 && mouseY < y + 60 + iY * 20) {
                    deployer$selected = tab;
                    init();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, pButton);
    }
}
