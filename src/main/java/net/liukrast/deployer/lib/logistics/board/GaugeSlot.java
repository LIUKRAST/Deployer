package net.liukrast.deployer.lib.logistics.board;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface GaugeSlot<A extends AbstractPanelBehaviour> {
    void renderInputSlot(GuiGraphics graphics, A panel, @Nullable PanelConnection<?> panelConnection, int count, int mouseX, int mouseY, int offsetX, int offsetY, Font font);
    void renderOutputSlot(GuiGraphics graphics, A panel, int count, int mouseX, int mouseY, int offsetX, int offsetY, Font font);

    /**
     * Can be used to modify the connection amount
     * */
    default int mouseScrolled(A panel, double mouseX, double mouseY, double scrollX, double scrollY, boolean ctrlDown, boolean shiftDown, boolean altDown, int count) {
        return 1;
    }
    default boolean locksCrafting(A panel) {
        return false;
    }

    static void renderDefaultSlot(GuiGraphics graphics, AbstractPanelBehaviour panel, PanelConnection<?> connection, int mouseX, int mouseY, int x, int y, Font font) {
        var stack = panel == null ? AllBlocks.FACTORY_GAUGE.asStack() : panel.getItem().getDefaultInstance();
        graphics.renderItem(stack, x, y);
        if (mouseX >= x - 1 && mouseX < x - 1 + 18 && mouseY >= y - 1
                && mouseY < y - 1 + 18) {

            MutableComponent c1 = CreateLang.builder().add(stack.getHoverName().copy())
                    .color(BaseConfigScreen.COLOR_TITLE_C)
                    .component();

            var id = DeployerRegistries.PANEL_CONNECTION.getKey(connection);
            MutableComponent c2;
            if(id == null) c2 = null;
            else {
                String key = "panel_connection." + id.getNamespace() + "." + id.getPath();
                c2 = Component.translatable("deployer.factory_panel.transferring", Component.translatable(key))
                        .withStyle(BaseConfigScreen.COLOR_TITLE_A.asStyle());
            }
            MutableComponent c3 = Component.translatable("deployer.gui.factory_panel.not_affecting_recipe_tip_0")
                    .withStyle(ChatFormatting.GRAY);
            MutableComponent c4 = Component.translatable("deployer.gui.factory_panel.not_affecting_recipe_tip_1")
                    .withStyle(ChatFormatting.GRAY);
            graphics.renderComponentTooltip(font, c2 == null ? List.of(c1, c3, c4) : List.of(c1, c2, c3, c4),
                    mouseX, mouseY);
        }
    }
}
