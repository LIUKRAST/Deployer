package net.liukrast.deployer.lib.helper;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.mixin.accessor.MouseHandlerAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper class that contains several methods related to GUI rendering
 * */
public class GuiRenderingHelpers {
    private GuiRenderingHelpers() {}

    public static final ResourceLocation LOGISTICS_FONT = ResourceLocation.fromNamespaceAndPath("deployer", "logistics");

    public static void renderFluidSlot(GuiGraphics graphics, FluidStack stack, int x, int y, int width, int height) {
        renderFluid(graphics, stack.copyWithAmount(1000), x, y, width, height);
        int amount = stack.getAmount();
        if(amount == 1) return;

        String text;

        if(amount >= 1_000_000) {
            text = amount/1_000_000 + "KB";
        } else if(amount >= 1000) {
            // Bucket
            text = amount/1000 + "B";
        } else {
            // decimals of bucket
            int rem = amount%10000;
            if(rem < 100)
                text = rem + "MB";
            else
                text = amount/10000 + "." + (rem)/10 + "B";
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        Component formatted = Component.literal(text).withStyle(s -> s.withFont(LOGISTICS_FONT).withColor(0xe4ddce));
        int w = Minecraft.getInstance().font.width(formatted);
        graphics.drawString(Minecraft.getInstance().font, formatted, x+width-w, y+height+2-Minecraft.getInstance().font.lineHeight, -1, false);
        graphics.pose().popPose();
    }

    /**
     * Renders a fluid sprite in a box, in GUI context
     * @param graphics The GUI graphics
     * @param stack The fluidstack to render
     * @param x The box's x position
     * @param y The box's y position
     * @param width The box width
     * @param height The box height
     * */
    public static void renderFluid(GuiGraphics graphics, FluidStack stack, int x, int y, int width, int height) {
        if(stack.isEmpty()) return;
        RenderSystem.enableBlend();

        Fluid fluid = stack.getFluid();
        IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation fluidStill = renderProperties.getStillTexture(stack);
        var texture = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(fluidStill);
        if(texture == null) return;
        if(texture.atlasLocation() == MissingTextureAtlasSprite.getLocation()) return;

        int fluidColor = renderProperties.getTintColor(stack);

        long amount = stack.getAmount();
        long scaledAmount = (amount * height) / 1000;
        if (amount > 0 && scaledAmount < 1) {
            scaledAmount = 1;
        }
        if (scaledAmount > height) {
            scaledAmount = height;
        }

        drawTiledSprite(graphics, width, height, fluidColor, scaledAmount, texture, x, y);

        RenderSystem.setShaderColor(1, 1, 1, 1);

        RenderSystem.disableBlend();
    }

    /**
     * Draws a texture sprite repeatedly in a tiled 16×16 pattern, supporting partial tiles
     * on the top and right edges based on the provided dimensions and scaled texture height.
     *
     * @param guiGraphics  the GUI graphics context used for rendering
     * @param tiledWidth   total width, in pixels, to fill with repeated tiles
     * @param tiledHeight  total height baseline used to compute vertical placement
     * @param color        ARGB color multiplier applied before rendering
     * @param scaledAmount the vertical fill amount, determining how many tiles are drawn
     * @param sprite       the texture atlas sprite to tile
     * @param posX         X coordinate where tiling begins
     * @param posY         Y coordinate used as a reference for bottom alignment
     *
     * @implNote This method expects the block atlas to be already bound as the active texture.
     * It also assumes the sprite is 16×16 pixels
     * Using non-standard sprite sizes may result in incorrect masking or stretching.
     */
    private static void drawTiledSprite(GuiGraphics guiGraphics, final int tiledWidth, final int tiledHeight, int color, long scaledAmount, TextureAtlasSprite sprite, int posX, int posY) {
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        setGLColorFromInt(color);

        final int xTileCount = tiledWidth / 16;
        final int xRemainder = tiledWidth - (xTileCount * 16);
        final long yTileCount = scaledAmount / 16;
        final long yRemainder = scaledAmount - (yTileCount * 16);

        final int yStart = tiledHeight + posY;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int width = (xTile == xTileCount) ? xRemainder : 16;
                long height = (yTile == yTileCount) ? yRemainder : 16;
                int x = posX + (xTile * 16);
                int y = yStart - ((yTile + 1) * 16);
                if (width > 0 && height > 0) {
                    long maskTop = 16 - height;
                    int maskRight = 16 - width;

                    drawTextureWithMasking(matrix, x, y, sprite, maskTop, maskRight, 100);
                }
            }
        }
    }

    /**
     * Sets the active shader color using an ARGB integer.
     *
     * <p>This affects all later rendering operations until the color is changed again.</p>
     *
     * @param color ARGB color packed into a single integer (alpha in the highest byte)
     *
     * @implNote Callers are responsible for restoring the previous shader color if needed.
     */
    private static void setGLColorFromInt(int color) {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255F;

        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    /**
     * Draws a quad of the specified sprite, masking out a number of pixels from
     * the top and right sides.
     * This effectively crops the sprite before rendering.
     *
     *
     * @param matrix        transformation matrix used for vertex placement
     * @param xCoord        X position of the quad
     * @param yCoord        Y position of the quad
     * @param textureSprite the texture atlas sprite to render
     * @param maskTop       number of pixels to omit from the top of the sprite
     * @param maskRight     number of pixels to omit from the right of the sprite
     * @param zLevel        Z depth at which the quad is rendered
     *
     * @implNote Must be called in a valid rendering context with the correct shader and atlas bound.
     * The masking logic assumes a 16×16 sprite area for UV interpolation.
     */
    private static void drawTextureWithMasking(Matrix4f matrix, float xCoord, float yCoord, TextureAtlasSprite textureSprite, long maskTop, long maskRight, @SuppressWarnings("SameParameterValue") float zLevel) {
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();
        uMax = uMax - (maskRight / 16F * (uMax - uMin));
        vMax = vMax - (maskTop / 16F * (vMax - vMin));

        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, xCoord, yCoord + 16, zLevel).setUv(uMin, vMax);
        bufferBuilder.addVertex(matrix, xCoord + 16 - maskRight, yCoord + 16, zLevel).setUv(uMax, vMax);
        bufferBuilder.addVertex(matrix, xCoord + 16 - maskRight, yCoord + maskTop, zLevel).setUv(uMax, vMin);
        bufferBuilder.addVertex(matrix, xCoord, yCoord + maskTop, zLevel).setUv(uMin, vMin);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    /**
     * Renders a tooltip for a {@link FluidStack}, including advanced information
     * such as the fluid registry name when advanced tooltips are enabled.
     *
     * @param graphics the GUI graphics renderer
     * @param stack    the fluid stack whose tooltip should be displayed
     * @param mouseX   current mouse X coordinate
     * @param mouseY   current mouse Y coordinate
     * @param font     font used to render the tooltip
     *
     * @apiNote Fluids that are not properly registered may not provide a valid registry key.
     */
    public static void renderTooltip(GuiGraphics graphics, FluidStack stack, int mouseX, int mouseY, Font font) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stack.getHoverName());
        var flag = ClientTooltipFlag.of(Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);

        if (flag.isAdvanced()) {
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            if (id != BuiltInRegistries.FLUID.getDefaultKey()) {
                MutableComponent advancedId = Component.literal(id.toString())
                        .withStyle(ChatFormatting.DARK_GRAY);
                tooltip.add(advancedId);
            }
        }

        graphics.renderTooltip(font, tooltip,Optional.empty(), mouseX, mouseY);
    }

    public static void renderHoveringArea(GuiGraphics graphics, int hoverTicks, DeltaTracker deltaTracker, ItemStack icon, List<Component> tooltip) {
        var mc = Minecraft.getInstance();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        int tooltipTextWidth = 0;
        for (FormattedText textLine : tooltip) {
            int textLineWidth = mc.font.width(textLine);
            if (textLineWidth > tooltipTextWidth)
                tooltipTextWidth = textLineWidth;
        }

        int tooltipHeight = 8;
        if (tooltip.size() > 1) {
            tooltipHeight += 2; // gap between title lines and next lines
            tooltipHeight += (tooltip.size() - 1) * 10;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        CClient cfg = AllConfigs.client();
        int posX = width / 2 + cfg.overlayOffsetX.get();
        int posY = height / 2 + cfg.overlayOffsetY.get();

        posX = Math.min(posX, width - tooltipTextWidth - 20);
        posY = Math.min(posY, height - tooltipHeight - 20);

        float fade = Mth.clamp((hoverTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24f, 0, 1);
        Boolean useCustom = cfg.overlayCustomColor.get();
        Color colorBackground = useCustom ? new Color(cfg.overlayBackgroundColor.get())
                : BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(.75f);
        Color colorBorderTop = useCustom ? new Color(cfg.overlayBorderColorTop.get())
                : BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
        Color colorBorderBot = useCustom ? new Color(cfg.overlayBorderColorBot.get())
                : BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();

        if (fade < 1) {
            poseStack.translate(Math.pow(1 - fade, 3) * Math.signum(cfg.overlayOffsetX.get() + .5f) * 8, 0, 0);
            colorBackground.scaleAlpha(fade);
            colorBorderTop.scaleAlpha(fade);
            colorBorderBot.scaleAlpha(fade);
        }

        GuiGameElement.of(icon)
                .at(posX + 10, posY - 16, 450)
                .render(graphics);

        if (!Mods.MODERNUI.isLoaded()) {
            // default tooltip rendering when modernUI is not loaded
            RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
                    colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);

            poseStack.popPose();

            return;
        }

        /*
         * special handling for modernUI
         *
         * their tooltip handler causes the overlay to jiggle each frame,
         * if the mouse is moving, guiScale is anything but 1 and exactPositioning is enabled
         *
         * this is a workaround to fix this behavior
         */
        MouseHandler mouseHandler = Minecraft.getInstance().mouseHandler;
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        double cursorX = mouseHandler.xpos();
        double cursorY = mouseHandler.ypos();
        ((MouseHandlerAccessor) mouseHandler).create$setXPos(Math.round(cursorX / guiScale) * guiScale);
        ((MouseHandlerAccessor) mouseHandler).create$setYPos(Math.round(cursorY / guiScale) * guiScale);

        RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
                colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);

        ((MouseHandlerAccessor) mouseHandler).create$setXPos(cursorX);
        ((MouseHandlerAccessor) mouseHandler).create$setYPos(cursorY);
        poseStack.popPose();
    }

    /**
     * Helper to calculate the next step, either linearly or by snapping to multiples.
     */
    public static int getAdjustedAmount(int current, int step, double delta, boolean shouldSnap) {
        if (!shouldSnap || step <= 10) {
            return current + (int) (step * Math.signum(delta));
        }

        int direction = (int) Math.signum(delta);
        int remainder = current % step;

        if (direction > 0) {
            return current + (step - remainder);
        } else {
            return remainder == 0 ? Math.max(0, current - step) : Math.max(0, current - remainder);
        }
    }
}
