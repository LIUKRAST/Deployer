package net.liukrast.deployer.lib.registry;

import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.logistics.board.connection.ConnectionLine;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.logistics.board.connection.StockConnection;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

/**
 * Defines all the default connections. You can obviously register your own
 * */
public class DeployerPanelConnections {
    private DeployerPanelConnections() {}
    private static final DeferredRegister<PanelConnection<?>> CONNECTIONS = DeferredRegister.create(DeployerRegistries.PANEL_CONNECTION, DeployerConstants.MOD_ID);

    public static final DeferredHolder<PanelConnection<?>, PanelConnection<StockConnection>> STOCK_CONNECTION = CONNECTIONS.register("stock_connection", () -> new PanelConnection<>(ConnectionLine.staticColor(0x888898, false, true)));
    //TODO: Boolean!
    public static final DeferredHolder<PanelConnection<?>, PanelConnection<Integer>> REDSTONE = CONNECTIONS.register("redstone", () -> new PanelConnection<>(bl -> new ConnectionLine(bl > 0 ? 0xEF0000 : 0x580101)));
    public static final DeferredHolder<PanelConnection<?>, PanelConnection<Integer>> INTEGER = CONNECTIONS.register("integer", () -> new PanelConnection<>(ConnectionLine.staticColor(0x4572e3, false, true)));
    public static final DeferredHolder<PanelConnection<?>, PanelConnection<String>> STRING = CONNECTIONS.register("string", () -> new PanelConnection<>(ConnectionLine.staticColor(0xFFFFFF, true, false)));
    @ApiStatus.Internal
    public static void register(IEventBus eventBus) {
        CONNECTIONS.register(eventBus);
    }
}
