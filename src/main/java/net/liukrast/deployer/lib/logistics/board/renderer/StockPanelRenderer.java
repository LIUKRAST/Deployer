package net.liukrast.deployer.lib.logistics.board.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.StockPanelBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class StockPanelRenderer {
    public static void tick(AbstractPanelBehaviour behaviour) {
        Minecraft mc = Minecraft.getInstance();
        BlockHitResult target = (BlockHitResult)mc.hitResult;
        if(!(behaviour instanceof StockPanelBehaviour<?,?> stock)) return;
        assert target != null;
        ClientLevel world = mc.level;
        BlockPos pos = target.getBlockPos();
        assert world != null;
        assert mc.player != null;
        BlockState state = world.getBlockState(pos);
        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean showCount = behaviour.isCountVisible();
        Component label = behaviour.getLabel();
        boolean hit = behaviour.getSlotPositioning().testHit(world, pos, state, target.getLocation()
                .subtract(Vec3.atLowerCornerOf(pos)));

        AABB emptyBB = new AABB(Vec3.ZERO, Vec3.ZERO);
        AABB bb = emptyBB.inflate(.25f);

        ValueBox box = stock.createBox(label, bb, pos);
        //ValueBox box = new FluidValueBox(label, bb, pos, filter, behaviour.getCountLabelForValueBox());
        box.passive(!hit || behaviour.bypassesInput(stack));

        Outliner.getInstance()
                .showOutline(Pair.of("filter" + behaviour.netId(), pos), box.transform(behaviour.getSlotPositioning()))
                .lineWidth(1 / 64f)
                .withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null)
                .highlightFace(target.getDirection());

        if (!hit)
            return;

        List<MutableComponent> tip = new ArrayList<>();
        tip.add(label.copy());
        tip.add(behaviour.getTip());
        if (showCount)
            tip.add(behaviour.getAmountTip());

        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
    }

    public static void render(AbstractPanelBehaviour behaviour, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if(!(behaviour instanceof StockPanelBehaviour<?,?> stock)) return;
        stock.render(partialTicks, ms, buffer, light, overlay);
    }
}
