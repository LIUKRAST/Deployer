package net.liukrast.deployer.lib.logistics.board;

import com.simibubi.create.content.logistics.BigItemStack;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class FakeStack<APB extends AbstractPanelBehaviour> extends BigItemStack {
    private final APB value;
    private final GaugeSlot<APB> slot;
    private final PanelConnection<?> panelConnection;


    // APB null -> Normal factory gauge in non-stock mode

    public FakeStack(@Nullable APB value, int amount, PanelConnection<?> panelConnection) {
        super(ItemStack.EMPTY, amount);
        this.value = value;
        GaugeSlot<APB> slot = ClientRegisterHelpers.getSlot(value);
        this.slot = slot == null ? createDefault() : slot;
        this.panelConnection = panelConnection;
    }

    public void renderAsInput(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, Font font) {
        slot.renderInputSlot(graphics, value, panelConnection, count, mouseX, mouseY, x, y, font);
    }

    public void renderAsOutput(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, Font font) {
        slot.renderOutputSlot(graphics, value, count, mouseX, mouseY, x, y, font);
    }

    public int mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, boolean shiftDown, boolean controlDown, boolean altDown) {
        return slot.mouseScrolled(value, mouseX, mouseY, scrollX, scrollY, controlDown, shiftDown, altDown, this.count);
    }

    private static <APB extends AbstractPanelBehaviour> GaugeSlot<APB> createDefault() {
        return new GaugeSlot<>() {
            @Override
            public void renderInputSlot(GuiGraphics graphics, APB panel, PanelConnection<?> connection, int count, int mouseX, int mouseY, int x, int y, Font font) {
                GaugeSlot.renderDefaultSlot(graphics, panel, connection, mouseX, mouseY, x, y, font);
            }

            @Override
            public void renderOutputSlot(GuiGraphics graphics, APB panel, int count, int mouseX, int mouseY, int offsetX, int offsetY, Font font) {

            }
        };
    }

    public boolean locksCrafting() {
        return slot.locksCrafting(value);
    }
}
