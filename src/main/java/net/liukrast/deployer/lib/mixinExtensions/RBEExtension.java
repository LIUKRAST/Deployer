package net.liukrast.deployer.lib.mixinExtensions;

import net.liukrast.deployer.lib.logistics.packager.GenericRepackageHelper;

public interface RBEExtension {
    void deployer$setGenericRepackageHelper(GenericRepackageHelper genericRepackageHelper);
    GenericRepackageHelper deployer$getGenericRepackageHelper();
}
