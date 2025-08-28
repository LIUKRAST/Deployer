package net.liukrast.deployer.lib;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DeployerConfig {
    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PANEL_CACHING = CLIENT
            .comment("Caching all input values to display when non instant updates are still processing")
            .define("panelCaching", true);

    static final ModConfigSpec CLIENT_SPEC = CLIENT.build();
}
