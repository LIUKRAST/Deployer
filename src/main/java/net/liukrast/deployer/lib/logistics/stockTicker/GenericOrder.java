package net.liukrast.deployer.lib.logistics.stockTicker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Hash;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public record GenericOrder<V>(List<V> stacks, Hash.Strategy<? super V> strategy) {

    public static final Hash.Strategy<Object> DEFAULT_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(Object o) {
            return Objects.hashCode(o);
        }
        @Override
        public boolean equals(Object a, Object b) {
            return Objects.equals(a, b);
        }
    };

    /* Might just cache it */
    public static <V> GenericOrder<V> empty() {
        return new GenericOrder<>(Collections.emptyList(), DEFAULT_STRATEGY);
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public static <V> Codec<GenericOrder<V>> simpleCodec(Codec<V> codec, Hash.Strategy<? super V> strategy) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(codec).fieldOf("entries").forGetter(GenericOrder::stacks)
        ).apply(instance, b -> new GenericOrder<>(b, strategy)));
    }

    public static <V> StreamCodec<RegistryFriendlyByteBuf, GenericOrder<V>> simpleStreamCodec(StreamCodec<? super RegistryFriendlyByteBuf, V> codec, Hash.Strategy<? super V> strategy) {
        return StreamCodec.of(
                (buf, val) -> {
                    buf.writeVarInt(val.stacks.size());
                    for(V v : val.stacks) {
                        codec.encode(buf, v);
                    }
                },
                (buf) -> {
                    int i = buf.readVarInt();
                    List<V> list = new ArrayList<>();

                    for(int j = 0; j < i; j++) {
                        list.add(codec.decode(buf));
                    }
                    return new GenericOrder<>(list, strategy);
                }
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericOrder<?> other)) return false;

        if (stacks.size() != other.stacks.size())
            return false;

        for (int i = 0; i < stacks.size(); i++) {
            V a = stacks.get(i);
            Object b = other.stacks.get(i);

            if (!strategy.equals(a,(V)b)) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (V v : stacks) {
            result = 31 * result + strategy.hashCode(v);
        }
        return result;
    }
}
