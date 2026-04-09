package net.liukrast.deployer.lib.logistics.board.connection;

import java.util.function.Function;

public record ConnectionLine(int color, boolean dots, boolean flowing) {
    public ConnectionLine(int color) {
        this(color, false, false);
    }

    public static <T> Function<T, ConnectionLine> staticColor(int color) {
        return staticColor(color, false, false);
    }
    public static <T> Function<T, ConnectionLine> staticColor(int color, boolean dots, boolean flowing) {
        ConnectionLine staticValue = new ConnectionLine(color, dots, flowing);
        return t -> staticValue;
    }
}
