package net.liukrast.deployer.lib.logistics.board.screen;

import com.google.common.collect.Lists;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * Represents a basic GUI screen for factory panels.
 * Automatically provides all the common buttons for panel management.
 * Users only need to override window dimensions and add custom widgets if desired.
 *
 * @param <T> the type of panel behavior displayed
 */
public class BasicPanelScreen<T extends AbstractPanelBehaviour> extends AbstractSimiScreen {
    @ApiStatus.Internal
    public static final ResourceLocation TEXTURE = Deployer.CONSTANTS.id("textures/gui/generic_gauge.png");

    protected final boolean canConnect,canMove;

    /**
     * The panel behavior associated with this screen.
     */
    public final T behaviour;
    /**
     * Whether a reset should be sent to the server when the screen closes.
     */
    private boolean sendReset;

    /**
     * Constructs a new screen for the given panel behavior.
     *
     * @param behaviour the panel behavior to display
     */
    public BasicPanelScreen(T behaviour) {
        this(behaviour, true, true);
    }

    public BasicPanelScreen(T behaviour, boolean canConnect, boolean canMove) {
        this(behaviour.getDisplayName(), behaviour, canConnect, canMove);
    }

    /**
     * Constructs a new screen with a custom title and panel behavior.
     *
     * @param component the title component to display
     * @param behaviour the panel behavior to display
     */
    public BasicPanelScreen(Component component, T behaviour) {
        this(component, behaviour, true, true);
    }

    public BasicPanelScreen(Component component, T behaviour, boolean canConnect, boolean canMove) {
        super(component);
        this.behaviour = behaviour;
        this.canConnect = canConnect;
        this.canMove = canMove;
    }

    /**
     * Returns the width of the screen window.
     * Override this to provide a custom width.
     *
     * @return the window width in pixels
     */
    public int getWindowWidth() {
        return 0;
    }

    /**
     * Returns the height of the screen window.
     * Override this to provide a custom height.
     *
     * @return the window height in pixels
     */
    public int getWindowHeight() {
        return 0;
    }

    /**
     * @return The texture to render background
     * */
    public ResourceLocation getBackgroundTexture() {
        return TEXTURE;
    }

    /**
     * Initializes the screen and adds standard GUI buttons.
     * Buttons include confirm, delete, add input, and relocate.
     */
    @Override
    protected void init() {
        setWindowSize(getWindowWidth() + 106, getWindowHeight() + 46);
        int sizeX = windowWidth;
        int sizeY = windowHeight;
        super.init();
        clearWidgets();

        int x = guiLeft;
        int y = guiTop;

        assert minecraft != null;
        IconButton confirmButton = new IconButton(x+sizeX-25, y+sizeY-24, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onConfirm);
        confirmButton.setToolTip(CreateLang.translate("gui.factory_panel.save_and_close")
                .component());
        addRenderableWidget(confirmButton);

        IconButton deleteButton = new IconButton(x+sizeX-47, y+sizeY-24, AllIcons.I_TRASH);
        deleteButton.withCallback(() -> {
            sendReset = true;
            minecraft.setScreen(null);
        });
        deleteButton.setToolTip(CreateLang.translate("gui.factory_panel.reset")
                .component());
        addRenderableWidget(deleteButton);

        if(canConnect) {
            IconButton newInputButton = new IconButton(x + 7, y + sizeY - 24, AllIcons.I_ADD);
            newInputButton.withCallback(() -> {
                FactoryPanelConnectionHandler.startConnection(behaviour);
                minecraft.setScreen(null);
            });
            newInputButton.setToolTip(CreateLang.translate("gui.factory_panel.connect_input")
                    .component());
            addRenderableWidget(newInputButton);
        }

        if(canMove) {
            int moveX = canConnect ? (x + 29) : (x + 7);
            IconButton relocateButton = new IconButton(moveX, y + sizeY - 24, AllIcons.I_MOVE_GAUGE);
            relocateButton.withCallback(() -> {
                FactoryPanelConnectionHandler.startRelocating(behaviour);
                minecraft.setScreen(null);
            });
            relocateButton.setToolTip(CreateLang.translate("gui.factory_panel.relocate")
                    .component());
            addRenderableWidget(relocateButton);
        }
    }

    /**
     * Callback invoked when the confirmation button is pressed.
     * Closes the screen and optionally sends updates to the server.
     */
    public void onConfirm() {
        assert minecraft != null;
        minecraft.setScreen(null);
    }

