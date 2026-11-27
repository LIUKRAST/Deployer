package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
@ApiStatus.Internal
public interface PRExtension {
    void deployer$flag();
    boolean deployer$isFlagged();
}
