package net.liukrast.deployer.lib.mixinExtensions;

import org.jetbrains.annotations.ApiStatus;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface FPCExtension {
    void deployer$setLinkMode(@Nullable PanelConnection<?> conn);
    @Nullable PanelConnection<?> deployer$getLinkMode();
}
