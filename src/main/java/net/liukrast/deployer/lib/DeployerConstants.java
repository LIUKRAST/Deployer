package net.liukrast.deployer.lib;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployerConstants {
    private DeployerConstants() {}
    public static final String MOD_ID = "deployer";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean PSIC_INSTALLED = false;

    public static ResourceLocation id(String path, Object... args) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, String.format(path, args));
    }
}
