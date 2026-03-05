package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveCustomOverlayIcon;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.mixin.accessor.MouseHandlerAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.helper.client.DeployerGoggleInformation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Allows Blocks and Entities to render the goggle overlay
 * */
@Mixin(GoggleOverlayRenderer.class)
public abstract class GoggleOverlayRendererMixin {

    @Unique
    private static int santa_logistics$hoverTicks = 0;

    @Shadow
    @Final
    private static Map<Object, Outliner.OutlineEntry> outlines;

    @Unique
    private static void santa_logistics$renderOverlay4Entities(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        assert mc.gameMode != null;
        if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
            return;

        HitResult objectMouseOver = mc.hitResult;
        if (!(objectMouseOver instanceof EntityHitResult result)) {
            santa_logistics$hoverTicks = 0;
            return;
        }

        for (Outliner.OutlineEntry entry : outlines.values()) {
            if (!entry.isAlive())
                continue;
            Outline outline = entry.getOutline();
            if (outline instanceof ValueBox && !((ValueBox) outline).isPassive)
                return;
        }

        Entity entity = result.getEntity();

        santa_logistics$hoverTicks++;

        boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);
        assert mc.player != null;
        boolean isShifting = mc.player.isShiftKeyDown();

        boolean hasGoggleInformation = entity instanceof DeployerGoggleInformation;

        ItemStack item = AllItems.GOGGLES.asStack();
        List<Component> tooltip = new ArrayList<>();

        if (entity instanceof IHaveCustomOverlayIcon customOverlayIcon)
            item = customOverlayIcon.getIcon(isShifting);

        if (hasGoggleInformation && wearingGoggles) {
            IHaveGoggleInformation gte = (IHaveGoggleInformation) entity;
            gte.addToGoggleTooltip(tooltip, isShifting);
        }

        // break early if goggle or hover returned false when present

        if (tooltip.isEmpty()) {
            santa_logistics$hoverTicks = 0;
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
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

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        CClient cfg = AllConfigs.client();
        int posX = width / 2 + cfg.overlayOffsetX.get();
        int posY = height / 2 + cfg.overlayOffsetY.get();

        posX = Math.min(posX, width - tooltipTextWidth - 20);
        posY = Math.min(posY, height - tooltipHeight - 20);

        float fade = Mth.clamp((santa_logistics$hoverTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24f, 0, 1);
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

        GuiGameElement.of(item)
                .at(posX + 10, posY - 16, 450)
                .render(guiGraphics);

        if (!Mods.MODERNUI.isLoaded()) {
            // default tooltip rendering when modernUI is not loaded
            RemovedGuiUtils.drawHoveringText(guiGraphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
                    colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);

            poseStack.popPose();
            return;
        }

        /* special handling for modernUI */
        MouseHandler mouseHandler = Minecraft.getInstance().mouseHandler;
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        double cursorX = mouseHandler.xpos();
        double cursorY = mouseHandler.ypos();
        ((MouseHandlerAccessor) mouseHandler).create$setXPos(Math.round(cursorX / guiScale) * guiScale);
        ((MouseHandlerAccessor) mouseHandler).create$setYPos(Math.round(cursorY / guiScale) * guiScale);

        RemovedGuiUtils.drawHoveringText(guiGraphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
                colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);

        ((MouseHandlerAccessor) mouseHandler).create$setXPos(cursorX);
        ((MouseHandlerAccessor) mouseHandler).create$setYPos(cursorY);
        poseStack.popPose();
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"))
    private static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // ENTITY
        santa_logistics$renderOverlay4Entities(guiGraphics, deltaTracker);
    }

    @Inject(method = "renderOverlay", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;<init>()V", shift = At.Shift.AFTER))
    private static void renderOverlay$1(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "pos") BlockPos pos, @Local(name = "world") ClientLevel world, @Local(name = "hasGoggleInformation") boolean hasGoggleInformation, @Local(name = "item") LocalRef<ItemStack> item, @Local(name = "isShifting") boolean isShifting) {
        if(!hasGoggleInformation && world.getBlockState(pos).getBlock() instanceof DeployerGoggleInformation info)
            item.set(info.getIcon(isShifting));
    }

    @Definition(id = "be", local = @Local(type = BlockEntity.class, name = "be"))
    @Definition(id = "IDisplayAssemblyExceptions", type = IDisplayAssemblyExceptions.class)
    @Expression("be instanceof IDisplayAssemblyExceptions")
    @Inject(method = "renderOverlay", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static void renderOverlay$2(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "pos") BlockPos pos, @Local(name = "world") ClientLevel world, @Local(name = "isShifting") boolean isShifting, @Local(name = "goggleAddedInformation") LocalBooleanRef goggleAddedInformation, @Local(name = "tooltip") List<Component> tooltip, @Local(name = "wearingGoggles") boolean wearingGoggles) {
        if(wearingGoggles && world.getBlockState(pos).getBlock() instanceof DeployerGoggleInformation info)
            goggleAddedInformation.set(goggleAddedInformation.get() || info.addToGoogleTooltip(world, pos, world.getBlockState(pos), tooltip, isShifting));
    }
}
