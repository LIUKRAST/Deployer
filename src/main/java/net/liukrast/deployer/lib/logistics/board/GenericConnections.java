package net.liukrast.deployer.lib.logistics.board;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;

import java.util.ArrayList;

public class GenericConnections<V> extends ArrayList<FactoryPanelConnection> {
    public V item;
    public int totalAmount;

    public GenericConnections(V item) {
        this.item = item;
    }
}
