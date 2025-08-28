package net.liukrast.deployer.lib.registry;

import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.liukrast.deployer.lib.DeployerConstants;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnection;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Defines all the default connections. You can obviously register your own
 * */
public class DeployerPanelConnections {
    private DeployerPanelConnections() {}
    private static final DeferredRegister<PanelConnection<?>> CONNECTIONS = DeferredRegister.create(DeployerRegistries.PANEL_CONNECTION, DeployerConstants.MOD_ID);

    public static final DeferredHolder<PanelConnection<?>, PanelConnection<ItemStack>> ITEMSTACK = CONNECTIONS.register("filter", () -> new PanelConnection<>(FilteringBehaviour::getFilter));
    public static final DeferredHolder<PanelConnection<?>, PanelConnection<Integer>> REDSTONE = CONNECTIONS.register("redstone", () -> new PanelConnection<>(b -> b.satisfied && b.count != 0 ? 15 : 0));
    public static final DeferredHolder<PanelConnection<?>, PanelConnection<Integer>> INTEGER = CONNECTIONS.register("integer", () -> new PanelConnection<>(FactoryPanelBehaviour::getLevelInStorage));
    public static final DeferredHolder<PanelConnection<?>, PanelConnection<String>> STRING = CONNECTIONS.register("string", () -> new PanelConnection<>(b -> {
        var source = AllDisplaySources.GAUGE_STATUS.get().createEntry(b.getWorld(), b.getPanelPosition());
        return source == null ? null : source.getFirst() + source.getValue().getString();
    }));

    /**
     * @return the value of a connection from a specific factory panel, even if it's a factory gauge
     * */
    public static <T> Optional<T> getConnectionValue(FactoryPanelBehaviour behaviour, DeferredHolder<PanelConnection<?>, PanelConnection<T>> panelConnection) {
        return getConnectionValue(behaviour, panelConnection.get());
    }

    /**
     * @return the value of a connection from a specific factory panel, even if it's a factory gauge
     * */
    public static <T> Optional<T> getConnectionValue(FactoryPanelBehaviour behaviour, PanelConnection<T> panelConnection) {
        if(behaviour == null) return Optional.empty();
        if(behaviour instanceof AbstractPanelBehaviour abstractPanelBehaviour) return abstractPanelBehaviour.getConnectionValue(panelConnection);
        return Optional.ofNullable(panelConnection.getDefault(behaviour));
    }

    /**
     * @return all the connections for a panel. Ordered by priority, even though they are contained in a set
     * */
    public static Set<PanelConnection<?>> getConnections(FactoryPanelBehaviour behaviour) {
        if(behaviour == null) return Set.of();
        if(behaviour instanceof AbstractPanelBehaviour ab) return ab.getConnections();
        return new LinkedHashSet<>(CONNECTIONS.getEntries().stream().map(DeferredHolder::get).toList()); //TODO: Check if order is maintained
    }

    @ApiStatus.Internal
    public static void register(IEventBus eventBus) {
        CONNECTIONS.register(eventBus);
    }
}
