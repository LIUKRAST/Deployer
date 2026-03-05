package net.liukrast.deployer.lib.helper.client;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageEntity;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PackageVisualExtension {
    private static final TransformedInstance[] EMPTY = new TransformedInstance[]{};
    public interface ChainConveyor {
        TransformedInstance[] EMPTY = PackageVisualExtension.EMPTY;
        default void beginFrame$start() {}
        default void beginFrame$end() {}
        default void _delete() {}
        TransformedInstance[] createBuffer(ChainConveyorPackage box,
                                           ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData,
                                           PostProcessor postProcessor);
    }

    public interface Entity {
        TransformedInstance[] EMPTY = PackageVisualExtension.EMPTY;
        default void beginFrame(DynamicVisual.Context context, PackageEntity packageEntity) {}
        default void _delete() {}
        TransformedInstance[] createBuffer(PackageEntity packageEntity);
    }

    public static class PostProcessor {
        private final Map<TransformedInstance, List<Consumer<TransformedInstance>>> map = new HashMap<>();

        public <T extends TransformedInstance> void subscribe(T instance, Consumer<T> consumer) {
            //noinspection unchecked
            map.computeIfAbsent(instance, k -> new ArrayList<>())
                    .add(t -> consumer.accept((T) t));
        }

        @ApiStatus.Internal
        public <T extends TransformedInstance> void consume(T instance) {
            var li = map.get(instance);
            if(li == null) return;
            li.forEach(consumer -> consumer.accept(instance));
        }
    }
}
