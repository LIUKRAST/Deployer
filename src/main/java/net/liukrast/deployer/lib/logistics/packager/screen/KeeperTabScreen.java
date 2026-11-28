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

    /**
     * Called every client tick while the Stock Keeper screen is open, even when the tab is not selected
     */
    public void containerTick() {

    }

    /**
     * @return The icon of the tab
     */
    public ItemStack getIcon() {
        return icon;
    }


    /**
     * Called when the player sends the order of the Stock Keeper screen
     */
    public void onSendIt() {

    }

    /**
     * Called when the player presses the chat key while the tab is selected, to focus or unfocus a text field
     */
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

    /**
     * @return A warning shown on the tab icon, or an empty list
     */
    public List<Component> getWarnTooltip() {
        return null;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

}
