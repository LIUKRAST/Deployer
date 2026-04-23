package net.liukrast.deployer.lib.registry;

import net.liukrast.deployer.lib.Deployer;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class DeployerTags {
    private DeployerTags() {}
    public static class Blocks {
        private Blocks() {}

        public static final TagKey<Block> OVERRIDE_BLOCK_CAPABILITY_FIX = create("override_block_capability_fix");

        private static TagKey<Block> create(String name) {
            return TagKey.create(Registries.BLOCK, Deployer.CONSTANTS.id(name));
        }
    }
}
