package net.liukrast.deployer.lib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.ModConfigSpec;

public class DeployerConfig {
    private DeployerConfig() {}

    @OnlyIn(Dist.CLIENT)
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

        /*public static final ModConfigSpec.BooleanValue FROGPORT_LIMIT = BUILDER
                .comment("Makes so frogport do not export packages to chain drives if the chain drive has reached the package limit")
                .define("frogPortLimit", false);*/ //TODO

        public static final ModConfigSpec.BooleanValue FAST_REPACKAGE_ALGORITHM = BUILDER
                .comment("Uses a different algorithm to perform re-packaging behaviour, reducing it from O(n⁴) to O(n). If other mods collide or modify the behaviour, disable it")
                .define("fastRepackageAlgorithm", false);

        static final ModConfigSpec SPEC = BUILDER.build();
    }
}
