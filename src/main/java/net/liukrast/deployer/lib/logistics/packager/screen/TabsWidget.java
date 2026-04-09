package net.liukrast.deployer.lib.logistics.packager.screen;

import com.simibubi.create.content.logistics.box.PackageStyles;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.liukrast.deployer.lib.DeployerConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TabsWidget<T extends TabData> extends AbstractSimiWidget {
    private static final ResourceLocation TEXTURE = DeployerConstants.id("textures/gui/stock_keeper_tabs.png");


    private final int maxTabs;
    public final int tabWidth;
    public final int tabHeight;

    private TabWidget<T> selected = null;
    private final List<TabWidget<T>> widgets;
    private int sectionIndex = 0;

    public TabsWidget(int x, int y, int maxTabs, int tabWidth, int tabHeight, TabFactory<T> factory, List<T> tabs, @Nullable T oldSelected) {
        super(x, y, tabWidth, 31 + (Math.min(maxTabs, tabs.size()+1)) * (tabHeight +1));
        this.maxTabs = maxTabs;
        this.tabWidth = tabWidth;
        this.tabHeight = tabHeight;
        widgets = new ArrayList<>();
        for(int i = 0; i < tabs.size(); i++) {
            var tab = tabs.get(i);
            int n = (i + 1)% this.maxTabs;
            var nT = factory.create(x, 16 + y + n*(this.tabHeight +1), this.tabWidth, this.tabHeight, tab);
            if(tab == oldSelected) {
                selected = nT;
                nT.setSelected(true);
            }
            widgets.add(nT);
        }
    }

    @Override
    protected void doRender(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if(sectionIndex != 0) {
            boolean hover1 = isInArea(getX() + 2, getY() + 1, 14, 12, mouseX, mouseY);
            graphics.blit(TEXTURE, getX() + 2, getY(), hover1 ? 48 : 32, 0, 16, 16);

            for(int i = 0; i < maxTabs; i++) {
                int fi = i+sectionIndex* maxTabs -1;
                if(fi >= widgets.size()) break;
                widgets.get(fi).render(graphics, mouseX, mouseY, partialTicks);
            }
        } else {
            renderBackground(graphics, mouseX, mouseY, partialTicks);
            graphics.renderItem(getIcon(), getX() + 2, getY() + 18); //TODO Config
            for(int i = 0; i < maxTabs - 1; i++) {
                if(i >= widgets.size()) break;
                widgets.get(i).render(graphics, mouseX, mouseY, partialTicks);
            }
        }
        if(maxTabs * (sectionIndex+1) <= widgets.size()+1) {
            boolean hover2 = isInArea(getX() + 2, getY() + height - 15, 14, 12, mouseX, mouseY);
            graphics.blit(TEXTURE, getX() + 2, getY() + height - 16, hover2 ? 48 : 32, 16, 16, 16);
        }
    }

    public abstract void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

    public abstract ItemStack getIcon();

    public abstract Component getTitle();

    @Override
    protected void renderTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (sectionIndex == 0 && isInArea(getX(), getY()+16, tabWidth, tabHeight, mouseX, mouseY)) {
            int ttx = this.lockedTooltipX == -1 ? mouseX : this.lockedTooltipX + this.getX();
            int tty = this.lockedTooltipY == -1 ? mouseY : this.lockedTooltipY + this.getY();
            Font font = Minecraft.getInstance().font;
            graphics.renderComponentTooltip(font, List.of(getTitle()), ttx, tty);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(sectionIndex > 0 && isInArea(getX()+2, getY()+1, 14, 12, mouseX, mouseY)) {
            // Above button
            sectionIndex--;
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if(maxTabs * (sectionIndex+1) <= widgets.size()+1 && // Check if it can still increase
                isInArea(getX()+2, getY()+height-15, 14, 12, mouseX, mouseY) // Check the correct area
        ) {
            // below button
            sectionIndex++;
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if(sectionIndex == 0) {
            if(isInArea(getX(), getY()+16, tabWidth, tabHeight, mouseX, mouseY)) {
                if(selected != null) selected.setSelected(false);
                selected = null;
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
            for(int i = 0; i < maxTabs - 1; i++) {
                if(i >= widgets.size()) break;
                var w = widgets.get(i);
                if(!w.isHovered()) continue;
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                if(selected != null) selected.setSelected(false);
                selected = w;
                w.setSelected(true);
                return true;
            }
        } else {
            for(int i = 0; i < maxTabs; i++) {
                int fi = i+sectionIndex* maxTabs -1;
                if(fi >= widgets.size()) break;
                var w = widgets.get(fi);
                if(!w.isHovered()) continue;
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                if(selected != null) selected.setSelected(false);
                selected = w;
                w.setSelected(true);
                return true;
            }
        }
        return false;
    }

    private boolean isInArea(int x, int y, int w, int h, int px, int py) {
        return px > x && px <= x+w && py > y && py <= y+h;
    }

    private boolean isInArea(int x, int y, int w, int h, double px, double py) {
        return px > x && px <= x+w && py > y && py <= y+h;
    }

    @Nullable
    public T getSelected() {
        if(this.selected == null) return null;
        return selected.getScreen();
    }

    @FunctionalInterface
    public interface TabFactory<T extends TabData> {
        TabWidget<T> create(int x, int y, int w, int h, T tab);
    }
}
