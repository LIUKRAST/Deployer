package net.liukrast.deployer.lib.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageEntity;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;

public class PackageRenderEvent extends Event implements IModBusEvent {
    private final List<SuperByteBufferFactory> chainRenderers = new ArrayList<>();
    private final List<EntityRenderer> entityRenderers = new ArrayList<>();

    public static List<SuperByteBufferFactory> dispatchChainConveyor() {
        var event = new PackageRenderEvent();
        ModLoader.postEvent(event);
        return event.chainRenderers;
    }

    public static List<EntityRenderer> dispatchEntity() {
        var event = new PackageRenderEvent();
        ModLoader.postEvent(event);
        return event.entityRenderers;
    }

    private PackageRenderEvent() {}

    public void registerForChainConveyor(SuperByteBufferFactory renderer) {
        chainRenderers.add(renderer);
    }

    public void registerForEntity(EntityRenderer renderer) {
        entityRenderers.add(renderer);
    }

    public interface SuperByteBufferFactory {
        SuperByteBuffer[] create(ChainConveyorBlockEntity be, ChainConveyorPackage box, float partialTicks);
    }

    public interface EntityRenderer {
        void render(PackageEntity entity, float yaw, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light);
    }
}
