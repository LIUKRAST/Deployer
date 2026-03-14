package net.liukrast.deployer.lib.helper;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.liukrast.deployer.lib.logistics.packager.screen.KeeperTabScreen;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Helper class that contains several methods related to GUI rendering
 * */
public class GuiRenderingHelpers {
    private GuiRenderingHelpers() {}

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
}
