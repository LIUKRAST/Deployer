package net.liukrast.deployer.lib.logistics.board.renderer;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.outliner.Outliner;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.ScrollOptionPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.ScrollPanelBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class ScrollPanelRenderer {
    public static void tick(AbstractPanelBehaviour behaviour) {
        if (!(behaviour instanceof ScrollPanelBehaviour scroll))
            return;
        /*if (!behaviour.isActive()) {
            Outliner.getInstance().remove(behaviour);
            continue;
        }*/
        Minecraft mc = Minecraft.getInstance();
        BlockHitResult target = (BlockHitResult) mc.hitResult;

        assert mc.player != null;
        ItemStack mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean clipboard = behaviour.bypassesInput(mainHandItem);
        assert target != null;

        boolean highlight = behaviour.testHit(target.getLocation()) && !clipboard;
        if (!scroll.renderHoverOverlay()) highlight = false;

        addBox(target.getBlockPos(), target.getDirection(), scroll, highlight);

        if (!highlight || !scroll.renderHoverOverlay())
            return;

        List<MutableComponent> tip = new ArrayList<>();
        tip.add(scroll.label.copy());
        tip.add(CreateLang.translateDirect("gui.value_settings.hold_to_edit"));
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
    }

    /**
     * Creates and displays an outline box for the given scroll panel behavior.
     * Chooses between text or icon display depending on the behavior type,
     * sets passive/wide outline properties, and highlights the relevant block face.
     *
     * @param pos the block position of the panel
     * @param face the block face currently being targeted
     * @param behaviour the scroll panel behavior to render
     * @param highlight whether this panel should be highlighted
     */
    protected static void addBox(BlockPos pos, Direction face, ScrollPanelBehaviour behaviour,
                                 boolean highlight) {
        AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.5f)
                .contract(0, 0, -.5f)
                .move(0, 0, -.125f);
        Component label = behaviour.label;
        ValueBox box;

        if (!behaviour.renderIcon()) {
            box = new ValueBox.TextValueBox(label, bb, pos, Component.empty());
        } else if (behaviour instanceof ScrollOptionPanelBehaviour) {
            box = new ValueBox.IconValueBox(label, ((ScrollOptionPanelBehaviour<?>) behaviour).getIconForSelected(), bb, pos);
        } else {
            box = new ValueBox.TextValueBox(label, bb, pos, Component.literal(behaviour.formatValue()));
        }

        box.passive(!highlight)
                .wideOutline();

        Outliner.getInstance().showOutline(behaviour, box.transform(behaviour.getSlotPositioning()))
                .highlightFace(face);
    }

}
