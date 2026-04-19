package net.liukrast.deployer.lib.logistics.board.connection;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.mixin.FactoryPanelSupportAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public abstract class AbstractPanelSupportBehaviour extends FactoryPanelSupportBehaviour implements ProvidesConnection {
    private final Set<PanelConnection<?>> connectionsIn = new LinkedHashSet<>();
    private final Map<PanelConnection<?>, Supplier<?>> connectionsOut = new LinkedHashMap<>();

    public AbstractPanelSupportBehaviour(SmartBlockEntity be, Supplier<Boolean> isOutput, Runnable onNotify) {
        super(be, isOutput, () -> false, onNotify);
        var builder = new PanelConnectionBuilder(this.connectionsOut, this.connectionsIn);
        addConnections(builder);
    }

    /**
     * @return The set (ordered) containing all connections this panel outputs.
     * */
    public Set<PanelConnection<?>> getInputConnections() {
        return connectionsIn;
    }

    /**
     * @return The set (ordered) containing all connections this panel reads.
     * */
    public Set<PanelConnection<?>> getOutputConnections() {
        return connectionsOut.keySet();
    }

    @Override
    public <T> Optional<T> getConnectionValue(PanelConnection<T> connection) {
        if(!connectionsOut.containsKey(connection)) return Optional.empty();
        // We can safely cast here.
        //noinspection unchecked
        return Optional.ofNullable((T) connectionsOut.get(connection).get());
    }

    @Override
    public @Nullable <T> List<AbstractPanelBehaviour.ConnectionValue<T>> getAllValuesWithSource(PanelConnection<T> connection) {
        List<AbstractPanelBehaviour.ConnectionValue<T>> out = new ArrayList<>();
        for (Iterator<FactoryPanelPosition> iterator = getLinkedPanels().iterator(); iterator.hasNext(); ) {
            FactoryPanelPosition panelPos = iterator.next();
            if (!getWorld().isLoaded(panelPos.pos()))
                return null;
            FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
            if (behaviour == null) {
                iterator.remove();
                ((FactoryPanelSupportAccessor)this).deployer$setChanged(true);
                continue;
            }
            if (!behaviour.isActive())
                continue;
            var conn = behaviour.targetedByLinks.get(getPos());
            if(conn == null) continue;
            var pc = ProvidesConnection.getCurrentConnection(conn, () -> ProvidesConnection.getPossibleConnections(behaviour, this).stream().findFirst().orElse(null));
            if(pc == null || pc != connection) continue;
            var opt = ((ProvidesConnection)behaviour).getConnectionValue(connection);
            opt.ifPresent(t -> out.add(new AbstractPanelBehaviour.ConnectionValue<>(conn, t)));
        }
        return out;
    }
}
