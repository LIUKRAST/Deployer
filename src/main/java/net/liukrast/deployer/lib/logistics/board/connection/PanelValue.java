package net.liukrast.deployer.lib.logistics.board.connection;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface PanelValue<T> {
    record Abort<T>() implements PanelValue<T> {}
    record Empty<T>() implements PanelValue<T> {}
    record Present<T>(@NotNull T value) implements PanelValue<T> {}

    static <T> PanelValue<T> abort() {
        return new Abort<>();
    }

    static <T> PanelValue<T> empty() {
        return new Empty<>();
    }

    static <T> PanelValue<T> of(@NotNull T value) {
        return new Present<>(value);
    }

    static <T> PanelValue<T> of(Optional<T> opt) {
        return opt.map(PanelValue::of).orElseGet(PanelValue::empty);
    }
}
