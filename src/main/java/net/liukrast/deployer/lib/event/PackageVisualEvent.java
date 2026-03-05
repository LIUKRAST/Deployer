package net.liukrast.deployer.lib.event;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.liukrast.deployer.lib.helper.client.PackageVisualExtension;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PackageVisualEvent extends Event implements IModBusEvent {

    private final List<ChainConveyorFactory> chainVisuals = new ArrayList<>();
    private final List<EntityFactory> entityVisuals = new ArrayList<>();

    public static List<ChainConveyorFactory> dispatchChainConveyor() {
        var event = new PackageVisualEvent();
        ModLoader.postEvent(event);
        return event.chainVisuals;
    }

    public static List<EntityFactory> dispatchEntity() {
        var event = new PackageVisualEvent();
        ModLoader.postEvent(event);
        return event.entityVisuals;
    }

    private PackageVisualEvent() {}

    public void registerForChainConveyor(ChainConveyorFactory visual) {
        chainVisuals.add(visual);
    }

    public void registerForEntity(EntityFactory.Simple factory, Predicate<PackageEntity> predicate) {
        registerForEntity(new EntityFactory() {
            @Override
            public PackageVisualExtension.Entity create(VisualizationContext context, PackageEntity entity, float partialTicks) {
                return factory.create(context, entity, partialTicks);
            }

            @Override
            public boolean validForPackage(PackageEntity box) {
                return predicate.test(box);
            }
        });
    }

    public void registerForEntity(EntityFactory factory) {
        entityVisuals.add(factory);
    }

    public interface ChainConveyorFactory {
        PackageVisualExtension.ChainConveyor create(VisualizationContext context, ChainConveyorBlockEntity be, float partialTicks);
    }

    public interface EntityFactory {
        PackageVisualExtension.Entity create(VisualizationContext context, PackageEntity entity, float partialTicks);
        default boolean validForPackage(PackageEntity box) {
            return true;
        }

        interface Simple {
            PackageVisualExtension.Entity create(VisualizationContext context, PackageEntity entity, float partialTicks);
        }
    }

}
