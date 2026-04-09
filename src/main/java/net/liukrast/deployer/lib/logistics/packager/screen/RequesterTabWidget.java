package net.liukrast.deployer.lib.logistics.packager.screen;

import net.liukrast.deployer.lib.DeployerConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RequesterTabWidget extends TabWidget<RequesterTabScreen<?>> {
    public static final int MAX_TABS = 3;
    public static final int TAB_WIDTH = 20;
    public static final int TAB_HEIGHT = 22;

    private static final ResourceLocation TEXTURE = DeployerConstants.id("textures/gui/stock_keeper_tabs.png");
    public RequesterTabWidget(int x, int y, int w, int h, RequesterTabScreen<?> tab) {
        super(x, y, w, h, tab);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(TEXTURE, getX()-1, getY()-1, 64, selected ? height : 0, width, height);
    }

    @Override
    protected void doRender(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.pose().pushPose();
        graphics.pose().translate(1, 1, 0);
        super.doRender(graphics, mouseX, mouseY, partialTicks);
        graphics.pose().popPose();
    }
}
