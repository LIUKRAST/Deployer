package net.liukrast.deployer.lib.logistics.packager.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KeeperTabScreen extends Screen implements TabData {
    private final ItemStack icon;
    protected final KeeperSourceContext context;

    private int guiLeft,guiTop;

    public KeeperTabScreen(KeeperSourceContext context, Component title, Item icon) {
        super(title);
        this.icon = icon.getDefaultInstance();
        this.context = context;
    }

    public void containerTick() {

    }

    public ItemStack getIcon() {
        return icon;
    }


    public void onSendIt() {

    }

    public void switchFocused() {

    }

    public final int getGuiLeft() {
        return guiLeft;
    }

    public final int getGuiTop() {
        return guiTop;
    }

    public final void setGui(int left, int top) {
        this.guiLeft = left;
        this.guiTop = top;
    }

    public List<Component> getWarnTooltip() {
        return null;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

}
