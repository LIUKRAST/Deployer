package net.liukrast.deployer.lib;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DeployerConfig {
    private DeployerConfig() {}

    public static class Client {
        private Client() {}
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue CAPES = BUILDER
                .comment("Shows Deployer supporter capes. When enabled, the UUID of each player whose cape is drawn is sent once per game session to api.liukrast.net")
                .define("capes", true);

        static final ModConfigSpec SPEC = BUILDER.build();
    }

    public static class Server {
        private Server() {}
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue BLOCK_CAPABILITY_FIX = BUILDER
                .comment("Allows create to interact with blocks that provide block capability, even though they have no block entity")
                .define("blockCapabilityFix", true);

        public static final ModConfigSpec.IntValue FACTORY_PANEL_MAX_CYCLES_PER_TICK = BUILDER
                .comment("Determines how many times a factory panel can be updated in the same tick")
                .defineInRange("factoryPanelMaxCyclesPerTick", 16, 0, Integer.MAX_VALUE);

        static final ModConfigSpec SPEC = BUILDER.build();
    }
}
