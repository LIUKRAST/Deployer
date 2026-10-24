package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.logistics.factoryBoard.*;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.DeployerClient;
import net.liukrast.deployer.lib.compat.sable.SableCompat;
import net.liukrast.deployer.lib.helper.ClientRegisterHelpers;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.*;
import net.liukrast.deployer.lib.mixinExtensions.FPBExtension;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.liukrast.deployer.lib.registry.DeployerPartialModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(FactoryPanelRenderer.class)
public class FactoryPanelRendererMixin {
    @Unique private static long deployer$timer = 0;
    @Unique private static final int deployer$INACTIVE = ConnectionLine.pack(0x888898);

    @Inject(method = "renderSafe(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"))
    private void renderSafe(FactoryPanelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        assert be.getLevel() != null;
        long currentTime = be.getLevel().getGameTime();
        if(currentTime - deployer$timer >= 5) {
            DeployerClient.SELECTED_CONNECTION = null;
            deployer$timer = currentTime;
        }
    }

    /* Allows abstract panels to have their own render system and decides whether a bulb should be rendered or not */
    @ModifyExpressionValue(
            method = "renderSafe(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;getAmount()I")
    )
    private int renderSafe(
            int original,
            @Local(name = "behaviour") FactoryPanelBehaviour behaviour,
            @Local(argsOnly = true) float partialTicks,
            @Local(argsOnly = true)PoseStack ms,
            @Local(argsOnly = true)MultiBufferSource buffer,
            @Local(argsOnly = true, ordinal = 0) int light,
            @Local(argsOnly = true, ordinal = 1) int overlay
    ) {
        if (behaviour instanceof AbstractPanelBehaviour abstractPanel) {
            ms.pushPose();
            ClientRegisterHelpers.getPanelRenderers().forEach(renderer -> renderer.render(abstractPanel, partialTicks, ms, buffer, light, overlay));
            ms.popPose();
            var state = abstractPanel.getBulbState();
            return state == AbstractPanelBehaviour.BulbState.DISABLED ? 0 : 1;
        } else return original;
    }

    @ModifyArg(
            method = "renderBulb",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"
            ),
            index = 0
    )
    private static PartialModel renderBulb(PartialModel partial, @Local(argsOnly = true) FactoryPanelBehaviour b) {
        if(!(b instanceof AbstractPanelBehaviour apb)) return partial;
        PartialModel model = apb.getBulbState().partialModel;
        return model != null ? model : partial;
    }

    @ModifyExpressionValue(
            method = "renderBulb",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperByteBuffer;light(I)Lnet/createmod/catnip/render/SuperByteBuffer;", ordinal = 0)
    )
    private static SuperByteBuffer tintBulb(SuperByteBuffer original, @Local(name = "behaviour") FactoryPanelBehaviour behaviour) {
        if (behaviour instanceof AbstractPanelBehaviour abstractPanel && abstractPanel.getBulbState() == AbstractPanelBehaviour.BulbState.TINTED) {
            return original.color(abstractPanel.getBulbTint());
        } else return original;
    }

    @WrapOperation(
            method = "renderBulb",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperByteBuffer;color(IIII)Lnet/createmod/catnip/render/SuperByteBuffer;")
    )
    private static SuperByteBuffer tintGlow(
            SuperByteBuffer instance, int r, int g, int b, int a,
            Operation<SuperByteBuffer> original,
            @Local(name = "behaviour") FactoryPanelBehaviour behaviour,
            @Local(name = "glow") float glow
    ) {
        if (!(behaviour instanceof AbstractPanelBehaviour apb) || apb.getBulbState() != AbstractPanelBehaviour.BulbState.TINTED) {
            return original.call(instance, r, g, b, a);
        }
        int tint = apb.getBulbTint();
        float tintR = Math.min(200, (tint >> 16) & 0xFF) * glow;
        float tintG = Math.min(200, (tint >>  8) & 0xFF) * glow;
        float tintB = Math.min(200,  tint & 0xFF) * glow;
        return instance.color((int)tintR, (int)tintG, (int)tintB, a);
    }

    /* Render extra connections */
    @Inject(
            method = "renderSafe(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;", ordinal = 0)
    )
    private void renderSafe(FactoryPanelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci, @Local(name = "behaviour") FactoryPanelBehaviour behaviour) {
        for(FactoryPanelConnection connection : ((FPBExtension)behaviour).deployer$getExtra().values())
            FactoryPanelRenderer.renderPath(behaviour, connection, partialTicks, ms, buffer, light, overlay);
    }

    /* Render paths */
    @Inject(
            method = "renderPath",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I",
                    ordinal = 0
            )
    )
    private static void renderPath(
            FactoryPanelBehaviour behaviour,
            FactoryPanelConnection connection,
            float partialTicks,
            PoseStack ms,
            MultiBufferSource buffer,
            int light,
            int overlay,
            CallbackInfo ci,
            @Share("line") LocalRef<Integer> line,
            @Local(name = "sbe") FactoryPanelSupportBehaviour sbe,
            @Local(name = "dots") LocalBooleanRef dots,
            @Local(name = "redstoneLinkMode") boolean redstoneLinkMode
    ) {
        if(!redstoneLinkMode) return;
        if(sbe instanceof AbstractPanelSupportBehaviour apsb) { // If the panel supports custom interactions
            var pc = ProvidesConnection.getCurrentConnection(connection, () -> null); // Avoid cache-loop here
            if(pc == DeployerPanelConnections.STOCK_CONNECTION.get() && !(behaviour instanceof AbstractPanelBehaviour)) { //If we are rendering a normal factory panel and connection is stock, he overrides color
                int color = behaviour.getIngredientStatusColor();
                float glow = behaviour.bulb.getValue(partialTicks);
                if(!behaviour.redstonePowered && !behaviour.waitingForNetwork && glow > 0 && !behaviour.satisfied) {
                    float p = (1 - (1 - glow) * (1 - glow));
                    color = Color.mixColors(color, connection.success ? 0xEAF2EC : 0xE5654B, p);
                }
                line.set(ConnectionLine.pack(color, false, !behaviour.isMissingAddress() && !behaviour.waitingForNetwork && !behaviour.satisfied && !behaviour.redstonePowered));
                return;
            }
            line.set(pc == null ? deployer$INACTIVE : deployer$getPanelColor(pc, apsb));
            dots.set(ConnectionLine.unpackDots(line.get()));
        } else {
            if(!(behaviour instanceof AbstractPanelBehaviour apb)) return; // If normal panel, and normal link, return to sourcecode
            var toCheck = sbe.isOutput() ? ((ProvidesConnection)behaviour).getInputConnections() : ((ProvidesConnection)behaviour).getOutputConnections();
            if(toCheck.contains(DeployerPanelConnections.REDSTONE.get())) {
                line.set(sbe.isOutput() ?
                        DeployerPanelConnections.REDSTONE.get().getColor(sbe.shouldPanelBePowered()) :
                                deployer$getPanelColor(DeployerPanelConnections.REDSTONE.get(), apb)
                );
            } else line.set(deployer$INACTIVE);
        }
    }

    @Inject(
            method = "renderPath",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;getIngredientStatusColor()I")
    )
    private static void renderPath$1(
            FactoryPanelBehaviour behaviour,
            FactoryPanelConnection connection,
            float partialTicks,
            PoseStack ms,
            MultiBufferSource buffer,
            int light,
            int overlay,
            CallbackInfo ci,
            @Share("line") LocalRef<Integer> line,
            @Local(name = "sbe") FactoryPanelSupportBehaviour sbe,
            @Local(name = "dots") LocalBooleanRef dots,
            @Local(name = "pathReversed") boolean pathReversed
    ) {
        ProvidesConnection other;
        if(sbe instanceof AbstractPanelSupportBehaviour aPSB) other = aPSB;
        else other = (ProvidesConnection) FactoryPanelBehaviour.at(behaviour.getWorld(), connection);
        if(other == null) {
            if(sbe != null) {
                if(!(behaviour instanceof AbstractPanelBehaviour apb)) {
                    line.set(ConnectionLine.pack(pathReversed ? (behaviour.count == 0 ? 0x888898 : behaviour.satisfied ? 0xEF0000 : 0x580101)
                            : (behaviour.redstonePowered ? 0xEF0000 : 0x580101)));
                    return;
                }
                var toCheck = sbe.isOutput() ? ((ProvidesConnection)behaviour).getInputConnections() : ((ProvidesConnection)behaviour).getOutputConnections();
                if(toCheck.contains(DeployerPanelConnections.REDSTONE.get())) {
                    line.set(sbe.isOutput() ?
                            DeployerPanelConnections.REDSTONE.get().getColor(sbe.shouldPanelBePowered()) :
                            deployer$getPanelColor(DeployerPanelConnections.REDSTONE.get(), apb)
                    );
                } else line.set(deployer$INACTIVE);
            } else {
                var pc = ProvidesConnection.getCurrentConnection(connection, () -> null);
                if(pc == null) line.set(deployer$INACTIVE);
                else {
                    var pos = connection.from.pos();
                    var level = Minecraft.getInstance().level;
                    assert level != null;
                    var state = level.getBlockState(pos);
                    line.set(deployer$getPanelColor(pc, level, state, pos));
                }
            }
            return;
        }

        var pc = ProvidesConnection.getCurrentConnection(connection, () -> null); // Avoid cache-loop here
        if(pc == DeployerPanelConnections.STOCK_CONNECTION.get() && !(behaviour instanceof AbstractPanelBehaviour)) return; //If we are rendering a normal factory panel and connection is stock, he overrides color
        line.set(pc == null ? deployer$INACTIVE : deployer$getPanelColor(pc, other));
        dots.set(ConnectionLine.unpackDots(line.get()));
    }

    @WrapOperation(method = "renderPath", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperByteBuffer;shiftUV(Lnet/createmod/catnip/render/SpriteShiftEntry;)Lnet/createmod/catnip/render/SuperByteBuffer;"))
    private static SuperByteBuffer renderPath(
            SuperByteBuffer instance,
            SpriteShiftEntry spriteShiftEntry,
            Operation<SuperByteBuffer> original,
            @Share("line") LocalRef<ConnectionLine> line
    ) {
        if(line.get() == null) return original.call(instance, spriteShiftEntry);
        return null;
    }

    @ModifyArg(method = "renderPath", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperByteBuffer;color(I)Lnet/createmod/catnip/render/SuperByteBuffer;"))
    private static int renderPath(
            int color,
            @Share("line") LocalRef<Integer> line,

            @Local(argsOnly = true) FactoryPanelBehaviour behaviour,
            @Local(argsOnly = true) PoseStack ms,
            @Local(argsOnly = true) float partialTicks,
            @Local(argsOnly = true) FactoryPanelConnection connection,
            @Local(argsOnly = true, ordinal = 0) int light,
            @Local(argsOnly = true, ordinal = 1) int overlay,
            @Local(argsOnly = true) MultiBufferSource buffer,

            @Local(name = "sbe") FactoryPanelSupportBehaviour sbe,
            @Local(name = "direction") Direction direction,
            @Local(name = "currentX") float currentX,
            @Local(name = "currentZ") float currentZ,
            @Local(name = "yOffset") float yOffset,
            @Local(name = "xRot") float xRot,
            @Local(name = "yRot") float yRot,
            @Local(name = "pathReversed") boolean pathReversed,
            @Local(name = "i") int i,
            @Local(name = "path") List<Direction> path,
            @Local(name = "dots") boolean dots,
            @Local(name = "isArrowSegment") boolean isArrowSegment,
            @Local(name = "blockState") BlockState blockState,
            @Local(name = "connectionSprite") SuperByteBuffer connectionSprite

    ) {
        // Hit logic
        Minecraft mc = Minecraft.getInstance();
        assert mc.level != null;
        float y = (yOffset + (direction.get2DDataValue() % 2) * 0.125f) / 512f;
        if(DeployerClient.SELECTED_CONNECTION == null && (pathReversed ? i != 0 : i != path.size() - 1) && mc.player != null && mc.player.getMainHandItem().is(AllItems.WRENCH.get())) {

            float thick = 0.25f;
            float thickOnAxis = 0.05f;
            Vector3f pos = new Vector3f(
                    currentX + behaviour.slot.xOffset * .5f + .25f,
                    y,
                    currentZ + behaviour.slot.yOffset * .5f + .25f
            ).add(-.5f, -.5f, -.5f);
            Quaternionf rotation = new Quaternionf()
                    .rotationY((float) (yRot + Math.PI))
                    .rotateX(-xRot);
            rotation.transform(pos);
            pos = pos.add(.5f, .5f, .5f);
            Direction.Axis axis = FactoryPanelBlock.connectedDirection(blockState).getAxis();

            float thickX = axis == Direction.Axis.X ? thickOnAxis : thick;
            float thickY = axis == Direction.Axis.Y ? thickOnAxis : thick;
            float thickZ = axis == Direction.Axis.Z ? thickOnAxis : thick;

            var aabb = new AABB(
                    pos.x - thickX,
                    pos.y - thickY,
                    pos.z - thickZ,
                    pos.x + thickX,
                    pos.y + thickY,
                    pos.z + thickZ
            );

            Vec3 eyePos = mc.player.getEyePosition(partialTicks);
            Vec3 lookVec = mc.player.getViewVector(partialTicks);
            double reach = mc.player.blockInteractionRange();
            Vec3 rayEnd = eyePos.add(lookVec.scale(reach));

            Pair<Vec3, Vec3> ray = SableCompat.setRayFromSubLevel(behaviour.blockEntity, eyePos, rayEnd, lookVec, reach);
            if (ray != null) {
                eyePos = ray.getFirst();
                rayEnd = ray.getSecond();
            }

            AABB worldAABB = aabb.move(behaviour.blockEntity.getBlockPos());
            Optional<Vec3> hit = worldAABB.clip(eyePos, rayEnd);
            if(hit.isPresent()) {
                if(mc.hitResult == null || mc.hitResult.getLocation().distanceToSqr(eyePos) > hit.get().distanceToSqr(eyePos)) {
                    DeployerClient.SELECTED_CONNECTION = connection;
                    DeployerClient.SELECTED_SOURCE = behaviour.getPanelPosition();
                }
            }
        }

        if(connection == DeployerClient.SELECTED_CONNECTION) {
            PartialModel partial = (dots ? DeployerPartialModels.LINES_HIGHLIGHTS : isArrowSegment ? DeployerPartialModels.ARROW_HIGHLIGHTS : DeployerPartialModels.LINES_HIGHLIGHTS)
                    .get(pathReversed ? direction : direction.getOpposite());
            SuperByteBuffer highSprite = CachedBuffers.partial(partial, blockState)
                    .rotateCentered(yRot, Direction.UP)
                    .rotateCentered(xRot, Direction.EAST)
                    .rotateCentered(Mth.PI, Direction.UP)
                    .translate(behaviour.slot.xOffset * .5 + .25, 0, behaviour.slot.yOffset * .5 + .25)
                    .translate(currentX, y, currentZ);

            highSprite
                    .light(light)
                    .overlay(overlay)
                    .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }

        var rl = line.get();
        if(rl == null) return color;
        if(sbe instanceof AbstractPanelSupportBehaviour apsb && !apsb.isOutput())
            rl = apsb.overrideConnectionColor(rl, connection, partialTicks);
        else if(behaviour instanceof AbstractPanelBehaviour apb)
            rl = apb.overrideConnectionColor(rl, connection, partialTicks);
        if(ConnectionLine.unpackFlowing(rl))
            connectionSprite.shiftUV(AllSpriteShifts.FACTORY_PANEL_CONNECTIONS);
        return ConnectionLine.unpackColor(rl);
    }

    @Unique
    private static <T> int deployer$getPanelColor(PanelConnection<T> pc, ProvidesConnection behaviour) {
        Optional<T> opt = behaviour.getConnectionValue(pc);
        if (opt.isEmpty()) return deployer$INACTIVE;
        T value = opt.get();
        return pc.getColor(value);
    }

    @Unique
    private static <T> int deployer$getPanelColor(PanelConnection<T> pc, Level level, BlockState state, BlockPos pos) {
        var listener = pc.getListener(state.getBlock());
        if(listener == null) return deployer$INACTIVE;
        else {
            var opt = listener.invalidate(level, state, pos, level.getBlockEntity(pos));
            return opt.map(pc::getColor).orElse(deployer$INACTIVE);
        }
    }
}
