package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
@ApiStatus.Internal
public interface VITBExtension {
    boolean deployer$stillWaiting(Object wrapper);
    void deployer$awaitNewVersion(Object wrapper);
}
