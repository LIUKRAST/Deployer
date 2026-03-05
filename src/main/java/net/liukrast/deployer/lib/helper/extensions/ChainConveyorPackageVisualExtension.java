package net.liukrast.deployer.lib.helper.extensions;

import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;

@Deprecated
public interface ChainConveyorPackageVisualExtension {
    void beginFramePre(DynamicVisual.Context ctx);

    TransformedInstance[] setupBoxVisual();

    void beginFramePost(DynamicVisual.Context ctx);

    void _delete();
}
