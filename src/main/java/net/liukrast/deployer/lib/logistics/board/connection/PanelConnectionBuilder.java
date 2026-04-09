package net.liukrast.deployer.lib.logistics.board.connection;

import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Builder for registering panel connections.
 */
public class PanelConnectionBuilder {
    private final Map<PanelConnection<?>, Supplier<?>> out;
    private final Set<PanelConnection<?>> in;

    public PanelConnectionBuilder(Map<PanelConnection<?>, Supplier<?>> out, Set<PanelConnection<?>> in) {
        this.out = out;
        this.in = in;
    }

    /**
     * Registers a panel connection as an input using a {@link DeferredHolder}.
     *
     * @param panelConnection the holder of the panel connection to register
     * @param <T> the type of value handled by the connection
     * @return this builder for chaining
     */
    public <T> PanelConnectionBuilder registerInput(@NotNull DeferredHolder<PanelConnection<?>, PanelConnection<T>> panelConnection) {
        return registerInput(panelConnection.get());
    }

    /**
     * Registers a panel connection as an input.
     *
     * @param panelConnection the panel connection to register
     * @param <T> the type of value handled by the connection
     * @return this builder for chaining
     */
    public <T> PanelConnectionBuilder registerInput(@NotNull PanelConnection<T> panelConnection) {
        this.in.add(panelConnection);
        return this;
    }

    /**
     * Registers a panel connection as an output with a value supplier using a {@link DeferredHolder}.
     *
     * @param panelConnection the holder of the panel connection to register
     * @param getter the supplier providing the current value for the output
     * @param <T> the type of value handled by the connection
     * @return this builder for chaining
     */
    public <T> PanelConnectionBuilder registerOutput(@NotNull DeferredHolder<PanelConnection<?>, PanelConnection<T>> panelConnection, @NotNull Supplier<T> getter) {
        return registerOutput(panelConnection.get(), getter);
    }

    /**
     * Registers a panel connection as an output with a value supplier.
     *
     * @param panelConnection the panel connection to register
     * @param getter the supplier providing the current value for the output
     * @param <T> the type of value handled by the connection
     * @return this builder for chaining
     */
    public <T> PanelConnectionBuilder registerOutput(@NotNull PanelConnection<T> panelConnection, @NotNull Supplier<T> getter) {
        this.out.put(panelConnection, getter);
        return this;
    }

    /**
     * Registers a panel connection as both an input and an output using a {@link DeferredHolder}.
     *
     * @param panelConnection the holder of the panel connection to register
     * @param getter the supplier providing the current value for the output
     * @param <T> the type of value handled by the connection
     * @r
     */
    public <T> PanelConnectionBuilder registerBoth(@NotNull DeferredHolder<PanelConnection<?>, PanelConnection<T>> panelConnection, @NotNull Supplier<T> getter) {
        return registerInput(panelConnection).registerOutput(panelConnection, getter);
    }

    /**
     * Registers a panel connection as both an input and an output.
     *
     * @param panelConnection the panel connection to register
     * @param getter the supplier providing the current value for the output
     * @param <T> the type of value handled by the connection
     * @return this builder for chaining
     */
    public <T> PanelConnectionBuilder registerBoth(@NotNull PanelConnection<T> panelConnection, @NotNull Supplier<T> getter) {
        return registerInput(panelConnection).registerOutput(panelConnection, getter);
    }

    /**
     * @deprecated Use {@link #registerBoth(DeferredHolder, Supplier)} instead.
     */
    @Deprecated(forRemoval = true, since = "2.1.0")
    public <T> PanelConnectionBuilder put(@NotNull DeferredHolder<PanelConnection<?>, PanelConnection<T>> panelConnection, @NotNull Supplier<T> getter) {
        return put(panelConnection.get(), getter);
    }

    /**
     * @deprecated Use {@link #registerBoth(PanelConnection, Supplier)} instead.
     */
    @Deprecated(forRemoval = true, since = "2.1.0")
    public <T> PanelConnectionBuilder put(@NotNull PanelConnection<T> panelConnection, @NotNull Supplier<T> getter) {
        out.put(panelConnection, getter);
        return this;
    }
}
