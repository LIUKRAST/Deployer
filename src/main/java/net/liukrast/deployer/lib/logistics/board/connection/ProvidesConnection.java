package net.liukrast.deployer.lib.logistics.board.connection;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.mixinExtensions.FPCExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public interface ProvidesConnection {
    /**
     * Connections are the core process of a panel. This method is called when your panel is created and will want you to decide what the panel needs to handle.
     * When you specify an output, you will be asked to provide a {@link Supplier}. That will tell other gauges trying to read this what value this gauge is holding, in real time.
     * On the other side, specifying inputs is only used as "declaration", you declare that this gauge will eventually read that type of value, so that we know what connections you can pick in the connecting cable.
     * Order matters! In fact, when you first connect two panels, the system will automatically
     * establish a link based on the first compatible data type found in the registration order
     * of the input panel.
     * * <p>
     * If there are no shared data types between the source's outputs and the target's inputs,
     * the connection will remain inactive (no data will flow). However, if multiple data types
     * are compatible, the first one registered in the input panel's builder takes priority
     * as the default.
     * </p>
     * * <p>
     * Users can manually override this selection using a Wrench on the connection.
     * The manual selection is restricted to the intersection of available types:
     * a connection can only be established for a type that is both provided by the output
     * panel and accepted by the input panel.
     * </p>
     */
    void addConnections(PanelConnectionBuilder builder);
    /**
     * @return The set (ordered) containing all connections this panel outputs.
     * */
    Set<PanelConnection<?>> getInputConnections();
    /**
     * @return The set (ordered) containing all connections this panel reads.
     * */
    Set<PanelConnection<?>> getOutputConnections();
    default <T> Optional<T> getConnectionValue(DeferredHolder<PanelConnection<?>, PanelConnection<T>> connection) {
        return getConnectionValue(connection.get());
    }
    <T> Optional<T> getConnectionValue(PanelConnection<T> connection);
    /**
     * @return A set of all panel connections two links share
     * @param from The panel pointing
     * @param to The panel pointed
     * */
    static Set<PanelConnection<?>> getPossibleConnections(ProvidesConnection from, ProvidesConnection to) {
        Set<PanelConnection<?>> out = new LinkedHashSet<>();

        Set<PanelConnection<?>> sourceOut = from.getOutputConnections();

        for (PanelConnection<?> incoming : to.getInputConnections()) {
            if (sourceOut.contains(incoming)) {
                out.add(incoming);
            }
        }

        return out;
    }

    static Set<PanelConnection<?>> getPossibleConnections(FactoryPanelBehaviour from, FactoryPanelBehaviour to) {
        return getPossibleConnections((ProvidesConnection) from, (ProvidesConnection) to);
    }

    static Set<PanelConnection<?>> getPossibleConnections(FactoryPanelBehaviour from, ProvidesConnection to) {
        return getPossibleConnections((ProvidesConnection) from, to);
    }

    static Set<PanelConnection<?>> getPossibleConnections(ProvidesConnection from, FactoryPanelBehaviour to) {
        return getPossibleConnections(from, (ProvidesConnection) to);
    }

    /**
     * Retrieves the currently active connection type for a given link.
     * <p>
     * If no specific connection mode is set (e.g., first-time initialization),
     * it attempts to resolve and store a default connection provided by the supplier.
     * </p>
     *
     * @param fpc          the connection instance between two panels
     * @param defaultValue a supplier providing the fallback connection type if none is set
     * @return the active {@link PanelConnection}, or {@code null} if the default supplier returns null
     */
    static PanelConnection<?> getCurrentConnection(FactoryPanelConnection fpc, Supplier<PanelConnection<?>> defaultValue) {
        FPCExtension extension = (FPCExtension) fpc;
        PanelConnection<?> current = extension.deployer$getLinkMode();
        if(current != null) return current;
        return defaultValue.get();
    }

    /**
     * Aggregates all values from all connected sources for a specific connection type.
     * <p>
     * This version uses a unified stream to process standard panels, links, and extra
     * block connections, maintaining the "Abort" logic for unloaded chunks.
     * </p>
     *
     * @param connection the type of data connection to poll
     * @param <T>        the type of the values
     * @return a {@link List} of all found values, or {@code null} if any source
     * returned an "Abort" state.
     */
    @Nullable
    default <T> List<T> getAllValues(PanelConnection<T> connection) {
        List<AbstractPanelBehaviour.ConnectionValue<T>> withSource = getAllValuesWithSource(connection);
        if (withSource == null) return null;
        return withSource.stream()
                .map(AbstractPanelBehaviour.ConnectionValue::value)
                .toList();
    }

    /**
     * Aggregates all values from all connected sources for a specific connection type.
     * <p>
     * This version uses a unified stream to process standard panels, links, and extra
     * block connections, maintaining the "Abort" logic for unloaded chunks.
     * </p>
     *
     * @param connection the type of data connection to poll
     * @param <T>        the type of the values
     * @return a {@link List} of all values and their {@link FactoryPanelConnection}, or {@code null} if any source
     * returned an "Abort" state.
     */
    @Nullable
    <T> List<AbstractPanelBehaviour.ConnectionValue<T>> getAllValuesWithSource(PanelConnection<T> connection);

    default int overrideConnectionColor(int original, FactoryPanelConnection connection, float partialTicks) {
        return original;
    }



}
