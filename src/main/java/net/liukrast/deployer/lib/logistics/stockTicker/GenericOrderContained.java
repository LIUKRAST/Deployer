package net.liukrast.deployer.lib.logistics.stockTicker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record GenericOrderContained<V>(GenericOrder<V> orderedStacks) {

    public List<V> stacks() {
        return orderedStacks.stacks();
    }

    public boolean isEmpty() {
        return orderedStacks.isEmpty();
    }

    public static <V> GenericOrderContained<V> simple(List<V> stacks) {
        return new GenericOrderContained<>(new GenericOrder<>(stacks));
    }

    public static <V> Codec<GenericOrderContained<V>> simpleCodec(Codec<V> codec) {
        return Codec.withAlternative(
                RecordCodecBuilder.create(i -> i.group(
                        GenericOrder.simpleCodec(codec).fieldOf("ordered_stacks").forGetter(GenericOrderContained::orderedStacks)
                ).apply(i, GenericOrderContained::new)),
                RecordCodecBuilder.create(i -> i.group(
                        Codec.list(codec).fieldOf("entries").forGetter(GenericOrderContained::stacks)
                ).apply(i, GenericOrderContained::simple))
        );
    }

    public static <B extends ByteBuf, V> StreamCodec<B, GenericOrderContained<V>> simpleStreamCodec(StreamCodec<B, V> codec) {
        return StreamCodec.composite(
                GenericOrder.simpleStreamCodec(codec), GenericOrderContained::orderedStacks,
                GenericOrderContained::new
        );
    }
}
