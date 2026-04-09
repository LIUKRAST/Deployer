package net.liukrast.deployer.lib.logistics.packager.screen;

import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class TabWidget<T extends TabData> extends AbstractSimiWidget {
    protected final T screen;
    protected boolean selected;

    public TabWidget(int x, int y, int w, int h, T tab) {
        super(x, y, w, h, tab.getTitle());
        this.screen = tab;
    }

    public abstract void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

    @Override
    protected void doRender(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.renderItem(screen.getIcon(), getX()+2, getY()+2);
    }

    @Override
    public @NotNull List<Component> getToolTip() {
        return List.of(screen.getTitle());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public T getScreen() {
        return screen;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