    /**
     * Renders the GUI window including the background, borders, and item preview.
     *
     * @param graphics the GUI graphics context
     * @param mouseX the current mouse X position
     * @param mouseY the current mouse Y position
     * @param partialTicks the partial tick time
     */
    @Override
    protected void renderWindow(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;
        ResourceLocation bg = getBackgroundTexture();
        graphics.drawCenteredString(font, title, x + windowWidth / 2, y + 4, 0x3D3C48);

        graphics.blit(bg, x, y, 0, 0, 53, 16);
        graphics.blit(bg, x + windowWidth - 53, y, 139, 0, 53, 16);

        int numLeftButtons = (canConnect ? 1 : 0) + (canMove ? 1 : 0);
        int footerLeftV = numLeftButtons == 2 ? 56 : (numLeftButtons == 1 ? 90 : 124);
        int footerLeftWidth = numLeftButtons == 2 ? 53 : 31;

        graphics.blit(bg, x, y + windowHeight - 31, 0, footerLeftV, footerLeftWidth, 32);
        graphics.blit(bg, x + windowWidth - 53, y + windowHeight - 31, 139, 56, 61, 32);

        if (windowWidth > 106) {
            int r = windowWidth - 106;
            int step = 0;
            while (r > 0) {
                graphics.blit(bg, x + 53 + step * 86, y, 53, 0, Math.min(r, 86), 16);
                step++;
                r -= 86;
            }
        }

        int fillerOffset = numLeftButtons == 0 ? 1 : footerLeftWidth;
        int bottomGap = windowWidth - 53 - fillerOffset;

        if (bottomGap > 0) {
            int r1 = bottomGap - 3;
            int step = 0;
            graphics.blit(bg, x + fillerOffset, y + windowHeight - 31, 53, 56, Math.min(bottomGap, 2), 32);
            if (bottomGap > 2) {
                graphics.blit(bg, x + windowWidth - 54, y + windowHeight - 31, 138, 56, 1, 32);
            }

            while (r1 > 0) {
                graphics.blit(bg, x + fillerOffset + 2 + step * 83, y + windowHeight - 31, 55, 56, Math.min(r1, 83), 32);
                step++;
                r1 -= 83;
            }
        }

        if (windowHeight > 47) {
            int r = windowHeight - 47;
            int step = 0;
            graphics.blit(bg, x, y + 16, 0, 16, 53, Math.min(r, 40));
            graphics.blit(bg, x + windowWidth - 53, y + 16, 139, 16, 53, Math.min(r, 40));

            if (windowWidth > 106) {
                int r1 = windowWidth - 106;
                int step1 = 0;
                while (r1 > 0) {
                    graphics.blit(bg, x + 53 + step1 * 86, y + 16, 53, 16, Math.min(r1, 86), Math.min(r, 40));
                    step1++;
                    r1 -= 86;
                }
            }
            r -= 40;
            while (r > 0) {
                graphics.blit(bg, x, y + 56 + step * 36, 0, 20, 53, Math.min(r, 36));
                graphics.blit(bg, x + windowWidth - 53, y + 56 + step * 36, 139, 20, 53, Math.min(r, 36));

                int r1 = windowWidth - 106;
                int step1 = 0;
                while (r1 > 0) {
                    graphics.blit(bg, x + 53 + step1 * 86, y + 56 + step * 36, 53, 20, Math.min(r1, 86), Math.min(r, 36));
                    step1++;
                    r1-=86;
                }
                step++;
                r-=36;
            }
        }

        GuiGameElement.of(behaviour.getItem().getDefaultInstance())
                .scale(4)
                .at(0, 0, -200)
                .render(graphics, x + windowWidth, y+windowHeight-48);
    }

    /**
     * Called when the screen is removed.
     * Sends any pending configuration updates to the server before closing.
     */
    @Override
    public void removed() {
        sendIt(null);
        super.removed();
    }

    /**
     * Sends the panel configuration to the server.
     *
     * @param toRemove optional panel position to remove
     */
    private void sendIt(@SuppressWarnings("SameParameterValue") @Nullable FactoryPanelPosition toRemove) {
        FactoryPanelPosition pos = behaviour.getPanelPosition();
        FactoryPanelConfigurationPacket packet = new FactoryPanelConfigurationPacket(
                pos,
                "",
                new HashMap<>(),
                Lists.newArrayList(),
                0,
                0,
                toRemove,
                false,
                sendReset,
                false);
        CatnipServices.NETWORK.sendToServer(packet);
    }
}
