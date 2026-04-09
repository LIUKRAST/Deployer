package net.liukrast.deployer.lib.helper.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.liukrast.deployer.lib.helper.GuiRenderingHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import static com.simibubi.create.foundation.gui.AllGuiTextures.NUMBERS;

public class FluidGhostSlot extends AbstractSimiWidget {

    private FluidStack ghostStack = FluidStack.EMPTY;
    protected final AbstractContainerMenu menu;
    public FluidGhostSlot(int x, int y, AbstractContainerMenu menu, FluidStack stack) {
        this(x,y,menu);
        this.ghostStack = stack.copy();
    }

    public FluidGhostSlot(int x, int y, AbstractContainerMenu menu) {
        super(x, y, 18, 18);
        this.menu = menu;
    }

    @Override
    protected void doRender(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.doRender(graphics, mouseX, mouseY, partialTicks);
        if(!ghostStack.isEmpty()) {
            GuiRenderingHelpers.renderFluidSlot(graphics, ghostStack, getX() + 1, getY() + 1, 16, 16);
        }
        if(isHovered) {
            graphics.fillGradient(RenderType.guiOverlay(), getX()+1, getY()+1, getX() + 17, getY() + 17, -2130706433, -2130706433, 0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!this.clicked(mouseX, mouseY)) return false;
        var filter = FilterItemStack.of(menu.getCarried()).fluid(Minecraft.getInstance().level);
        setGhostStack(filter.copyWithAmount(10));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if(!this.clicked(mouseX, mouseY)) return false;
        if(ghostStack.isEmpty()) return true;
        ghostStack.setAmount(Mth.clamp((int) (ghostStack.getAmount() + Math.signum(scrollY) * scrollAmount(hasControlDown(), hasShiftDown(), hasAltDown())), 1, 16000));
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public int scrollAmount(boolean ctrlDown, boolean shiftDown, boolean altDown) {
        return ctrlDown ? 100 : shiftDown ? 1000 : altDown ? 1 : 10;
    }

    public static boolean hasControlDown() {
        return Minecraft.ON_OSX
                ? InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 343)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 347)
                : InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 341)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 345);
    }

    public static boolean hasShiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344);
    }

    public static boolean hasAltDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 346);
    }

    public FluidStack getGhostStack() {
        return ghostStack;
    }

    public void setGhostStack(FluidStack ghostStack) {
        this.ghostStack = ghostStack;
    }

    private void drawItemCount(GuiGraphics graphics, int customCount) {

        String text;

        if(customCount >= 1_000_000) {
            text = customCount/1_000_000 + "kb";
        } else if(customCount >= 1000) {
            // Bucket
            text = customCount/1000 + "b";
        } else {
            // decimals of bucket
            int rem = customCount%10000;
            if(rem < 100)
                text = rem + "mb";
            else
                text = customCount/10000 + "." + (rem)/10 + "b";
        }

        if (customCount >= BigItemStack.INF /* What is considered infinite? */)
            text = "+";

        if (text.isBlank())
            return;

        int x = (int) Math.floor(-text.length() * 2.5);
        for (char c : text.toCharArray()) {
            int index = c - '0';
            int xOffset = index * 6;
            int spriteWidth = NUMBERS.getWidth();

            switch (c) {
                case ' ':
                    x += 4;
                    continue;
                case '.':
                    spriteWidth = 3;
                    xOffset = 60;
                    break;
                case 'k':
                    xOffset = 64;
                    break;
                case 'm':
                    spriteWidth = 7;
                    xOffset = 70;
                    break;
                case '+':
                    spriteWidth = 9;
                    xOffset = 84;
                    break;
                case 'b':
                    xOffset = 78;
                    break;
            }

            RenderSystem.enableBlend();
            graphics.blit(NUMBERS.location, 14 + x, 10, 0, NUMBERS.getStartX() + xOffset, NUMBERS.getStartY(),
                    spriteWidth, NUMBERS.getHeight(), 256, 256);
            x += spriteWidth - 1;
        }

    }
}
