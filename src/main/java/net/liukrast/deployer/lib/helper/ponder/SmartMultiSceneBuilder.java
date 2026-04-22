package net.liukrast.deployer.lib.helper.ponder;

import net.createmod.ponder.api.registration.MultiSceneBuilder;

public class SmartMultiSceneBuilder {
    public static SmartMultiSceneBuilder of(MultiSceneBuilder builder) {
        return new SmartMultiSceneBuilder(builder);
    }

    private final MultiSceneBuilder builder;

    private SmartMultiSceneBuilder(MultiSceneBuilder builder) {
        this.builder = builder;
    }

    public SmartMultiSceneBuilder addPonder(Ponder ponder) {
        builder.addStoryBoard(ponder.getSchematicPath(), ponder::create);
        return this;
    }
}
