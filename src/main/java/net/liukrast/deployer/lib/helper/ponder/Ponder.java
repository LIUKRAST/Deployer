package net.liukrast.deployer.lib.helper.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;

import static net.liukrast.deployer.lib.helper.PonderSceneHelpers.*;

public interface Ponder {
    String getSchematicPath();
    void create(SceneBuilder builder, SceneBuildingUtil util);
}
