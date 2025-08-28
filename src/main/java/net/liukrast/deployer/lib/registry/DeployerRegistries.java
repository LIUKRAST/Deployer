package net.liukrast.deployer.lib.registry;

import com.simibubi.create.Create;
import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.logistics.board.PanelType;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class DeployerRegistries {
    public static final ResourceKey<Registry<PanelType<?>>> PANEL_KEY = ResourceKey.createRegistryKey(DeployerConstants.id("panels"));
    public static final Registry<PanelType<?>> PANEL = new RegistryBuilder<>(PANEL_KEY)
            .sync(true)
            .defaultKey(Create.asResource("factory"))
            .create();

    public static final ResourceKey<Registry<PanelConnection<?>>> PANEL_CONNECTION_KEY = ResourceKey.createRegistryKey(DeployerConstants.id("panel_connections"));
    public static final Registry<PanelConnection<?>> PANEL_CONNECTION = new RegistryBuilder<>(PANEL_CONNECTION_KEY)
            .sync(true)
            .defaultKey(DeployerConstants.id("redstone"))
            .create();



    public static void init(NewRegistryEvent event) {
        event.register(PANEL);
        event.register(PANEL_CONNECTION);

    }
}
