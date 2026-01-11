package net.liukrast.deployer.lib;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DeployerConfig {
    private DeployerConfig() {}

    public static class Client {
        private Client() {}
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue PANEL_CACHING = BUILDER
                .comment("Caching all input values to display when non instant updates are still processing")
                .define("panelCaching", true);

        public static final ModConfigSpec.BooleanValue PACKAGE_GOGGLE_INFO = BUILDER
                .comment("Adds a simple goggle overlay on package entities")
                .define("packageGoggleInformation", true);

        static final ModConfigSpec SPEC = BUILDER.build();
    }

    public static class Server {
        private Server() {}
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue BLOCK_CAPABILITY_FIX = BUILDER
                .comment("Allows create to interact with blocks that provide block capability, even though they have no block entity")
                .define("blockCapabilityFix", true);

        static final ModConfigSpec SPEC = BUILDER.build();
    }
}
