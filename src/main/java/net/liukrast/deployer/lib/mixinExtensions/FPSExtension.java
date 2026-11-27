package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;

@ApiStatus.Internal
public interface FPSExtension {
    boolean deployer$isRestocker(AbstractPanelBehaviour apb);
}
