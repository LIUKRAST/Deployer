package net.liukrast.deployer.lib.registry;

import com.simibubi.create.Create;
import net.liukrast.deployer.lib.Deployer;
import net.liukrast.deployer.lib.logistics.board.PanelType;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class DeployerRegistries {
    private DeployerRegistries() {}
    public static final ResourceKey<Registry<PanelType<?>>> PANEL_KEY = ResourceKey.createRegistryKey(Deployer.CONSTANTS.id("panels"));
    public static final Registry<PanelType<?>> PANEL = new RegistryBuilder<>(PANEL_KEY)
            .sync(true)
            .defaultKey(Create.asResource("factory"))
            .create();

    public static final ResourceKey<Registry<PanelConnection<?>>> PANEL_CONNECTION_KEY = ResourceKey.createRegistryKey(Deployer.CONSTANTS.id("panel_connections"));
    public static final Registry<PanelConnection<?>> PANEL_CONNECTION = new RegistryBuilder<>(PANEL_CONNECTION_KEY)
            .sync(true)
            .defaultKey(Deployer.CONSTANTS.id("redstone"))
            .create();

    public static final ResourceKey<Registry<StockInventoryType<?,?,?>>> STOCK_INVENTORY_KEY = ResourceKey.createRegistryKey(Deployer.CONSTANTS.id("stock_inventory"));
    public static final Registry<StockInventoryType<?,?,?>> STOCK_INVENTORY = new RegistryBuilder<>(STOCK_INVENTORY_KEY)
            .sync(true)
            .defaultKey(Deployer.CONSTANTS.id("item"))
            .create();

    public static void init(NewRegistryEvent event) {
        event.register(PANEL);
        event.register(PANEL_CONNECTION);
        event.register(STOCK_INVENTORY);
    }
}
