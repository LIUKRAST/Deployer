package net.liukrast.deployer.lib.logistics.packager.screen;

import net.liukrast.deployer.lib.Deployer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class KeeperTabWidget extends TabWidget<KeeperTabScreen> {
    public static final int MAX_TABS = 5;
    public static final int TAB_WIDTH = 19;
    public static final int TAB_HEIGHT = 21;

    private static final ResourceLocation TEXTURE = Deployer.CONSTANTS.id("textures/gui/stock_keeper_tabs.png");
    public KeeperTabWidget(int x, int y, int w, int h, KeeperTabScreen tab) {
        super(x, y, w, h, tab);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var tip = screen.getWarnTooltip();
        boolean warn = tip != null && !selected;
        graphics.blit(TEXTURE, getX() - (warn ? 13 : 0), getY(), warn ? 0 : 13, selected ? height : 0, width + (warn ? 13 : 0), height);
        if(warn && isInArea(getX()-11, getY() + 5, 10, 10, mouseX, mouseY)) {
            graphics.renderTooltip(Minecraft.getInstance().font, screen.getWarnTooltip(), Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean isInArea(int x, int y, int w, int h, int px, int py) {
        return px > x && px <= x+w && py > y && py <= y+h;
    }

}
