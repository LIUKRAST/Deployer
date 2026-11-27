package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import net.liukrast.deployer.lib.logistics.packager.GenericRepackageHelper;

@ApiStatus.Internal
public interface RBEExtension {
    void deployer$setGenericRepackageHelper(GenericRepackageHelper genericRepackageHelper);
    GenericRepackageHelper deployer$getGenericRepackageHelper();
}
